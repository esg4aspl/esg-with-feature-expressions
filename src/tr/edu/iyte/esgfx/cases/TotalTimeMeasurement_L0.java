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

        featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFilePath, ESGFxFilePath);
        List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);

        int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
        int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));

        long totalSatTimeNanos = 0;
        long totalProdGenTimeNanos = 0;
        long totalTestGenTimeNanos = 0;

        double startTime1 = System.nanoTime();

        SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();
        ISolver solver = SolverFactory.newDefault();
        satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel, featureExpressionList);

        int productID = 0;
        int handledProducts = 0;

       
        ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();

        long satStart = System.nanoTime();

        while (true) {
            
            // 1. SAT Time Measurement
            boolean isSatisfiable = solver.isSatisfiable();
            long satEnd = System.nanoTime();
            totalSatTimeNanos += (satEnd - satStart);

            if (!isSatisfiable) break;

            productID++;

            // Model processing (Logic kept same)
            int[] model = solver.model();
            for (int i = 0; i < model.length; i++) {
                FeatureExpression fe = featureExpressionList.get(i);
                if (model[i] > 0) fe.setTruthValue(true);
                else fe.setTruthValue(false);
            }

            long blockingStart = System.nanoTime();
            VecInt blockingClause = new VecInt();
            for (int literal : model) blockingClause.push(-literal);
            solver.addClause(blockingClause);
            totalSatTimeNanos += (System.nanoTime() - blockingStart);

            boolean isProductConfigurationValid = isProductConfigurationValid(featureModel, featureExpressionMapFromFeatureModel);

            if (!isProductConfigurationValid) {
                //productID--; // ID logic is tricky, keep your original if needed
                satStart = System.nanoTime(); // Reset timer
                continue;
            }

            if (((productID - 1) % N_SHARDS) != CURRENT_SHARD) {
                satStart = System.nanoTime(); // Reset timer
                continue;
            }

            handledProducts++;
            String productName = ProductIDUtil.format(productID);
            
            // 2. Product Generation Measurement
            long prodGenStart = System.nanoTime();
            ESG productESGFx = productESGFxGenerator.generateProductESGFx(productID, productName, ESGFx);
            totalProdGenTimeNanos += (System.nanoTime() - prodGenStart);

            // 3. Test Generation Measurement
            long testGenStart = System.nanoTime();
            int safetyLimit = (int) (5 * Math.pow((productESGFx.getVertexList().size()), 3));
            
            RandomWalkTestGenerator rw = new RandomWalkTestGenerator((ESGFx) productESGFx, 0.85);
            rw.generateWalkUntilEdgeCoverage(100, safetyLimit);
            
            totalTestGenTimeNanos += (System.nanoTime() - testGenStart);

            // OPTIMIZATION 2: Explicit Cleanup
            rw = null;
            productESGFx = null;
            
            // OPTIMIZATION 3: GC Hint
            if (handledProducts % 100 == 0) System.gc();

            satStart = System.nanoTime(); // Reset for next loop
        }

        double stopTime1 = System.nanoTime();
        
        // Results writing (Logic kept same)
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
    }
}