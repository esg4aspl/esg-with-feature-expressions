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

public class RQ3_RandomWalkMultiSeed_TestGenerator extends CaseStudyUtilities {

    private static final long[] SEEDS = {42L, 43L, 44L, 45L, 46L, 47L, 48L, 49L, 50L, 51L};
    private static final double DAMPING_FACTOR = 0.85;

    public void generateMultiSeedRandomWalkTests() throws Exception {
        System.out.println("MULTI-SEED RANDOM WALK TEST GENERATION - SPL: " + SPLName + " STARTED");
        System.out.flush();

        int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
        int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));

        // =====================================================================
        // IN-MEMORY MODE — DISPATCH FLAG BEGIN
        // =====================================================================
        boolean inMemoryMode = TestSuiteFactory.isInMemoryMode();
        System.out.println("RQ3_MODE = " + (inMemoryMode ? "memory (skipping suite file writes)" : "file"));
        // =====================================================================
        // IN-MEMORY MODE — DISPATCH FLAG END
        // =====================================================================

        featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFile, ESGFxFile);

        String dotDirectoryPath = DOTFolder + "L2/";
        File dotDir = new File(dotDirectoryPath);

        if (!dotDir.exists() || !dotDir.isDirectory()) {
            throw new RuntimeException("DOT directory not found: " + dotDirectoryPath);
        }

        File[] dotFiles = dotDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".dot"));
        if (dotFiles == null || dotFiles.length == 0) {
            return;
        }

        Arrays.sort(dotFiles, (f1, f2) -> f1.getName().compareTo(f2.getName()));

        // =====================================================================
        // IN-MEMORY MODE — SEED DIR CREATION GUARD BEGIN
        // =====================================================================
        if (!inMemoryMode) {
            for (long seed : SEEDS) {
                String seedDir = testsequencesFolder + "L0/seed" + seed + "/";
                new File(seedDir).mkdirs();
            }
        }
        // =====================================================================
        // IN-MEMORY MODE — SEED DIR CREATION GUARD END
        // =====================================================================

        String logDir = testsequencesFolder + "L0/";
        new File(logDir).mkdirs();
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
                        continue;
                    }

                    int vertexCount = productESGFx.getVertexList().size();
                    int maxSteps = Math.min(5 * vertexCount * vertexCount * vertexCount, 2_000_000);

                    for (long seed : SEEDS) {
                        RandomWalkTestGenerator rwGenerator = new RandomWalkTestGenerator(
                                (ESGFx) productESGFx, DAMPING_FACTOR, seed);

                        Set<EventSequence> testSequences = rwGenerator.generateWalkUntilEdgeCoverage(100.0, maxSteps);

                        int numTestCases = testSequences.size();
                        int numTestEvents = 0;
                        for (EventSequence seq : testSequences) {
                            numTestEvents += seq.length();
                        }

                        // =====================================================================
                        // IN-MEMORY MODE — SUITE FILE WRITE GUARD BEGIN
                        // =====================================================================
                        if (!inMemoryMode) {
                            String outputPath = testsequencesFolder + "L0/seed" + seed + "/"
                                    + productName + "_RandomWalk.txt";
                            TestSuiteFileWriter.writeEventSequenceSetAndCoverageAnalysisToFile(
                                    outputPath, testSequences, "L0_seed" + seed,
                                    rwGenerator.getAchievedCoverage());
                        }
                        // =====================================================================
                        // IN-MEMORY MODE — SUITE FILE WRITE GUARD END
                        // =====================================================================

                        logWriter.println(productName + ";" + seed + ";" + numTestCases + ";"
                                + numTestEvents + ";" + rwGenerator.getAchievedCoverage() + ";"
                                + rwGenerator.getStepsTaken() + ";" + rwGenerator.isSafetyLimitHit());
                        logWriter.flush();
                    }
                } catch (OutOfMemoryError oom) {
                    System.gc();
                } catch (Exception e) {
                    System.err.println("Error for product " + productName + ": " + e.getMessage());
                } finally {
                    productESGFx = null;
                    if (handledProducts % 50 == 0) {
                        System.gc();
                    }
                }
            }
        }

        System.out.println("MULTI-SEED RANDOM WALK TEST GENERATION - Shard " + CURRENT_SHARD + " FINISHED");
    }
}
