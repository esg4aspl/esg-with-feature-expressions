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
 * Sensitivity analysis: generates Random Walk test suites for multiple damping factors.
 * 
 * Purpose: Show that the choice of damping factor (0.85) does not significantly
 * affect achieved coverage, test suite size, or downstream fault detection.
 * 
 * For each product, generates tests with damping factors {0.80, 0.85, 0.90}
 * using a fixed seed (42L). Saves to:
 *   testsequences/L0/damping080/{productName}_RandomWalk.txt
 *   testsequences/L0/damping085/{productName}_RandomWalk.txt
 *   testsequences/L0/damping090/{productName}_RandomWalk.txt
 * 
 * Also records a summary CSV with coverage and test suite metrics per damping factor,
 * which can be used directly as the sensitivity analysis table in the paper.
 */
public class RQ3_DampingSensitivity_TestGenerator extends CaseStudyUtilities {

    private static final long SEED = 42L;
    private static final double[] DAMPING_FACTORS = {0.80, 0.85, 0.90};

    public void generateDampingSensitivityTests() throws Exception {
        System.out.println("DAMPING SENSITIVITY TEST GENERATION - SPL: " + SPLName + " STARTED");

        int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
        int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));

        featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFile, ESGFxFile);

        String dotDirectoryPath = DOTFolder + "L2/";
        File dotDir = new File(dotDirectoryPath);

        if (!dotDir.exists() || !dotDir.isDirectory()) {
            throw new RuntimeException("DOT directory not found: " + dotDirectoryPath);
        }

        File[] dotFiles = dotDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".dot"));
        if (dotFiles == null || dotFiles.length == 0) {
            System.out.println("No DOT files found.");
            return;
        }

        Arrays.sort(dotFiles, (f1, f2) -> f1.getName().compareTo(f2.getName()));

        // Create damping-specific directories
        for (double df : DAMPING_FACTORS) {
            String dampingDir = testsequencesFolder + "L0/damping" + formatDampingFolder(df) + "/";
            new File(dampingDir).mkdirs();
        }

        // Summary CSV: one row per product per damping factor
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
                        System.err.println("Could not parse ESGFx for: " + productName);
                        continue;
                    }

                    int vertexCount = productESGFx.getVertexList().size();
                    int safetyLimit = Math.min(
                    	    5 * vertexCount * vertexCount * vertexCount,
                    	    2_000_000
                    	);
                    for (double dampingFactor : DAMPING_FACTORS) {
                        RandomWalkTestGenerator rwGenerator = new RandomWalkTestGenerator(
                                (ESGFx) productESGFx, dampingFactor, SEED);

                        Set<EventSequence> testSequences = rwGenerator.generateWalkUntilEdgeCoverage(100.0, safetyLimit);

                        int numTestCases = testSequences.size();
                        int numTestEvents = 0;
                        for (EventSequence seq : testSequences) {
                            numTestEvents += seq.length();
                        }

                        // Save to damping-specific folder
                        String outputPath = testsequencesFolder + "L0/damping" + formatDampingFolder(dampingFactor)
                                + "/" + productName + "_RandomWalk.txt";

                        TestSuiteFileWriter.writeEventSequenceSetAndCoverageAnalysisToFile(
                                outputPath, testSequences, "L0_damping" + formatDampingFolder(dampingFactor),
                                rwGenerator.getAchievedCoverage());

                        // Record summary
                        summaryWriter.println(SPLName + ";" + productName + ";" + dampingFactor + ";"
                                + numTestCases + ";" + numTestEvents + ";"
                                + String.format("%.2f", rwGenerator.getAchievedCoverage()) + ";"
                                + rwGenerator.getStepsTaken() + ";" + rwGenerator.isSafetyLimitHit());
                        summaryWriter.flush();
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

        System.out.println("DAMPING SENSITIVITY TEST GENERATION - Shard " + CURRENT_SHARD + " FINISHED");
    }

    /**
     * Converts damping factor to folder name: 0.80 -> "080", 0.85 -> "085", 0.90 -> "090"
     */
    private static String formatDampingFolder(double dampingFactor) {
        return String.format("%03d", (int) (dampingFactor * 100));
    }
}