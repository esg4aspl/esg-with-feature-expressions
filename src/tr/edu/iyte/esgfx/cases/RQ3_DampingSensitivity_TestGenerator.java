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

public class RQ3_DampingSensitivity_TestGenerator extends CaseStudyUtilities {

    private static final long SEED = 42L;
    private static final double[] DAMPING_FACTORS = {0.80, 0.85, 0.90};

    public void generateDampingSensitivityTests() throws Exception {
        System.out.println("DAMPING SENSITIVITY TEST GENERATION - SPL: " + SPLName + " STARTED");

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
        // IN-MEMORY MODE — DAMPING DIR CREATION GUARD BEGIN
        // =====================================================================
        if (!inMemoryMode) {
            for (double df : DAMPING_FACTORS) {
                String dampingDir = testsequencesFolder + "L0/damping" + formatDampingFolder(df) + "/";
                new File(dampingDir).mkdirs();
            }
        }
        // =====================================================================
        // IN-MEMORY MODE — DAMPING DIR CREATION GUARD END
        // =====================================================================

        String summaryDir = faultDetectionFolder + "sensitivity/";
        new File(summaryDir).mkdirs();
        String summaryPath = summaryDir + SPLName + "_DampingSensitivity_TestGen_shard"
                + String.format("%02d", CURRENT_SHARD) + ".csv";

        int productID = 0;
        int handledProducts = 0;

        try (PrintWriter summaryWriter = new PrintWriter(new FileWriter(summaryPath, true))) {

            File summaryFile = new File(summaryPath);
            if (summaryFile.length() == 0) {
                summaryWriter.println("SPL;ProductID;DampingFactor;NumTestCases;NumTestEvents;"
                        + "AchievedEdgeCoverage(%);StepsTaken;SafetyLimitHit");
            }

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
                    int safetyLimit = Math.min(5 * vertexCount * vertexCount * vertexCount, 2_000_000);

                    for (double dampingFactor : DAMPING_FACTORS) {
                        RandomWalkTestGenerator rwGenerator = new RandomWalkTestGenerator(
                                (ESGFx) productESGFx, dampingFactor, SEED);

                        Set<EventSequence> testSequences = rwGenerator.generateWalkUntilEdgeCoverage(100.0, safetyLimit);

                        int numTestCases = testSequences.size();
                        int numTestEvents = 0;
                        for (EventSequence seq : testSequences) {
                            numTestEvents += seq.length();
                        }

                        // =====================================================================
                        // IN-MEMORY MODE — SUITE FILE WRITE GUARD BEGIN
                        // =====================================================================
                        if (!inMemoryMode) {
                            String outputPath = testsequencesFolder + "L0/damping" + formatDampingFolder(dampingFactor)
                                    + "/" + productName + "_RandomWalk.txt";
                            TestSuiteFileWriter.writeEventSequenceSetAndCoverageAnalysisToFile(
                                    outputPath, testSequences, "L0_damping" + formatDampingFolder(dampingFactor),
                                    rwGenerator.getAchievedCoverage());
                        }
                        // =====================================================================
                        // IN-MEMORY MODE — SUITE FILE WRITE GUARD END
                        // =====================================================================

                        summaryWriter.println(SPLName + ";" + productName + ";" + dampingFactor + ";"
                                + numTestCases + ";" + numTestEvents + ";"
                                + String.format(java.util.Locale.ROOT, "%.2f", rwGenerator.getAchievedCoverage()) + ";"
                                + rwGenerator.getStepsTaken() + ";" + rwGenerator.isSafetyLimitHit());
                        summaryWriter.flush();
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

        System.out.println("DAMPING SENSITIVITY TEST GENERATION - Shard " + CURRENT_SHARD + " FINISHED");
    }

    private static String formatDampingFolder(double dampingFactor) {
        return String.format(java.util.Locale.ROOT, "%03d", (int) (dampingFactor * 100));
    }
}
