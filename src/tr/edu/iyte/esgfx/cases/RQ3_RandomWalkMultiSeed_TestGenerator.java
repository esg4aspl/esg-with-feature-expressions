package tr.edu.iyte.esgfx.cases;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Set;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esgfx.conversion.dot.DOTFileToESGFxConverter;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.testgeneration.TestSuiteFileWriter;
import tr.edu.iyte.esgfx.testgeneration.randomwalktesting.RandomWalkTestGenerator;

/**
 * Generates Random Walk test suites for multiple seeds.
 * 
 * For each product, this class runs the Random Walk test generator with 10 different seeds
 * and saves the resulting test suites into seed-specific subfolders:
 *   testsequences/L0/seed42/productName_RandomWalk.txt
 *   testsequences/L0/seed43/productName_RandomWalk.txt
 *   ...
 * 
 * This enables RQ3 fault detection to evaluate Random Walk across multiple seeds,
 * capturing the stochastic variation inherent in random testing.
 * 
 * Usage: Set environment variables N_SHARDS, SHARD for parallelization.
 *        Seeds are hardcoded: {42, 43, 44, 45, 46, 47, 48, 49, 50, 51}
 */
public class RQ3_RandomWalkMultiSeed_TestGenerator extends CaseStudyUtilities {

    private static final long[] SEEDS = {42L, 43L, 44L, 45L, 46L, 47L, 48L, 49L, 50L, 51L};
    private static final double DAMPING_FACTOR = 0.85;

    public void generateMultiSeedRandomWalkTests() throws Exception {
        System.out.println("MULTI-SEED RANDOM WALK TEST GENERATION - SPL: " + SPLName + " STARTED");
        System.out.flush();

        int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
        int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));
        System.out.println("DEBUG: Shards parsed. N=" + N_SHARDS + " CURRENT=" + CURRENT_SHARD);
        System.out.flush();

        System.out.println("DEBUG: Generating feature expression map...");
        System.out.flush();
        featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFile, ESGFxFile);
        System.out.println("DEBUG: Feature expression map generated. Size=" + featureExpressionMapFromFeatureModel.size());
        System.out.flush();

        String dotDirectoryPath = DOTFolder + "L2/";
        File dotDir = new File(dotDirectoryPath);
        System.out.println("DEBUG: DOT dir=" + dotDirectoryPath + " exists=" + dotDir.exists());
        System.out.flush();

        if (!dotDir.exists() || !dotDir.isDirectory()) {
            throw new RuntimeException("DOT directory not found: " + dotDirectoryPath);
        }

        File[] dotFiles = dotDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".dot"));
        if (dotFiles == null || dotFiles.length == 0) {
            System.out.println("No DOT files found.");
            return;
        }
        System.out.println("DEBUG: dotFiles count=" + (dotFiles == null ? "null" : dotFiles.length));
        System.out.flush();

        Arrays.sort(dotFiles, (f1, f2) -> f1.getName().compareTo(f2.getName()));

        // Create seed directories
        for (long seed : SEEDS) {
            String seedDir = testsequencesFolder + "L0/seed" + seed + "/";
            new File(seedDir).mkdirs();
        }
        
        System.out.println("DEBUG: Entering product loop...");
        System.out.flush();

        // Log file for tracking generation metadata
        String logDir = testsequencesFolder + "L0/";
        String logPath = logDir + SPLName + "_MultiSeedRW_shard" + String.format("%02d", CURRENT_SHARD) + ".log";

        int productID = 0;
        int handledProducts = 0;

        try (PrintWriter logWriter = new PrintWriter(new FileWriter(logPath, true))) {
            logWriter.println("ProductID;Seed;NumTestCases;NumTestEvents;AchievedCoverage;StepsTaken;SafetyLimitHit");

            for (File dotFile : dotFiles) {
                productID++;
                if (((productID - 1) % N_SHARDS) != CURRENT_SHARD) {
                    continue;
                }

                handledProducts++;
                String productName = dotFile.getName().replaceAll("(?i)\\.dot", "");
                String configFilePath = productConfigurationFolder + productName + ".config";

                ESG productESGFx = null;

                try {
                    updateFeatureExpressionMapFromConfigFile(configFilePath);

                    productESGFx = DOTFileToESGFxConverter.parseDOTFileForESGFxCreation(
                            dotFile.getAbsolutePath(), featureExpressionMapFromFeatureModel);

                    if (productESGFx == null) {
                        System.err.println("Could not parse ESGFx for: " + productName);
                        continue;
                    }

                    int vertexCount = productESGFx.getVertexList().size();
                    int maxSteps = Math.min(
                    	    5 * vertexCount * vertexCount * vertexCount,
                    	    2_000_000
                    	);

                    for (long seed : SEEDS) {
                        RandomWalkTestGenerator rwGenerator = new RandomWalkTestGenerator(
                                (ESGFx) productESGFx, DAMPING_FACTOR, seed);

                        Set<EventSequence> testSequences = rwGenerator.generateWalkUntilEdgeCoverage(100.0, maxSteps);

                        int numTestCases = testSequences.size();
                        int numTestEvents = 0;
                        for (EventSequence seq : testSequences) {
                            numTestEvents += seq.length();
                        }

                        // Save to seed-specific folder
                        String outputPath = testsequencesFolder + "L0/seed" + seed + "/" 
                                + productName + "_RandomWalk.txt";

                        TestSuiteFileWriter.writeEventSequenceSetAndCoverageAnalysisToFile(
                                outputPath, testSequences, "L0_seed" + seed, 
                                rwGenerator.getAchievedCoverage());

                        // Log metadata
                        logWriter.println(productName + ";" + seed + ";" + numTestCases + ";" 
                                + numTestEvents + ";" + rwGenerator.getAchievedCoverage() + ";" 
                                + rwGenerator.getStepsTaken() + ";" + rwGenerator.isSafetyLimitHit());
                        logWriter.flush();
                    }

                } catch (OutOfMemoryError oom) {
                    System.err.println("OOM for product: " + productName);
                    System.gc();
                } catch (Exception e) {
                    System.err.println("Error for product " + productName + ": " + e.getMessage());
                } finally {
                    productESGFx = null;
                    if (handledProducts % 50 == 0) {
                        System.gc();
                        System.out.println("Handled products: " + handledProducts);
                    }
                }
            }
        }

        System.out.println("MULTI-SEED RANDOM WALK TEST GENERATION - Shard " + CURRENT_SHARD + " FINISHED");
    }
}