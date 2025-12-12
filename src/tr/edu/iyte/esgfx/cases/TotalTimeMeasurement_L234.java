package tr.edu.iyte.esgfx.cases;

import java.util.List;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.cases.resultrecordingutilities.TestSequenceGenerationTimeMeasurementWriter;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EulerCycleGeneratorForEdgeCoverage;
import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;
import tr.edu.iyte.esgfx.testgeneration.eventtriplecoverage.TransformedESGFxGenerator;

public class TotalTimeMeasurement_L234 extends CaseStudyUtilities {

    public void measureToTalTimeForEdgeCoverage() throws Exception {
        System.out.println("Total Time Measurement L=" + coverageLength + " " +  SPLName + " STARTED");
        featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFilePath, ESGFxFilePath);
        List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);

        // --- 1. SHARD CONFIGURATION ---
        int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
        int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));

        // --- TIMERS INITIALIZATION ---
        long totalSatTimeNanos = 0;
        long totalProdGenTimeNanos = 0;
        long totalTestGenTimeNanos = 0;

        double startTime1 = System.nanoTime();

        // Initialize solver
        SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();
        ISolver solver = SolverFactory.newDefault();
        satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel, featureExpressionList);

        int productID = 0;
        int handledProducts = 0; 

        ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();
        TransformedESGFxGenerator transformedESGFxGenerator = new TransformedESGFxGenerator();
        EulerCycleGeneratorForEdgeCoverage eulerCycleGeneratorForEdgeCoverage = new EulerCycleGeneratorForEdgeCoverage();
        EulerCycleToTestSequenceGenerator eulerCycleToTestSequenceGenerator = new EulerCycleToTestSequenceGenerator();

        long satStart = System.nanoTime();

        // Loop infinitely
        while (true) {
            
            boolean isSatisfiable = solver.isSatisfiable();
            long satEnd = System.nanoTime();
            totalSatTimeNanos += (satEnd - satStart);

            if (!isSatisfiable) break;

            productID++;

            int[] model = solver.model();
            for (int i = 0; i < model.length; i++) {
                FeatureExpression fe = featureExpressionList.get(i);
                if (model[i] > 0) fe.setTruthValue(true);
                else fe.setTruthValue(false);
            }

            long blockingStart = System.nanoTime();
            VecInt blockingClause = new VecInt();
            for (int literal : solver.model()) blockingClause.push(-literal);
            solver.addClause(blockingClause);
            totalSatTimeNanos += (System.nanoTime() - blockingStart);

            boolean isProductConfigurationValid = isProductConfigurationValid(featureModel, featureExpressionMapFromFeatureModel);

            if (!isProductConfigurationValid) {
                satStart = System.nanoTime();
                continue;
            }

            if (((productID - 1) % N_SHARDS) != CURRENT_SHARD) {
                satStart = System.nanoTime();
                continue;
            }

            handledProducts++; 
            String productName = ProductIDUtil.format(productID);
            String ESGFxName = productName;

            // --- MEASURE PRODUCT GENERATION ---
            long prodGenStart = System.nanoTime();
            ESG productESGFx = productESGFxGenerator.generateProductESGFx(productID, ESGFxName, ESGFx);
            totalProdGenTimeNanos += (System.nanoTime() - prodGenStart);

            // --- MEASURE TEST GENERATION ---
            long testGenStart = System.nanoTime();


            ESG transformedProductESGFx = transformedESGFxGenerator.generateTransformedESGFx(coverageLength, productESGFx);

            productESGFx = null;
            ESG stronglyConnectedBalancedESGFx = StronglyConnectedBalancedESGFxGeneration.getStronglyConnectedBalancedESGFxGeneration(transformedProductESGFx);
            transformedProductESGFx = null; 

            // 3. Generate Euler Cycle
            eulerCycleGeneratorForEdgeCoverage.generateEulerCycle(stronglyConnectedBalancedESGFx);
            stronglyConnectedBalancedESGFx = null;
            
            List<Vertex> eulerCycle = eulerCycleGeneratorForEdgeCoverage.getEulerCycle();
            
            
            // 4. Generate Test Sequences
            eulerCycleToTestSequenceGenerator.CESgenerator(eulerCycle);
            
            totalTestGenTimeNanos += (System.nanoTime() - testGenStart);
            
            eulerCycleGeneratorForEdgeCoverage.reset();
            eulerCycle = null;
            
            // Force GC
            if (handledProducts % 50 == 0) System.gc(); 

            satStart = System.nanoTime();
        }

        double stopTime1 = System.nanoTime();
        
        double timeElapsedTotalMs = (stopTime1 - startTime1) / 1_000_000.0;
        double satTimeMs = totalSatTimeNanos / 1_000_000.0;
        double prodGenTimeMs = totalProdGenTimeNanos / 1_000_000.0;
        double testGenTimeMs = totalTestGenTimeNanos / 1_000_000.0;

        if (N_SHARDS > 1) {
            String shardResultFilePath = shards_timemeasurement + String.format("timemeasurement_shard%02d.csv", CURRENT_SHARD);
            TestSequenceGenerationTimeMeasurementWriter.writeDetailedTimeMeasurement(
                    timeElapsedTotalMs, satTimeMs, prodGenTimeMs, testGenTimeMs, handledProducts,
                    shardResultFilePath, SPLName, coverageType);
        } else {
            TestSequenceGenerationTimeMeasurementWriter.writeDetailedTimeMeasurement(
                    timeElapsedTotalMs, satTimeMs, prodGenTimeMs, testGenTimeMs, handledProducts,
                    timemeasurementFolderPath + SPLName + "_" + coverageType + ".csv", SPLName, coverageType);
        }
        System.out.println("Total Time Measurement L=" + coverageLength + " " +  SPLName + " FINISHED");
    }
}