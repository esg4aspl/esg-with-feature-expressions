package tr.edu.iyte.esgfx.cases;

import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.ArrayList;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;

import tr.edu.iyte.esgfx.cases.resultrecordingutilities.TestPipelineMeasurementWriter_RandomWalk_ExtremeScalability;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.randomwalktesting.RandomWalkTestGenerator;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EdgeCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.eventcoverage.EventCoverageAnalyser;
import tr.edu.iyte.esgfx.testexecution.TestExecutor;

/**
 * RQ2 Extreme Scalability: Random Walk Baseline
 * 
 * PURPOSE: Demonstrate Random Walk's O(|V|³) time complexity explosion
 * 
 * KEY FINDINGS TO CAPTURE:
 * 1. Unpredictable time (syngo.via: 7.4 hours vs HockertyShirts: 2.7 minutes)
 * 2. Safety limit hits (syngo.via: 110 hits, HockertyShirts: 349 hits)
 * 3. Coverage at termination (most hit 99%+ but can't reach 100%)
 * 4. Memory consumption (syngo.via: 439MB peak)
 * 
 * THEORETICAL BASIS:
 * - Feige (1995): Cover time O(|V|³) for undirected graphs
 * - ESG-Fx is directed → worse case scenarios possible
 * - Safety limit: 5|V|³ steps to prevent infinite loops
 * 
 * IMPLEMENTATION NOTES:
 * - Fixed seed 42 (RQ2 focuses on time, not stochasticity)
 * - Damping factor 0.85 (PageRank standard)
 * - Track time/coverage when safety limit hit
 * - Per-product generator (no reset() needed)
 */
public class RQ2_ExtremeScalability_RandomWalk extends CaseStudyUtilities {

    private static final int RANDOM_WALK_SEED = 42;  // Fixed for RQ2
    private static final double DAMPING_FACTOR = 0.85;
    private static final double TARGET_EDGE_COVERAGE = 100.0;  // Aim for 100%

    public void measureRandomWalkScalability() throws Exception {
        
        int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));
        int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
        int runID = Integer.parseInt(System.getenv().getOrDefault("runID", "1"));
        int timeoutHours = Integer.parseInt(System.getenv().getOrDefault("TIMEOUT_HOURS", "0"));
        long maxDurationNanos = timeoutHours > 0 ? timeoutHours * 60L * 60L * 1_000_000_000L : Long.MAX_VALUE;
        
        coverageLength = 0;  
        setCoverageType();

        System.out.println("Random Walk Extreme Scalability Pipeline " + SPLName + " STARTED");
        
        long totalSatTimeNanos = 0;
        long totalProdGenTimeNanos = 0;
        long totalTestGenTimeNanos = 0;
        long totalTestExecTimeNanos = 0;
        
        long globalPeakMemoryGenBytes = 0;
        long globalPeakMemoryExecBytes = 0;

        int globalTotalVertices = 0;
        int globalTotalEdges = 0;
        long globalTotalTestCases = 0;
        long globalTotalTestEvents = 0;
        long globalTotalAbortedSequences = 0;

        double globalTotalEventCoverage = 0.0;
        double globalTotalEdgeCoverage = 0.0;
        long globalTotalEventCoverageAnalysisTimeNanos = 0;
        long globalTotalEdgeCoverageAnalysisTimeNanos = 0;

        // Safety limit tracking
        int safetyLimitHitCount = 0;
        List<Long> safetyLimitTimes = new ArrayList<>();
        List<Long> safetyLimitSteps = new ArrayList<>();
        List<Double> safetyLimitCoverages = new ArrayList<>();
        
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
            Set<EventSequence> testSequences = null;
            TestExecutor testExecutor = null;
            EventCoverageAnalyser eventCoverageAnalyser = null;
            EdgeCoverageAnalyser edgeCoverageAnalyser = null;
            RandomWalkTestGenerator randomWalkGenerator = null;

            try {
                // 1. PRODUCT GENERATION
                long prodGenStart = System.nanoTime();
                productESGFx = productESGFxGenerator.generateProductESGFx(productID, productName, ESGFx);
                totalProdGenTimeNanos += (System.nanoTime() - prodGenStart);

                if (productESGFx != null) {
                    globalTotalVertices += productESGFx.getVertexList().size();
                    globalTotalEdges += productESGFx.getEdgeList().size();
                }

                // 2. RANDOM WALK TEST GENERATION
                resetPeakMemoryCounters();
                
                // FIX: Instantiate generator per product (stateless)
                randomWalkGenerator = new RandomWalkTestGenerator(
                        (ESGFx) productESGFx,  // Correct type casting
                        DAMPING_FACTOR, 
                        RANDOM_WALK_SEED);
                
                // Calculate safety limit: 5|V|³
                int vertexCount = productESGFx.getVertexList().size();
                int safetyLimit = calculateSafetyLimit(vertexCount);
                
                long testGenStart = System.nanoTime();
                
                // FIX: Use correct API
                testSequences = randomWalkGenerator.generateWalkUntilEdgeCoverage(
                        TARGET_EDGE_COVERAGE,  // 100% edge coverage target
                        safetyLimit);
                
                long testGenEnd = System.nanoTime();
                totalTestGenTimeNanos += (testGenEnd - testGenStart);

                // FIX: Track safety limit hits with CORRECT API
                if (randomWalkGenerator.isSafetyLimitHit()) {
                    safetyLimitHitCount++;
                    safetyLimitTimes.add(randomWalkGenerator.getExecutionTimeMs() * 1_000_000L); // ms → ns
                    safetyLimitSteps.add((long) randomWalkGenerator.getStepsTaken());
                    safetyLimitCoverages.add(randomWalkGenerator.getAchievedCoverage());
                }

                if (testSequences != null) {
                    globalTotalTestCases += testSequences.size();
                    for (EventSequence seq : testSequences) {
                        globalTotalTestEvents += seq.length();
                    }
                    globalTotalAbortedSequences += randomWalkGenerator.getAbortedSequenceCount();
                }

                long currentPeakGenBytes = getPeakHeapMemoryBytes();
                if (currentPeakGenBytes > globalPeakMemoryGenBytes) {
                    globalPeakMemoryGenBytes = currentPeakGenBytes;
                }
                
                // ISOLATION BARRIER
                System.gc();

                // 3. TEST EXECUTION
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

                // 4. COVERAGE ANALYSIS
                long eventCovAnalysisStart = System.nanoTime();
                eventCoverageAnalyser = new EventCoverageAnalyser();
                double currentEventCoverage = eventCoverageAnalyser.analyseEventCoverage(
                        productESGFx, testSequences, featureExpressionMapFromFeatureModel);
                long eventCovAnalysisEnd = System.nanoTime();
                globalTotalEventCoverageAnalysisTimeNanos += (eventCovAnalysisEnd - eventCovAnalysisStart);
                globalTotalEventCoverage += currentEventCoverage;

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
                System.err.println("Random Walk failure on product " + productID + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                // FIX: No reset() needed - generator is per-product
                randomWalkGenerator = null;
                testSequences = null;
                testExecutor = null;
                productESGFx = null;
                eventCoverageAnalyser = null;
                edgeCoverageAnalyser = null;

                if (handledProducts % 100 == 0) {
                    System.out.println("Processed " + handledProducts + " products. Current ID: " + productID 
                            + " (Safety hits: " + safetyLimitHitCount + ")");
                    System.gc();
                }
            }

            satStart = System.nanoTime();
        }

        // AGGREGATION
        double satTimeMs = totalSatTimeNanos / 1_000_000.0;
        double prodGenTimeMs = totalProdGenTimeNanos / 1_000_000.0;
        double testGenTimeMs = totalTestGenTimeNanos / 1_000_000.0;
        double testExecTimeMs = totalTestExecTimeNanos / 1_000_000.0;
        double eventCoverageAnalysisTimeMs = globalTotalEventCoverageAnalysisTimeNanos / 1_000_000.0;
        double edgeCoverageAnalysisTimeMs = globalTotalEdgeCoverageAnalysisTimeNanos / 1_000_000.0;
        
        double timeElapsedTotalMs = satTimeMs + prodGenTimeMs + testGenTimeMs 
                + testExecTimeMs + eventCoverageAnalysisTimeMs + edgeCoverageAnalysisTimeMs;
        
        double avgEventCoverage = handledProducts > 0 ? globalTotalEventCoverage / handledProducts : 0.0;
        double avgEdgeCoverage = handledProducts > 0 ? globalTotalEdgeCoverage / handledProducts : 0.0;

        double peakGenMemoryMB = globalPeakMemoryGenBytes / (1024.0 * 1024.0);
        double peakExecMemoryMB = globalPeakMemoryExecBytes / (1024.0 * 1024.0);

        // Safety limit statistics
        double avgTimeOnSafetyLimitMs = 0.0;
        long avgStepsOnSafetyLimit = 0;
        double avgCoverageAtSafetyLimit = 0.0;
        
        if (safetyLimitHitCount > 0) {
            avgTimeOnSafetyLimitMs = safetyLimitTimes.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0;
            avgStepsOnSafetyLimit = (long) safetyLimitSteps.stream().mapToLong(Long::longValue).average().orElse(0.0);
            avgCoverageAtSafetyLimit = safetyLimitCoverages.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }
        
        String summaryResultPath = (N_SHARDS > 1)
                ? String.format("%sRandomWalk/L0/%s_RandomWalk_L0_shard%02d.csv", 
                        extremeScalabilityTestPipelineMeasurementFolder, SPLName, CURRENT_SHARD)
                : String.format("%sRandomWalk/L0/%s_RandomWalk_L0.csv", 
                        extremeScalabilityTestPipelineMeasurementFolder, SPLName);

        TestPipelineMeasurementWriter_RandomWalk_ExtremeScalability.writeDetailedPipelineMeasurementForRandomWalk(
                runID, timeElapsedTotalMs, satTimeMs, prodGenTimeMs, testGenTimeMs, peakGenMemoryMB,
                globalTotalVertices, globalTotalEdges, globalTotalTestCases, globalTotalTestEvents,
                globalTotalAbortedSequences,
                avgEventCoverage, eventCoverageAnalysisTimeMs,
                avgEdgeCoverage, edgeCoverageAnalysisTimeMs,
                testExecTimeMs, peakExecMemoryMB,
                safetyLimitHitCount, avgTimeOnSafetyLimitMs, avgStepsOnSafetyLimit, avgCoverageAtSafetyLimit,
                handledProducts, failedProducts,
                summaryResultPath, SPLName);

        System.out.println(String.format(Locale.ROOT,
                "Random Walk %s FINISHED. Processed: %d, Failed: %d, Safety Hits: %d, " +
                "Avg Edge Coverage: %.2f%%, Peak Memory: %.2fMB, Total Time: %.2f min",
                SPLName, handledProducts, failedProducts, safetyLimitHitCount, avgEdgeCoverage,
                Math.max(peakGenMemoryMB, peakExecMemoryMB), timeElapsedTotalMs / 60000.0));
    }
    
    /**
     * Calculate safety limit: 5|V|³
     * 
     * Based on Feige (1995) cover time bound for random walks.
     * ESG-Fx graphs are directed, so we use a conservative 5× multiplier.
     */
    private int calculateSafetyLimit(int vertexCount) {
        // Safety limit = 5 × |V|³
        long limit = 5L * vertexCount * vertexCount * vertexCount;
        
        // Cap at Integer.MAX_VALUE to prevent overflow
        if (limit > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        
        return (int) limit;
    }
}