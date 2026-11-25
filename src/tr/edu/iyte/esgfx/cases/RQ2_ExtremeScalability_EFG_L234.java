package tr.edu.iyte.esgfx.cases;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.Locale;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;

import tr.edu.iyte.esgfx.cases.resultrecordingutilities.TestPipelineMeasurementWriter_EFG_ExtremeScalability;
import tr.edu.iyte.esgfx.conversion.xml.ESGToEFGFileWriter;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EdgeCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.eventcoverage.EventCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.guitar.EFGMetricsExtractor;
import tr.edu.iyte.esgfx.testgeneration.guitar.EFGMetricsExtractor.EFGMetrics;
import tr.edu.iyte.esgfx.testgeneration.guitar.GuitarExecutionWrapper;
import tr.edu.iyte.esgfx.testgeneration.guitar.GuitarOutputToEventSequenceParser;
import tr.edu.iyte.esgfx.testexecution.TestExecutor;

/**
 * RQ2 Extreme Scalability: EFG Baseline (CORRECTED)
 * 
 * CRITICAL FIXES:
 * 1. Proper test sequence parsing (was missing!)
 * 2. Disk cleanup after each product (streaming pipeline)
 * 3. Parse time tracking added
 * 4. EFG vertices/edges tracking added
 * 
 * PURPOSE: Demonstrate EFG's scalability limitations on large SPLs
 * 
 * KEY FINDINGS TO CAPTURE:
 * 1. Coverage degradation (syngo.via: 4-10%, HockertyShirts: 37-40%)
 * 2. Time cost (2-5 minutes per 400 products)
 * 3. Failure modes (OOM, GUITAR crashes)
 * 
 * IMPLEMENTATION NOTES (from skill Design Decision #2):
 * - Streaming pipeline: SAT → ESGFx in memory → temp EFG → GUITAR → measure → delete
 * - Disk usage stays constant (~50 MB per shard)
 * - Crash recovery via CSV last-row check
 * - Edge coverage tracking is CRITICAL for validity assessment
 */
public class RQ2_ExtremeScalability_EFG_L234 extends CaseStudyUtilities {

    public void measureEFGScalability() throws Exception {
        RunMeasurements.markStart();
        RunMeasurements.Totals checkTotals = new RunMeasurements.Totals();
        long maxChildPeakRssKb = -1;
        long ownSatTimeNanos = 0, skippedSatTimeNanos = 0, skippedConfigurations = 0, enumeratedConfigurations = 0;
        
        int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));
        int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
        int L_LEVEL = Integer.parseInt(System.getenv().getOrDefault("L_LEVEL", "2"));
        int runID = Integer.parseInt(System.getenv().getOrDefault("runID", "1"));
        int timeoutHours = Integer.parseInt(System.getenv().getOrDefault("TIMEOUT_HOURS", "0"));
        long maxDurationNanos = RunMeasurements.timeBudgetNanos();
        
        String EFGCoverageType = "L" + L_LEVEL;
        coverageLength = L_LEVEL;
        setCoverageType();

        System.out.println("EFG Extreme Scalability Pipeline L=" + coverageLength + " " + SPLName + " STARTED");
        
        long totalSatTimeNanos = 0;
        long totalProdGenTimeNanos = 0;
        long totalEFGTransformationTimeNanos = 0;  // ESGFx → EFG XML writing
        long totalTestGenTimeNanos = 0;            // GUITAR test generation
        long totalParseTimeNanos = 0;              // GUITAR output → EventSequence parsing
        long totalTestExecTimeNanos = 0;
        
        long globalPeakMemoryGenBytes = 0;
        long globalPeakMemoryExecBytes = 0;

        int globalTotalEFGVertices = 0;
        int globalTotalEFGEdges = 0;
        long globalTotalTestCases = 0;
        long globalTotalTestEvents = 0;

        double globalTotalEventCoverage = 0.0;
        double globalTotalEdgeCoverage = 0.0;      // CRITICAL METRIC
        long globalTotalEventCoverageAnalysisTimeNanos = 0;
        long globalTotalEdgeCoverageAnalysisTimeNanos = 0;
        
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

        String stopReason = "Completed";
        long budgetExcludedNanos = 0;
        long globalPeakGenAboveBaselineBytes = 0;
        // One row per product, with the configuration, so that distributions over products and
        // the throughput over time can be computed afterwards.
        java.io.PrintWriter productRecord = RunMeasurements.openProductRecord(
                extremeScalabilityTestPipelineMeasurementFolder + "perProduct/",
                String.format("%s_%s_shard%02d_run%d", SPLName, "EFG_L" + coverageLength, CURRENT_SHARD, runID),
                "# features: " + RunMeasurements.featureNames(featureExpressionList) + "\n"
                        + "ProductNumber;Selection;Vertices;Edges;Sequences;Events;ProductGenTime(us);TestGenTime(us);"
                        + "TestExecTime(us);GenPeakAboveBaseline(kB);ElapsedSinceStart(ms);Status");
        long satStart = System.nanoTime();

        while (true) {
            
            if (System.nanoTime() - startTime1 - budgetExcludedNanos >= maxDurationNanos) {
                stopReason = "TimeBudget";
                System.out.println("Timeout reached. Processed products: " + handledProducts);
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
            String productName = ProductIDUtil.format(productID);
            
            // Temporary files (will be deleted after measurement)
            String efgFilePath = EFGFolder + productName + ".EFG";
            String EFGTestSequencesFolderPerProduct = efg_testsequencesFolder + productName + "/" + EFGCoverageType + "/";
            
            int efgVertices = 0, efgEdges = 0;

            ESG productESGFx = null;
            Set<EventSequence> testSequences = null;
            TestExecutor testExecutor = null;
            EventCoverageAnalyser eventCoverageAnalyser = null;
            EdgeCoverageAnalyser edgeCoverageAnalyser = null;

            try {
                // 1. PRODUCT GENERATION
                long prodGenStart = System.nanoTime();
                productESGFx = productESGFxGenerator.generateProductESGFx(productID, productName, ESGFx);
                totalProdGenTimeNanos += (System.nanoTime() - prodGenStart);

                // 2. EFG TRANSFORMATION (ESGFx → EFG XML)
                resetPeakMemoryCounters();
                long heapAtGenStart = RunMeasurements.usedHeapBytes();
                
                long efgTransformStart = System.nanoTime();
                ESGToEFGFileWriter.writeESGFxToEFGFile(productESGFx, productName, EFGFolder);
                long efgTransformEnd = System.nanoTime();
                totalEFGTransformationTimeNanos += (efgTransformEnd - efgTransformStart);

                // Extract EFG metrics (vertices/edges count)
                EFGMetrics efgMetrics = EFGMetricsExtractor.getMetrics(efgFilePath);
                efgVertices = efgMetrics.vertices;
                efgEdges = efgMetrics.edges;
                globalTotalEFGVertices += efgVertices;
                globalTotalEFGEdges += efgEdges;

                // 3. GUITAR TEST GENERATION
                long testGenStart = System.nanoTime();
                GuitarExecutionWrapper.generateTestsFromEFG(efgFilePath, EFGTestSequencesFolderPerProduct, L_LEVEL);
                totalTestGenTimeNanos += (System.nanoTime() - testGenStart);
                maxChildPeakRssKb = Math.max(maxChildPeakRssKb, GuitarExecutionWrapper.getLastChildPeakRssKb());

                // 4. PARSE GUITAR OUTPUT TO EVENT SEQUENCES (CRITICAL FIX!)
                long parseStart = System.nanoTime();
                testSequences = GuitarOutputToEventSequenceParser.parseGuitarTests(
                        EFGTestSequencesFolderPerProduct, 
                        efgFilePath, 
                        (ESGFx) productESGFx);
                long parseEnd = System.nanoTime();
                totalParseTimeNanos += (parseEnd - parseStart);

                // Count test cases and events
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
                if (currentPeakGenBytes > globalPeakMemoryGenBytes) {
                    globalPeakMemoryGenBytes = currentPeakGenBytes;
                }
                
                // ISOLATION BARRIER
                System.gc();

                // 5. TEST EXECUTION
                resetPeakMemoryCounters();
                long testExecStart = System.nanoTime();
                if (testSequences != null && !testSequences.isEmpty()) {
                    testExecutor = new TestExecutor(testSequences);
                    testExecutor.executeAllTests(productESGFx);
                }
                totalTestExecTimeNanos += (System.nanoTime() - testExecStart);

                long currentPeakExecBytes = getPeakHeapMemoryBytes();
                if (currentPeakExecBytes > globalPeakMemoryExecBytes) {
                    globalPeakMemoryExecBytes = currentPeakExecBytes;
                }

                // 6. COVERAGE ANALYSIS - BOTH EVENT AND EDGE
                long eventCovAnalysisStart = System.nanoTime();
                eventCoverageAnalyser = new EventCoverageAnalyser();
                double currentEventCoverage = eventCoverageAnalyser.analyseEventCoverage(
                        productESGFx, testSequences, featureExpressionMapFromFeatureModel);
                long eventCovAnalysisEnd = System.nanoTime();
                globalTotalEventCoverageAnalysisTimeNanos += (eventCovAnalysisEnd - eventCovAnalysisStart);
                globalTotalEventCoverage += currentEventCoverage;

                // Edge coverage - CRITICAL for assessing EFG's validity
                long edgeCovAnalysisStart = System.nanoTime();
                edgeCoverageAnalyser = new EdgeCoverageAnalyser();
                double currentEdgeCoverage = edgeCoverageAnalyser.analyseEdgeCoverage(
                        productESGFx, testSequences, featureExpressionMapFromFeatureModel);
                long edgeCovAnalysisEnd = System.nanoTime();
                globalTotalEdgeCoverageAnalysisTimeNanos += (edgeCovAnalysisEnd - edgeCovAnalysisStart);
                globalTotalEdgeCoverage += currentEdgeCoverage;

                // Outside every timer: replay check and sequence coverage.
                RunMeasurements.check(productESGFx, testSequences, checkTotals, 2, coverageLength);
                // The check is not part of the time budget.
                budgetExcludedNanos += RunMeasurements.lastCheckNanos();

            } catch (OutOfMemoryError oom) {
                failedProducts++;
                productStatus = "OutOfMemory";
                System.err.println("OOM on product " + productID);
                System.gc();
            } catch (Exception e) {
                failedProducts++;
                productStatus = "Exception: " + e.getClass().getSimpleName() + ": " + e.getMessage();
                System.err.println("GUITAR/EFG failure on product " + productID + ": " + e.getMessage());
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
                        + String.valueOf(productStatus).replace(';', ',').replace('\n', ' '));
                if (handledProducts % 200 == 0) {
                    productRecord.flush();
                }
                // 7. CLEANUP - DELETE TEMPORARY FILES (STREAMING PIPELINE!)
                deleteEFGTemporaryFiles(efgFilePath, EFGTestSequencesFolderPerProduct);
                
                // Memory cleanup
                testSequences = null;
                testExecutor = null;
                productESGFx = null;
                eventCoverageAnalyser = null;
                edgeCoverageAnalyser = null;

                if (handledProducts % 100 == 0) {
                    System.out.println("Processed " + handledProducts + " products. Current ID: " + productID);
                    System.gc();
                }
            }

            satStart = System.nanoTime();
        }

        // AGGREGATION
        double satTimeMs = totalSatTimeNanos / 1_000_000.0;
        double prodGenTimeMs = totalProdGenTimeNanos / 1_000_000.0;
        double efgTransformationTimeMs = totalEFGTransformationTimeNanos / 1_000_000.0;
        double testGenTimeMs = totalTestGenTimeNanos / 1_000_000.0;
        double parseTimeMs = totalParseTimeNanos / 1_000_000.0;
        double testExecTimeMs = totalTestExecTimeNanos / 1_000_000.0;
        double eventCoverageAnalysisTimeMs = globalTotalEventCoverageAnalysisTimeNanos / 1_000_000.0;
        double edgeCoverageAnalysisTimeMs = globalTotalEdgeCoverageAnalysisTimeNanos / 1_000_000.0;
        
        double timeElapsedTotalMs = satTimeMs + prodGenTimeMs + efgTransformationTimeMs 
                + testGenTimeMs + parseTimeMs + testExecTimeMs 
                + eventCoverageAnalysisTimeMs + edgeCoverageAnalysisTimeMs;
        
        double avgEventCoverage = handledProducts > 0 ? globalTotalEventCoverage / handledProducts : 0.0;
        double avgEdgeCoverage = handledProducts > 0 ? globalTotalEdgeCoverage / handledProducts : 0.0;

        double peakGenMemoryMB = globalPeakMemoryGenBytes / (1024.0 * 1024.0);
        double peakExecMemoryMB = globalPeakMemoryExecBytes / (1024.0 * 1024.0);
        
        String summaryResultPath = (N_SHARDS > 1)
                ? String.format("%sEFG/%s/%s_EFG_L%d_shard%02d.csv", 
                        extremeScalabilityTestPipelineMeasurementFolder, coverageType, SPLName, coverageLength, CURRENT_SHARD)
                : String.format("%sEFG/%s/%s_EFG_L%d.csv", 
                        extremeScalabilityTestPipelineMeasurementFolder, coverageType, SPLName, coverageLength);

        TestPipelineMeasurementWriter_EFG_ExtremeScalability.writeDetailedPipelineMeasurementForEFG_L234(
                runID, timeElapsedTotalMs, satTimeMs, prodGenTimeMs, 
                efgTransformationTimeMs, testGenTimeMs, parseTimeMs,  // ADDED parseTimeMs
                peakGenMemoryMB, 
                globalTotalEFGVertices, globalTotalEFGEdges,  // ADDED EFG metrics
                globalTotalTestCases, globalTotalTestEvents,
                avgEventCoverage, eventCoverageAnalysisTimeMs,
                avgEdgeCoverage, edgeCoverageAnalysisTimeMs,
                testExecTimeMs, peakExecMemoryMB, 
                handledProducts, failedProducts, 
                summaryResultPath, SPLName, coverageType);

        productRecord.close();
        RunMeasurements.writeJvmRow(summaryResultPath, runID, CURRENT_SHARD, N_SHARDS, "EFG", "L" + coverageLength,
                handledProducts, failedProducts, maxChildPeakRssKb,
                "StopReason;GenPeakAboveBaseline(MB);EnumeratedConfigurations;SkippedConfigurations;OwnSatTime(ms);SkippedSatTime(ms);" + RunMeasurements.Totals.COLUMNS,
                stopReason + ";" + (globalPeakGenAboveBaselineBytes / (1024 * 1024)) + ";" + enumeratedConfigurations + ";"
                        + skippedConfigurations + ";"
                        + String.format(Locale.ROOT, "%.3f", ownSatTimeNanos / 1_000_000.0) + ";"
                        + String.format(Locale.ROOT, "%.3f", skippedSatTimeNanos / 1_000_000.0) + ";" + checkTotals.toRow());

        System.out.println(String.format(Locale.ROOT,
                "EFG L=%d %s FINISHED. Processed: %d, Failed: %d, Avg Edge Coverage: %.2f%%, Peak Memory: %.2fMB",
                coverageLength, SPLName, handledProducts, failedProducts, avgEdgeCoverage, 
                Math.max(peakGenMemoryMB, peakExecMemoryMB)));
    }
    
    /**
     * Delete EFG temporary files to keep disk usage constant
     * 
     * CRITICAL: This implements the streaming pipeline from skill Design Decision #2:
     * "SAT solver → ESGFx in memory → write temp EFG → GUITAR → measure → delete"
     */
    private void deleteEFGTemporaryFiles(String efgFilePath, String testSequencesFolder) {
        try {
            // Delete EFG XML file
            File efgFile = new File(efgFilePath);
            if (efgFile.exists()) {
                efgFile.delete();
            }
            
            // Delete GUITAR test output folder recursively
            File testSeqDir = new File(testSequencesFolder);
            if (testSeqDir.exists()) {
                deleteDirectory(testSeqDir);
            }
        } catch (Exception e) {
            // Log but don't fail - cleanup is optional for correctness
            System.err.println("Warning: Could not delete temp files: " + e.getMessage());
        }
    }
    
    /**
     * Recursively delete directory
     */
    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
}