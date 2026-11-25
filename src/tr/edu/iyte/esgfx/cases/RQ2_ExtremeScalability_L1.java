package tr.edu.iyte.esgfx.cases;

import java.util.List;
import java.util.Set;
import java.util.Locale;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;

import tr.edu.iyte.esgfx.cases.resultrecordingutilities.TestPipelineMeasurementWriter_ExtremeScalability;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;
import tr.edu.iyte.esgfx.testgeneration.eventcoverage.EventCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.eventcoverage.EulerCycleGeneratorForEventCoverage;
import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;
import tr.edu.iyte.esgfx.testexecution.TestExecutor;

public class RQ2_ExtremeScalability_L1 extends CaseStudyUtilities {

    public void measurePipelineForESGFxEventCoverage() throws Exception {
        RunMeasurements.markStart();
        RunMeasurements.Totals checkTotals = new RunMeasurements.Totals();
        long ownSatTimeNanos = 0, skippedSatTimeNanos = 0, skippedConfigurations = 0, enumeratedConfigurations = 0;

        coverageLength = 1;
        setCoverageType();
        System.out.println("Test Generation And Execution Pipeline L=" + coverageLength + " " + SPLName + " STARTED");
        
        int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));
        int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
        int runID = Integer.parseInt(System.getenv().getOrDefault("runID", "1"));
        int timeoutHours = Integer.parseInt(System.getenv().getOrDefault("TIMEOUT_HOURS", "0"));
        long maxDurationNanos = RunMeasurements.timeBudgetNanos();
        
        long totalSatTimeNanos = 0;
        long totalProdGenTimeNanos = 0;
        long totalTestGenTimeNanos = 0;
        long totalTestExecTimeNanos = 0;

        long globalPeakMemoryGenBytes = 0;
        long globalPeakMemoryExecBytes = 0;

        int globalTotalESGFxVertices = 0;
        int globalTotalESGFxEdges = 0;
        long globalTotalTestCases = 0;
        long globalTotalTestEvents = 0;

        double globalTotalCoverage = 0.0;
        long globalTotalCoverageAnalysisTimeNanos = 0;
        
        long startTime1 = System.nanoTime();
        
        featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFile, ESGFxFile);
        List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);

        // The products are enumerated from the DIMACS file of the feature model.
        ProductSpace productSpace = ProductSpace.open(DIMACSFile, DIMACSMappingFile, featureModelFile,
                featureExpressionMapFromFeatureModel, mandatoryFeatures);

        int productID = 0;
        int handledProducts = 0;
        int failedProducts = 0;

        ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();
        EulerCycleGeneratorForEventCoverage eulerCycleGeneratorForEventCoverage = new EulerCycleGeneratorForEventCoverage(
                featureExpressionMapFromFeatureModel);
        EulerCycleToTestSequenceGenerator eulerCycleToTestSequenceGenerator = new EulerCycleToTestSequenceGenerator();

        String stopReason = "Completed";
        long budgetExcludedNanos = 0;
        long globalPeakGenAboveBaselineBytes = 0;
        // One row per product, with the configuration, so that distributions over products and
        // the throughput over time can be computed afterwards.
        java.io.PrintWriter productRecord = RunMeasurements.openProductRecord(
                extremeScalabilityTestPipelineMeasurementFolder + "perProduct/",
                String.format("%s_%s_shard%02d_run%d", SPLName, "ESG-Fx_L1", CURRENT_SHARD, runID),
                "# features: " + RunMeasurements.featureNames(featureExpressionList) + "\n"
                        + "ProductNumber;Selection;Vertices;Edges;Sequences;Events;ProductGenTime(us);TestGenTime(us);"
                        + "TestExecTime(us);GenPeakAboveBaseline(kB);ElapsedSinceStart(ms);Status;"
                        + RunMeasurements.PRODUCT_COLUMNS);
        long satStart = System.nanoTime();

        while (true) {
        	
        	if (System.nanoTime() - startTime1 - budgetExcludedNanos >= maxDurationNanos) {
                stopReason = "TimeBudget";
                System.out.println("Timeout reached. Processed products within limit: " + handledProducts);
                break;
            }

            boolean isSatisfiable;
            try {
                isSatisfiable = productSpace.next();
            } catch (OutOfMemoryError oom) {
                // The clauses that exclude the enumerated products no longer fit in the heap.
                productSpace = null;
                System.gc();
                stopReason = "OutOfMemoryInEnumeration";
                break;
            }
            long satEnd = System.nanoTime();
            long configurationSatNanos = satEnd - satStart;
            totalSatTimeNanos += configurationSatNanos;

            if (!isSatisfiable) break; // every product has been enumerated

            productID++;
            String productName = ProductIDUtil.format(productID);


            long blockingStart = System.nanoTime();
            try {
                productSpace.exclude();
            } catch (OutOfMemoryError oom) {
                productSpace = null;
                System.gc();
                stopReason = "OutOfMemoryInEnumeration";
                break;
            }
            long blockingNanos = System.nanoTime() - blockingStart;
            totalSatTimeNanos += blockingNanos;
            configurationSatNanos += blockingNanos;
            enumeratedConfigurations++;

            boolean isProductConfigurationValid = isProductConfigurationValid(featureModel,
                    featureExpressionMapFromFeatureModel);

            if (!isProductConfigurationValid || ((productID - 1) % N_SHARDS) != CURRENT_SHARD) {
                // Enumerated here but generated by another shard (or not valid).
                skippedSatTimeNanos += configurationSatNanos;
                skippedConfigurations++;
                satStart = System.nanoTime();
                continue;
            }
            ownSatTimeNanos += configurationSatNanos;

            handledProducts++;
            long p0ProdGen = totalProdGenTimeNanos, p0TestGen = totalTestGenTimeNanos, p0Exec = totalTestExecTimeNanos;
            long p0Cases = globalTotalTestCases, p0Events = globalTotalTestEvents;
            long pGenPeakAbove = -1;
            String productStatus = "SUCCESS";
            String productCheck = RunMeasurements.check(null, null, null);

            ESG productESGFx = null;
            ESG stronglyConnectedBalancedESGFx = null;
            Set<EventSequence> testSequences = null;
            List<Vertex> eulerCycle = null;
            EventCoverageAnalyser eventCoverageAnalyser = null;
            TestExecutor testExecutor = null;

            try {
                // 1. MEASURE PRODUCT GENERATION TIME 
                long prodGenStart = System.nanoTime();
                productESGFx = productESGFxGenerator.generateProductESGFx(productID, productName, ESGFx);
                totalProdGenTimeNanos += (System.nanoTime() - prodGenStart);

                if (productESGFx != null) {
                    globalTotalESGFxVertices += productESGFx.getVertexList().size();
                    globalTotalESGFxEdges += productESGFx.getEdgeList().size();
                }

                // 2. MEASURE TEST GENERATION
                resetPeakMemoryCounters();
                long heapAtGenStart = RunMeasurements.usedHeapBytes();
                long testGenStart = System.nanoTime();

                // Event coverage walks the product-level model itself.
                eulerCycleGeneratorForEventCoverage.generateEulerCycle(productESGFx);
                eulerCycle = eulerCycleGeneratorForEventCoverage.getEulerCycle();

                testSequences = eulerCycleToTestSequenceGenerator.CESgenerator(eulerCycle);
                
                totalTestGenTimeNanos += (System.nanoTime() - testGenStart);

                if (testSequences != null) {
                    globalTotalTestCases += testSequences.size();
                    for (EventSequence seq : testSequences) {
                        globalTotalTestEvents += seq.length();
                    }
                }
                
                long currentPeakGenBytes = getPeakHeapMemoryBytes();
                // The heap also holds the clauses of the enumeration; this is the growth during generation.
                pGenPeakAbove = Math.max(0, currentPeakGenBytes - heapAtGenStart);
                if (pGenPeakAbove > globalPeakGenAboveBaselineBytes)
                    globalPeakGenAboveBaselineBytes = pGenPeakAbove;
                if (currentPeakGenBytes > globalPeakMemoryGenBytes)
                    globalPeakMemoryGenBytes = currentPeakGenBytes;
                
                // ISOLATION BARRIER
                System.gc();
                
                // 3. MEASURE TEST EXECUTION
                resetPeakMemoryCounters();
                long testExecStart = System.nanoTime();
                if (testSequences != null && !testSequences.isEmpty()) {
                    testExecutor = new TestExecutor(testSequences);
                    testExecutor.executeAllTests(productESGFx);
                }
                totalTestExecTimeNanos += (System.nanoTime() - testExecStart);

                long currentPeakExecBytes = getPeakHeapMemoryBytes();
                if (currentPeakExecBytes > globalPeakMemoryExecBytes)
                    globalPeakMemoryExecBytes = currentPeakExecBytes;
                
                // 4. MEASURE COVERAGE ANALYSIS
                long coverageAnalysisStart = System.nanoTime();
                eventCoverageAnalyser = new EventCoverageAnalyser();
                double currentCoverage = eventCoverageAnalyser.analyseEventCoverage(productESGFx, testSequences, featureExpressionMapFromFeatureModel);
                long coverageAnalysisEnd = System.nanoTime();
                
                globalTotalCoverageAnalysisTimeNanos += (coverageAnalysisEnd - coverageAnalysisStart);
                globalTotalCoverage += currentCoverage;

                // Outside every timer: replay check and sequence coverage.
                productCheck = RunMeasurements.check(productESGFx, testSequences, checkTotals, 1, 2);
                // The check is not part of the time budget.
                budgetExcludedNanos += RunMeasurements.lastCheckNanos();

            } catch (OutOfMemoryError oom) {
                failedProducts++;
                productStatus = "OutOfMemory";
                System.gc(); 
            } catch (Exception e) {
                failedProducts++;
                productStatus = "Exception: " + e.getClass().getSimpleName() + ": " + e.getMessage();
            } catch (Throwable t) {
                failedProducts++;
                productStatus = "Error: " + t;
            } finally {
                productRecord.println(productID + ";" + RunMeasurements.selectionBits(featureExpressionList) + ";"
                        + (productESGFx != null ? productESGFx.getVertexList().size() : -1) + ";"
                        + (productESGFx != null ? productESGFx.getEdgeList().size() : -1) + ";"
                        + (globalTotalTestCases - p0Cases) + ";" + (globalTotalTestEvents - p0Events) + ";"
                        + ((totalProdGenTimeNanos - p0ProdGen) / 1000) + ";" + ((totalTestGenTimeNanos - p0TestGen) / 1000) + ";"
                        + ((totalTestExecTimeNanos - p0Exec) / 1000) + ";" + (pGenPeakAbove < 0 ? -1 : pGenPeakAbove / 1024) + ";"
                        + (long) RunMeasurements.elapsedMs() + ";"
                        + String.valueOf(productStatus).replace(';', ',').replace('\n', ' ') + ";" + productCheck);
                if (handledProducts % 200 == 0) {
                    productRecord.flush();
                }
                eulerCycleGeneratorForEventCoverage.reset();
                eulerCycleToTestSequenceGenerator.reset();
                testExecutor = null;
                testSequences = null;
                productESGFx = null;
                stronglyConnectedBalancedESGFx = null;
                eulerCycle = null;
                eventCoverageAnalyser = null;

                if (handledProducts % 100 == 0) {
                    System.out.println("Processed " + handledProducts + " products. Current product ID: " + productID);
                    System.gc(); 
                }
            }

            satStart = System.nanoTime();
        }

        double satTimeMs = totalSatTimeNanos / 1_000_000.0;
        double prodGenTimeMs = totalProdGenTimeNanos / 1_000_000.0;
        double testGenTimeMs = totalTestGenTimeNanos / 1_000_000.0;
        double testExecTimeMs = totalTestExecTimeNanos / 1_000_000.0;
        double coverageAnalysisTimeMs = globalTotalCoverageAnalysisTimeNanos / 1_000_000.0;
        
        double timeElapsedTotalMs = satTimeMs + prodGenTimeMs + testGenTimeMs + testExecTimeMs + coverageAnalysisTimeMs;
        
        double avgCoverage = handledProducts > 0 ? globalTotalCoverage / handledProducts : 0.0;

        double peakGenMemoryMB = globalPeakMemoryGenBytes / (1024.0 * 1024.0);
        double peakExecMemoryMB = globalPeakMemoryExecBytes / (1024.0 * 1024.0);
        
        String summaryResultPath = (N_SHARDS > 1)
                ? String.format("%sESG-Fx/%s/%s_ESG-Fx_L%d_shard%02d.csv", extremeScalabilityTestPipelineMeasurementFolder, coverageType, SPLName, coverageLength, CURRENT_SHARD)
                : String.format("%sESG-Fx/%s/%s_ESG-Fx_L%d.csv", extremeScalabilityTestPipelineMeasurementFolder, coverageType, SPLName, coverageLength);
        
        TestPipelineMeasurementWriter_ExtremeScalability.writeDetailedPipelineMeasurementForESGFx_L1(runID,
                timeElapsedTotalMs, satTimeMs, prodGenTimeMs, testGenTimeMs, peakGenMemoryMB, 
                globalTotalESGFxVertices, globalTotalESGFxEdges, globalTotalTestCases, globalTotalTestEvents, 
                avgCoverage, coverageAnalysisTimeMs, testExecTimeMs, peakExecMemoryMB, 
                handledProducts, failedProducts, summaryResultPath, SPLName, coverageType);

        productRecord.close();
        RunMeasurements.writeJvmRow(summaryResultPath, runID, CURRENT_SHARD, N_SHARDS, "ESG-Fx", "L1",
                handledProducts, failedProducts, -1,
                "StopReason;GenPeakAboveBaseline(MB);EnumeratedConfigurations;SkippedConfigurations;OwnSatTime(ms);SkippedSatTime(ms);" + RunMeasurements.Totals.COLUMNS,
                stopReason + ";" + (globalPeakGenAboveBaselineBytes / (1024 * 1024)) + ";" + enumeratedConfigurations + ";"
                        + skippedConfigurations + ";"
                        + String.format(Locale.ROOT, "%.3f", ownSatTimeNanos / 1_000_000.0) + ";"
                        + String.format(Locale.ROOT, "%.3f", skippedSatTimeNanos / 1_000_000.0) + ";" + checkTotals.toRow());

        System.out.println("Total Time Measurement L=" + coverageLength + " " + SPLName + " FINISHED. Peak Memory: "
                + String.format(Locale.ROOT, "%.2f", Math.max(peakGenMemoryMB, peakExecMemoryMB)) + " MB");
    }
}