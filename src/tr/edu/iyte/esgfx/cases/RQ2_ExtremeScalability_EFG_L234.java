package tr.edu.iyte.esgfx.cases;

import java.util.List;
import java.util.Set;
import java.util.Locale;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;

import tr.edu.iyte.esgfx.cases.resultrecordingutilities.TestPipelineMeasurementWriter_EFG_ExtremeScalability;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.conversion.mxe.EFGConversionFacade;
import tr.edu.iyte.esgfx.testgeneration.guitartestgeneration.GUITARTestGeneratorFacade;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EdgeCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.eventcoverage.EventCoverageAnalyser;
import tr.edu.iyte.esgfx.testexecution.TestExecutor;

/**
 * RQ2 Extreme Scalability: EFG Baseline
 * 
 * PURPOSE: Demonstrate EFG's scalability limitations on large SPLs
 * 
 * KEY FINDINGS TO CAPTURE:
 * 1. Coverage degradation (syngo.via: 4-10%, HockertyShirts: 37-40%)
 * 2. Time cost (2-5 minutes per 400 products)
 * 3. Failure modes (OOM, GUITAR crashes)
 * 
 * IMPLEMENTATION NOTES:
 * - Streaming pipeline: SAT → ESGFx in memory → temp EFG → GUITAR → measure → delete
 * - No DOT files generated (disk-efficient)
 * - Crash recovery via CSV last-row check
 * - Edge coverage tracking is CRITICAL for validity assessment
 */
public class RQ2_ExtremeScalability_EFG_L234 extends CaseStudyUtilities {

    public void measureEFGScalability() throws Exception {
        
        int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));
        int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
        int L_LEVEL = Integer.parseInt(System.getenv().getOrDefault("L_LEVEL", "2"));
        int runID = Integer.parseInt(System.getenv().getOrDefault("runID", "1"));
        int timeoutHours = Integer.parseInt(System.getenv().getOrDefault("TIMEOUT_HOURS", "0"));
        long maxDurationNanos = timeoutHours > 0 ? timeoutHours * 60L * 60L * 1_000_000_000L : Long.MAX_VALUE;
        
        coverageLength = L_LEVEL;
        setCoverageType();

        System.out.println("EFG Extreme Scalability Pipeline L=" + coverageLength + " " + SPLName + " STARTED");
        
        long totalSatTimeNanos = 0;
        long totalProdGenTimeNanos = 0;
        long totalEFGTransformationTimeNanos = 0;  // ESGFx → EFG conversion
        long totalTestGenTimeNanos = 0;            // GUITAR test generation
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

        SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();
        ISolver solver = SolverFactory.newDefault();
        satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel,
                featureExpressionList);

        int productID = 0;
        int handledProducts = 0;
        int failedProducts = 0;

        ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();
        EFGConversionFacade efgConverter = new EFGConversionFacade();
        GUITARTestGeneratorFacade guitarGenerator = new GUITARTestGeneratorFacade(coverageLength);

        long satStart = System.nanoTime();

        while (true) {
            
            if (System.nanoTime() - startTime1 >= maxDurationNanos) {
                System.out.println("Timeout reached. Processed products: " + handledProducts);
                break;
            }

            boolean isSatisfiable = solver.isSatisfiable();
            long satEnd = System.nanoTime();
            totalSatTimeNanos += (satEnd - satStart);

            if (!isSatisfiable) break;

            productID++;

            int[] model = solver.model();
            for (int i = 0; i < model.length; i++) {
                FeatureExpression fe = featureExpressionList.get(i);
                fe.setTruthValue(model[i] > 0);
            }

            long blockingStart = System.nanoTime();
            VecInt blockingClause = new VecInt();
            for (int literal : solver.model()) {
                blockingClause.push(-literal);
            }
            solver.addClause(blockingClause);
            totalSatTimeNanos += (System.nanoTime() - blockingStart);

            boolean isProductConfigurationValid = isProductConfigurationValid(featureModel,
                    featureExpressionMapFromFeatureModel);

            if (!isProductConfigurationValid || ((productID - 1) % N_SHARDS) != CURRENT_SHARD) {
                satStart = System.nanoTime();
                continue;
            }

            handledProducts++;
            String productName = ProductIDUtil.format(productID);

            ESG productESGFx = null;
            ESG productEFG = null;
            Set<EventSequence> testSequences = null;
            TestExecutor testExecutor = null;
            EventCoverageAnalyser eventCoverageAnalyser = null;
            EdgeCoverageAnalyser edgeCoverageAnalyser = null;

            try {
                // 1. PRODUCT GENERATION
                long prodGenStart = System.nanoTime();
                productESGFx = productESGFxGenerator.generateProductESGFx(productID, productName, ESGFx);
                totalProdGenTimeNanos += (System.nanoTime() - prodGenStart);

                // 2. EFG TRANSFORMATION (ESGFx → EFG)
                resetPeakMemoryCounters();
                long efgTransformStart = System.nanoTime();
                productEFG = efgConverter.convertToEFG(productESGFx, coverageLength);
                long efgTransformEnd = System.nanoTime();
                totalEFGTransformationTimeNanos += (efgTransformEnd - efgTransformStart);

                if (productEFG != null) {
                    globalTotalEFGVertices += productEFG.getVertexList().size();
                    globalTotalEFGEdges += productEFG.getEdgeList().size();
                }

                // 3. GUITAR TEST GENERATION
                long testGenStart = System.nanoTime();
                testSequences = guitarGenerator.generateTests(productEFG);
                totalTestGenTimeNanos += (System.nanoTime() - testGenStart);

                if (testSequences != null) {
                    globalTotalTestCases += testSequences.size();
                    for (EventSequence seq : testSequences) {
                        globalTotalTestEvents += seq.length();
                    }
                }

                long currentPeakGenBytes = getPeakHeapMemoryBytes();
                if (currentPeakGenBytes > globalPeakMemoryGenBytes) {
                    globalPeakMemoryGenBytes = currentPeakGenBytes;
                }
                
                // ISOLATION BARRIER
                System.gc();

                // 4. TEST EXECUTION
                resetPeakMemoryCounters();
                long testExecStart = System.nanoTime();
                if (testSequences != null && !testSequences.isEmpty()) {
                    testExecutor = new TestExecutor(testSequences);
                    testExecutor.executeAllTests(productESGFx);  // Execute on ESGFx
                }
                totalTestExecTimeNanos += (System.nanoTime() - testExecStart);

                long currentPeakExecBytes = getPeakHeapMemoryBytes();
                if (currentPeakExecBytes > globalPeakMemoryExecBytes) {
                    globalPeakMemoryExecBytes = currentPeakExecBytes;
                }

                // 5. COVERAGE ANALYSIS - BOTH EVENT AND EDGE
                // Event coverage
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

            } catch (OutOfMemoryError oom) {
                failedProducts++;
                System.err.println("OOM on product " + productID);
                System.gc();
            } catch (Exception e) {
                failedProducts++;
                System.err.println("GUITAR/EFG failure on product " + productID + ": " + e.getMessage());
            } finally {
                // Cleanup
                testSequences = null;
                testExecutor = null;
                productESGFx = null;
                productEFG = null;
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
        double testExecTimeMs = totalTestExecTimeNanos / 1_000_000.0;
        double eventCoverageAnalysisTimeMs = globalTotalEventCoverageAnalysisTimeNanos / 1_000_000.0;
        double edgeCoverageAnalysisTimeMs = globalTotalEdgeCoverageAnalysisTimeNanos / 1_000_000.0;
        
        double timeElapsedTotalMs = satTimeMs + prodGenTimeMs + efgTransformationTimeMs 
                + testGenTimeMs + testExecTimeMs + eventCoverageAnalysisTimeMs + edgeCoverageAnalysisTimeMs;
        
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
                runID, timeElapsedTotalMs, satTimeMs, prodGenTimeMs, efgTransformationTimeMs, 
                testGenTimeMs, peakGenMemoryMB, 
                globalTotalEFGVertices, globalTotalEFGEdges, globalTotalTestCases, globalTotalTestEvents,
                avgEventCoverage, eventCoverageAnalysisTimeMs,
                avgEdgeCoverage, edgeCoverageAnalysisTimeMs,  // CRITICAL
                testExecTimeMs, peakExecMemoryMB, 
                handledProducts, failedProducts, 
                summaryResultPath, SPLName, coverageType);

        System.out.println(String.format(Locale.ROOT,
                "EFG L=%d %s FINISHED. Processed: %d, Failed: %d, Avg Edge Coverage: %.2f%%, Peak Memory: %.2fMB",
                coverageLength, SPLName, handledProducts, failedProducts, avgEdgeCoverage, 
                Math.max(peakGenMemoryMB, peakExecMemoryMB)));
    }
}