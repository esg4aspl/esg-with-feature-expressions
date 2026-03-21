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
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EdgeCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EulerCycleGeneratorForEdgeCoverage;
import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;
import tr.edu.iyte.esgfx.testgeneration.eventtriplecoverage.TransformedESGFxGenerator;
import tr.edu.iyte.esgfx.testexecution.TestExecutor;

public class RQ2_ExtremeScalability_L234 extends CaseStudyUtilities {

    public void measureToTalTimeForEdgeCoverage() throws Exception {
        
        int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));
        int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
        int L_LEVEL = Integer.parseInt(System.getenv().getOrDefault("L_LEVEL", "2"));
        int runID = Integer.parseInt(System.getenv().getOrDefault("runID", "3"));
        int timeoutHours = Integer.parseInt(System.getenv().getOrDefault("TIMEOUT_HOURS", "0"));
        long maxDurationNanos = timeoutHours > 0 ? timeoutHours * 60L * 60L * 1_000_000_000L : Long.MAX_VALUE;
        
        coverageLength = L_LEVEL;
        setCoverageType();

        System.out.println("Test Generation And Execution Pipeline L=" + coverageLength + " " + SPLName + " STARTED");
        
        long totalSatTimeNanos = 0;
        long totalProdGenTimeNanos = 0;
        long totalTransformationTimeNanos = 0; 
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

        SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();
        ISolver solver = SolverFactory.newDefault();
        satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel,
                featureExpressionList);

        int productID = 0;
        int handledProducts = 0;
        int failedProducts = 0;

        ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();
        TransformedESGFxGenerator transformedESGFxGenerator = new TransformedESGFxGenerator();
        EulerCycleGeneratorForEdgeCoverage eulerCycleGeneratorForEdgeCoverage = new EulerCycleGeneratorForEdgeCoverage();
        EulerCycleToTestSequenceGenerator eulerCycleToTestSequenceGenerator = new EulerCycleToTestSequenceGenerator();

        long satStart = System.nanoTime();

        while (true) {
            
            if (System.nanoTime() - startTime1 >= maxDurationNanos) {
                System.out.println("Timeout reached. Processed products within limit: " + handledProducts);
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
            ESG transformedProductESGFx = null;
            ESG stronglyConnectedBalancedESGFx = null;
            Set<EventSequence> testSequences = null;
            List<Vertex> eulerCycle = null;
            TestExecutor testExecutor = null;
            EdgeCoverageAnalyser edgeCoverageAnalyser = null;

            try {
                // 1. MEASURE PRODUCT GENERATION 
                long prodGenStart = System.nanoTime();
                productESGFx = productESGFxGenerator.generateProductESGFx(productID, productName, ESGFx);
                totalProdGenTimeNanos += (System.nanoTime() - prodGenStart);

                if (productESGFx != null) {
                    globalTotalESGFxVertices += productESGFx.getVertexList().size();
                    globalTotalESGFxEdges += productESGFx.getEdgeList().size();
                }

                // 2. MEASURE TEST GENERATION & TRANSFORMATION
                resetPeakMemoryCounters();
                long testGenStart = System.nanoTime();

                long transformStart = System.nanoTime();
                transformedProductESGFx = transformedESGFxGenerator.generateTransformedESGFx(coverageLength, productESGFx);
                long transformEnd = System.nanoTime();
                totalTransformationTimeNanos += (transformEnd - transformStart);

                stronglyConnectedBalancedESGFx = StronglyConnectedBalancedESGFxGeneration
                        .getStronglyConnectedBalancedESGFxGeneration(transformedProductESGFx);
                transformedProductESGFx = null; // GC Assist

                eulerCycleGeneratorForEdgeCoverage.generateEulerCycle(stronglyConnectedBalancedESGFx);
                stronglyConnectedBalancedESGFx = null; // GC Assist

                eulerCycle = eulerCycleGeneratorForEdgeCoverage.getEulerCycle();
                testSequences = eulerCycleToTestSequenceGenerator.CESgenerator(eulerCycle);

                totalTestGenTimeNanos += (System.nanoTime() - testGenStart);

                if (testSequences != null) {
                    globalTotalTestCases += testSequences.size();
                    for (EventSequence seq : testSequences) {
                        globalTotalTestEvents += seq.length();
                    }
                }

                // Measure Generation Memory
                long currentPeakGenBytes = getPeakHeapMemoryBytes();
                if (currentPeakGenBytes > globalPeakMemoryGenBytes) {
                    globalPeakMemoryGenBytes = currentPeakGenBytes;
                }
                
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

                // Measure Execution Memory
                long currentPeakExecBytes = getPeakHeapMemoryBytes();
                if (currentPeakExecBytes > globalPeakMemoryExecBytes) {
                    globalPeakMemoryExecBytes = currentPeakExecBytes;
                }

                // 4. MEASURE COVERAGE ANALYSIS
                long coverageAnalysisStart = System.nanoTime();
                edgeCoverageAnalyser = new EdgeCoverageAnalyser();
                double currentCoverage = edgeCoverageAnalyser.analyseEdgeCoverage(productESGFx, testSequences, featureExpressionMapFromFeatureModel);
                long coverageAnalysisEnd = System.nanoTime();
                
                globalTotalCoverageAnalysisTimeNanos += (coverageAnalysisEnd - coverageAnalysisStart);
                globalTotalCoverage += currentCoverage;

            } catch (OutOfMemoryError oom) {
                failedProducts++;
                System.gc(); 
            } catch (Exception e) {
                failedProducts++;
            } finally {
                // 5. CLEANUP
                eulerCycleGeneratorForEdgeCoverage.reset();
                eulerCycleToTestSequenceGenerator.reset();
                eulerCycle = null;
                testSequences = null;
                testExecutor = null;
                productESGFx = null;
                edgeCoverageAnalyser = null;

                if (handledProducts % 100 == 0) {
                    System.out.println("Processed " + handledProducts + " products. Current product ID: " + productID);
                    System.gc();
                }
            }

            satStart = System.nanoTime();
        }

        double satTimeMs = totalSatTimeNanos / 1_000_000.0;
        double prodGenTimeMs = totalProdGenTimeNanos / 1_000_000.0;
        double transformationTimeMs = totalTransformationTimeNanos / 1_000_000.0;
        double testGenTimeMs = totalTestGenTimeNanos / 1_000_000.0;
        double testExecTimeMs = totalTestExecTimeNanos / 1_000_000.0;
        double coverageAnalysisTimeMs = globalTotalCoverageAnalysisTimeNanos / 1_000_000.0;
        
 
        double timeElapsedTotalMs = satTimeMs + prodGenTimeMs + transformationTimeMs + testGenTimeMs + testExecTimeMs + coverageAnalysisTimeMs;
        
        double avgCoverage = handledProducts > 0 ? globalTotalCoverage / handledProducts : 0.0;

        double peakGenMemoryMB = globalPeakMemoryGenBytes / (1024.0 * 1024.0);
        double peakExecMemoryMB = globalPeakMemoryExecBytes / (1024.0 * 1024.0);
        
        // FIX: Corrected path generation for RQ2 to match L1 and prevent broken file names
        String summaryResultPath = (N_SHARDS > 1)
                ? String.format("%sESG-Fx/%s/%s_ESG-Fx_L%d_shard%02d.csv", extremeScalabilityTestPipelineMeasurementFolder, coverageType, SPLName, coverageLength, CURRENT_SHARD)
                : String.format("%sESG-Fx/%s/%s_ESG-Fx_L%d.csv", extremeScalabilityTestPipelineMeasurementFolder, coverageType, SPLName, coverageLength);

        TestPipelineMeasurementWriter_ExtremeScalability.writeDetailedPipelineMeasurementForESGFx_L234(runID,
                timeElapsedTotalMs, satTimeMs, prodGenTimeMs, transformationTimeMs, testGenTimeMs, peakGenMemoryMB, 
                globalTotalESGFxVertices, globalTotalESGFxEdges, globalTotalTestCases, globalTotalTestEvents, 
                avgCoverage, coverageAnalysisTimeMs, testExecTimeMs, peakExecMemoryMB, 
                handledProducts, failedProducts, summaryResultPath, SPLName, coverageType);

        System.out.println("Total Time Measurement L=" + coverageLength + " " + SPLName + " FINISHED. Peak Memory: "
                + String.format(Locale.ROOT, "%.2f", Math.max(peakGenMemoryMB, peakExecMemoryMB)) + " MB");
    }
}