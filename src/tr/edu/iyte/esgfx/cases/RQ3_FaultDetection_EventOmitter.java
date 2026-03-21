package tr.edu.iyte.esgfx.cases;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.conversion.dot.DOTFileToESGFxConverter;
import tr.edu.iyte.esgfx.mutationtesting.faultdetection.FaultDetector;
import tr.edu.iyte.esgfx.mutationtesting.mutationoperators.EventOmitter;
import tr.edu.iyte.esgfx.mutationtesting.resultutils.FaultDetectionResultRecorder;
import tr.edu.iyte.esgfx.testgeneration.FileToTestSuiteConverter;

/**
 * Event Omission fault detection with multi-seed Random Walk support.
 * 
 * Deterministic approaches (ESG-Fx L1-4, EFG L2-4): run once, same as original.
 * Random Walk (ESG-Fx_L0): iterated over 10 seeds to capture stochastic variation.
 */
public class RQ3_FaultDetection_EventOmitter extends CaseStudyUtilities {

    private static final long[] SEEDS = {42L, 43L, 44L, 45L, 46L, 47L, 48L, 49L, 50L, 51L};

    public void evaluateFaultDetection() throws Exception {
        System.out.println("RQ3 FAULT DETECTION (EVENT OMISSION, MULTI-SEED RW) - SPL: " + SPLName + " STARTED");

        int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
        int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));

        featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFile, ESGFxFile);

        String dotDirectoryPath = DOTFolder + "L2/";
        File dotDir = new File(dotDirectoryPath);

        if (!dotDir.exists() || !dotDir.isDirectory()) {
            throw new RuntimeException("CRITICAL ERROR: DOT directory not found: " + dotDirectoryPath);
        }

        File[] dotFiles = dotDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".dot"));
        if (dotFiles == null || dotFiles.length == 0) {
            return;
        }

        Arrays.sort(dotFiles, (f1, f2) -> f1.getName().compareTo(f2.getName()));

        int productID = 0;
        int handledProducts = 0;

        EventOmitter eventOmitter = new EventOmitter();

        String[] deterministicApproaches = {
            "ESG-Fx_L1", "ESG-Fx_L2", "ESG-Fx_L3", "ESG-Fx_L4",
            "EFG_L2", "EFG_L3", "EFG_L4"
        };

        String perProductLogDir = faultDetectionFolder + "perProduct/";
        new File(perProductLogDir).mkdirs();

        String deterministicLogPath = perProductLogDir + SPLName
                + "_EventOmission_shard" + String.format("%02d", CURRENT_SHARD) + ".csv";

        String multiSeedLogPath = perProductLogDir + SPLName
                + "_EventOmission_MultiSeedRW_shard" + String.format("%02d", CURRENT_SHARD) + ".csv";

        for (File dotFile : dotFiles) {
            productID++;
            if (((productID - 1) % N_SHARDS) != CURRENT_SHARD) {
                continue;
            }

            handledProducts++;
            String productName = dotFile.getName().replaceAll("(?i)\\.dot", "");

            ESG productESGFx = DOTFileToESGFxConverter.parseDOTFileForESGFxCreation(
                    dotFile.getAbsolutePath(), featureExpressionMapFromFeatureModel);
            List<Vertex> productESGFxVertices = productESGFx.getRealVertexList();
            int totalMutants = productESGFxVertices.size();

            // --- Deterministic approaches ---
            for (String approach : deterministicApproaches) {

                Set<EventSequence> loadedTestSuites = FileToTestSuiteConverter
                        .loadTestSequencesFromFile(productName, approach, productESGFx);

                if (loadedTestSuites == null || loadedTestSuites.isEmpty()) {
                    continue;
                }

                FaultDetector detector = new FaultDetector(loadedTestSuites);
                int totalEventsInSuite = detector.getTotalEventsInSuite();

                int detectedMutants = 0;
                List<Integer> stepsToDetectList = new ArrayList<>();
                List<Double> percentagesToDetectList = new ArrayList<>();

                int mutantID = 0;
                for (Vertex eventToOmit : productESGFxVertices) {
                    mutantID++;
                    ESG mutant = eventOmitter.createSingleMutant(productESGFx, eventToOmit, mutantID);

                    if (detector.isFaultDetected(mutant)) {
                        detectedMutants++;
                        int stepsWalked = detector.getEventsWalked();
                        double percentageWalked = totalEventsInSuite > 0
                                ? ((double) stepsWalked / totalEventsInSuite) * 100.0
                                : 0.0;
                        stepsToDetectList.add(stepsWalked);
                        percentagesToDetectList.add(percentageWalked);
                    }
                    mutant = null;
                }

                double mutationScore = totalMutants > 0 ? ((double) detectedMutants / totalMutants) * 100.0 : 0.0;
                double medianEventsToDetect = calculateMedian(stepsToDetectList);
                double medianPercentageOfSuite = calculateMedianDouble(percentagesToDetectList);

                FaultDetectionResultRecorder.writeRQ3PerProductLog(deterministicLogPath, SPLName, productName,
                        "EventOmission", approach, totalMutants, detectedMutants, mutationScore,
                        totalEventsInSuite, medianEventsToDetect, medianPercentageOfSuite);
            }

            // --- Multi-seed Random Walk ---
            for (long seed : SEEDS) {

                Set<EventSequence> loadedTestSuites = FileToTestSuiteConverter
                        .loadTestSequencesFromFile(productName, "ESG-Fx_L0", productESGFx, seed);

                if (loadedTestSuites == null || loadedTestSuites.isEmpty()) {
                    continue;
                }

                FaultDetector detector = new FaultDetector(loadedTestSuites);
                int totalEventsInSuite = detector.getTotalEventsInSuite();

                int detectedMutants = 0;
                List<Integer> stepsToDetectList = new ArrayList<>();
                List<Double> percentagesToDetectList = new ArrayList<>();

                int mutantID = 0;
                for (Vertex eventToOmit : productESGFxVertices) {
                    mutantID++;
                    ESG mutant = eventOmitter.createSingleMutant(productESGFx, eventToOmit, mutantID);

                    if (detector.isFaultDetected(mutant)) {
                        detectedMutants++;
                        int stepsWalked = detector.getEventsWalked();
                        double percentageWalked = totalEventsInSuite > 0
                                ? ((double) stepsWalked / totalEventsInSuite) * 100.0
                                : 0.0;
                        stepsToDetectList.add(stepsWalked);
                        percentagesToDetectList.add(percentageWalked);
                    }
                    mutant = null;
                }

                double mutationScore = totalMutants > 0 ? ((double) detectedMutants / totalMutants) * 100.0 : 0.0;
                double medianEventsToDetect = calculateMedian(stepsToDetectList);
                double medianPercentageOfSuite = calculateMedianDouble(percentagesToDetectList);

                FaultDetectionResultRecorder.writeRQ3MultiSeedPerProductLog(multiSeedLogPath, SPLName, productName,
                        "EventOmission", seed, totalMutants, detectedMutants, mutationScore,
                        totalEventsInSuite, medianEventsToDetect, medianPercentageOfSuite);
            }

            productESGFx = null;
            if (handledProducts % 50 == 0) {
                System.gc();
            }
        }

        System.out.println("RQ3 FAULT DETECTION (EVENT OMISSION, MULTI-SEED RW) - Shard " + CURRENT_SHARD + " FINISHED");
    }

    private double calculateMedian(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        Collections.sort(values);
        int size = values.size();
        if (size % 2 == 1) {
            return values.get(size / 2);
        } else {
            return (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0;
        }
    }

    private double calculateMedianDouble(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        Collections.sort(values);
        int size = values.size();
        if (size % 2 == 1) {
            return values.get(size / 2);
        } else {
            return (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0;
        }
    }
}