package tr.edu.iyte.esgfx.cases;

import java.util.List;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;

import tr.edu.iyte.esg.model.ESG;

import tr.edu.iyte.esgfx.cases.resultrecordingutilities.TestSequenceGenerationTimeMeasurementWriter;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;

import tr.edu.iyte.esgfx.testgeneration.randomwalktesting.RandomWalkTestGenerator;

public class TotalTimeMeasurement_L0 extends CaseStudyUtilities {

    public void measureToTalTimeForEdgeCoverage() throws Exception {

        featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFilePath,
                ESGFxFilePath);

        List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);

        // --- 1. SHARD CONFIGURATION ---
        int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
        int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));

        // --- TIMERS INITIALIZATION ---
        long totalSatTimeNanos = 0;       // Cumulative time for SAT Solver operations
        long totalProdGenTimeNanos = 0;   // Cumulative time for Product ESG-Fx generation
        long totalTestGenTimeNanos = 0;   // Cumulative time for Random Walk Test generation

        double startTime1 = System.nanoTime();

        // Initialize solver
        SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();
        ISolver solver = SolverFactory.newDefault();
        satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel,
                featureExpressionList);

        int productID = 0;
        int handledProducts = 0; // Counts processed products

        // Start timer for the first SAT solution search
        long satStart = System.nanoTime();

        // Loop infinitely; we will break manually when UNSAT
        while (true) {
            
            // 1. Measure SAT Solving Time
            // This captures the time for found solutions AND the final UNSAT check
            boolean isSatisfiable = solver.isSatisfiable();
            long satEnd = System.nanoTime();
            
            totalSatTimeNanos += (satEnd - satStart);

            // If no more solutions, exit the loop
            if (!isSatisfiable) {
                break;
            }

            productID++;

            int[] model = solver.model();
            for (int i = 0; i < model.length; i++) {
                FeatureExpression featureExpression = featureExpressionList.get(i);
                if (model[i] > 0) {
                    featureExpression.setTruthValue(true);
                } else {
                    featureExpression.setTruthValue(false);
                }
            }

            // Blocking clause - This is part of the Solver logic, so we measure it
            long blockingStart = System.nanoTime();
            VecInt blockingClause = new VecInt();
            for (int literal : solver.model()) {
                blockingClause.push(-literal);
            }
            solver.addClause(blockingClause);
            totalSatTimeNanos += (System.nanoTime() - blockingStart);

            boolean isProductConfigurationValid = isProductConfigurationValid(featureModel,
                    featureExpressionMapFromFeatureModel);

            if (!isProductConfigurationValid) {
                productID--;
                // Reset SAT timer for the next iteration
                satStart = System.nanoTime();
                continue;
            }

            // --- 2. SHARD GATE ---
            if (((productID - 1) % N_SHARDS) != CURRENT_SHARD) {
                // Reset SAT timer for the next iteration before skipping
                satStart = System.nanoTime();
                continue;
            }

            handledProducts++; // Increment count for this shard

            String productName = ProductIDUtil.format(productID);
            String ESGFxName = productName;

            // --- MEASURE PRODUCT GENERATION ---
            long prodGenStart = System.nanoTime();

            ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();
            ESG productESGFx = productESGFxGenerator.generateProductESGFx(productID, ESGFxName, ESGFx);

            totalProdGenTimeNanos += (System.nanoTime() - prodGenStart);

            // --- MEASURE TEST GENERATION ---
            long testGenStart = System.nanoTime();

            // Random-walk generation on product ESG-Fx
            int safetyLimit = (int) (5 * Math.pow((productESGFx.getVertexList().size()), 3));
            RandomWalkTestGenerator rw = new RandomWalkTestGenerator((ESGFx) productESGFx, 0.85);

            rw.generateWalkUntilEdgeCoverage(100, safetyLimit);

            totalTestGenTimeNanos += (System.nanoTime() - testGenStart);

            // Reset SAT timer for the next 'while' check
            satStart = System.nanoTime();
        }

        double stopTime1 = System.nanoTime();
        
        // Convert all times to milliseconds
        double timeElapsedTotalMs = (stopTime1 - startTime1) / 1_000_000.0;
        double satTimeMs = totalSatTimeNanos / 1_000_000.0;
        double prodGenTimeMs = totalProdGenTimeNanos / 1_000_000.0;
        double testGenTimeMs = totalTestGenTimeNanos / 1_000_000.0;

        
        if (N_SHARDS > 1) {
//            System.out.println("Shard " + CURRENT_SHARD + " Completed.");
//            System.out.println("Total Products Processed by this Shard: " + handledProducts);
        	
			String shardResultFilePath = shards_timemeasurement
					+ String.format("timemeasurement_shard%02d.csv", CURRENT_SHARD);

            // Using a hypothetical new method name 'writeDetailedTimeMeasurement'
            TestSequenceGenerationTimeMeasurementWriter.writeDetailedTimeMeasurement(
                    timeElapsedTotalMs, satTimeMs, prodGenTimeMs, testGenTimeMs, handledProducts,
                    shardResultFilePath, SPLName, coverageType);

        } else {
            TestSequenceGenerationTimeMeasurementWriter.writeDetailedTimeMeasurement(
                    timeElapsedTotalMs, satTimeMs, prodGenTimeMs, testGenTimeMs, handledProducts,
                    timemeasurementFolderPath + SPLName + "_" + coverageType + ".csv", SPLName, coverageType);
        }

    }
}