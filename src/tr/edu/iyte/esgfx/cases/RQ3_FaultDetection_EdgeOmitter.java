package tr.edu.iyte.esgfx.cases;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esgfx.conversion.dot.DOTFileToESGFxConverter;
import tr.edu.iyte.esgfx.mutationtesting.faultdetection.FaultDetector;
import tr.edu.iyte.esgfx.mutationtesting.mutationoperators.EdgeOmitter;
import tr.edu.iyte.esgfx.mutationtesting.resultutils.FaultDetectionResultRecorder;
import tr.edu.iyte.esgfx.testgeneration.FileToTestSuiteConverter;

public class RQ3_FaultDetection_EdgeOmitter extends CaseStudyUtilities {

    private static final long[] SEEDS = {42L, 43L, 44L, 45L, 46L, 47L, 48L, 49L, 50L, 51L};

    // =====================================================================
    // IN-MEMORY MODE — CONSTANTS BEGIN
    // =====================================================================
    private static final double IN_MEMORY_RW_DAMPING = 0.85;
    private static final long IN_MEMORY_RW_DET_SEED = 42L;
    // =====================================================================
    // IN-MEMORY MODE — CONSTANTS END
    // =====================================================================

    public void evaluateFaultDetection() throws Exception {
        System.out.println("RQ3 FAULT DETECTION (EDGE OMISSION, MULTI-SEED RW) - SPL: " + SPLName + " STARTED");

        int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
        int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));

        // =====================================================================
        // IN-MEMORY MODE — DISPATCH FLAG BEGIN
        // =====================================================================
        boolean inMemoryMode = TestSuiteFactory.isInMemoryMode();
        System.out.println("RQ3_MODE = " + (inMemoryMode ? "memory" : "file"));
        // =====================================================================
        // IN-MEMORY MODE — DISPATCH FLAG END
        // =====================================================================

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

        EdgeOmitter edgeOmitter = new EdgeOmitter();

        String[] deterministicApproaches = {
            "ESG-Fx_L0", "ESG-Fx_L1", "ESG-Fx_L2", "ESG-Fx_L3", "ESG-Fx_L4",
            "EFG_L2", "EFG_L3", "EFG_L4"
        };

        String perProductLogDir = faultDetectionFolder + "perProduct/";
        new File(perProductLogDir).mkdirs();

        String deterministicLogPath = perProductLogDir + SPLName
                + "_EdgeOmission_shard" + String.format("%02d", CURRENT_SHARD) + ".csv";

        String multiSeedLogPath = perProductLogDir + SPLName
                + "_EdgeOmission_MultiSeedRW_shard" + String.format("%02d", CURRENT_SHARD) + ".csv";

        for (File dotFile : dotFiles) {
            productID++;
            if (((productID - 1) % N_SHARDS) != CURRENT_SHARD) {
                continue;
            }

            handledProducts++;
            String productName = dotFile.getName().replaceAll("(?i)\\.dot", "");

            // =====================================================================
            // IN-MEMORY MODE — PER-PRODUCT CONFIG REFRESH BEGIN
            // =====================================================================
            // In-memory generators evaluate feature expressions per product.
            // Apply config unconditionally so file-mode and memory-mode see
            // consistent feature truth values.
            String configFilePath = productConfigurationFolder + productName + ".config";
            updateFeatureExpressionMapFromConfigFile(configFilePath);
            // =====================================================================
            // IN-MEMORY MODE — PER-PRODUCT CONFIG REFRESH END
            // =====================================================================

            ESG productESGFx = DOTFileToESGFxConverter.parseDOTFileForESGFxCreation(
                    dotFile.getAbsolutePath(), featureExpressionMapFromFeatureModel);
            List<Edge> productESGFxEdges = productESGFx.getRealEdgeList();
            int totalMutants = productESGFxEdges.size();

            for (String approach : deterministicApproaches) {

                Set<EventSequence> loadedTestSuites;

                // =====================================================================
                // IN-MEMORY MODE — DETERMINISTIC SUITE SOURCING BEGIN
                // =====================================================================
                if (inMemoryMode) {
                    if ("ESG-Fx_L0".equals(approach)) {
                        loadedTestSuites = TestSuiteFactory.generateRandomWalkSuite(
                                productESGFx, IN_MEMORY_RW_DAMPING, IN_MEMORY_RW_DET_SEED);
                    } else {
                        int level = TestSuiteFactory.approachToLevel(approach);
                        if (level >= 1) {
                            loadedTestSuites = TestSuiteFactory.generateESGFxSuite(
                                    productESGFx, level, featureExpressionMapFromFeatureModel);
                        } else {
                            // EFG_* approaches: GUITAR-produced, no in-memory generator.
                            loadedTestSuites = FileToTestSuiteConverter
                                    .loadTestSequencesFromFile(productName, approach, productESGFx);
                        }
                    }
                } else {
                    loadedTestSuites = FileToTestSuiteConverter
                            .loadTestSequencesFromFile(productName, approach, productESGFx);
                }
                // =====================================================================
                // IN-MEMORY MODE — DETERMINISTIC SUITE SOURCING END
                // =====================================================================

                if (loadedTestSuites == null || loadedTestSuites.isEmpty()) {
                    continue;
                }

                FaultDetector detector = new FaultDetector(loadedTestSuites, productESGFx);
                int totalEventsInSuite = detector.getTotalEventsInSuite();

                int detectedMutants = 0;
                int killsByEdgeMissing = 0;
                int killsByVertexMissing = 0;
                List<Integer> stepsToDetectList = new ArrayList<>();
                List<Double> percentagesToDetectList = new ArrayList<>();
                List<Double> penalizedPercentagesList = new ArrayList<>();
                Map<String, int[]> featureHistogram = new LinkedHashMap<>();

                int mutantID = 0;
                for (Edge edgeToOmit : productESGFxEdges) {
                    mutantID++;
                    String srcFE = FaultDetectionResultRecorder.extractFeatureExpressionKey(edgeToOmit.getSource());
                    String tgtFE = FaultDetectionResultRecorder.extractFeatureExpressionKey(edgeToOmit.getTarget());
                    String feKey = srcFE + " >> " + tgtFE;
                    int[] kt = featureHistogram.get(feKey);
                    if (kt == null) { kt = new int[]{0, 0}; featureHistogram.put(feKey, kt); }
                    kt[1]++;

                    ESG mutant = edgeOmitter.createSingleMutant(productESGFx, edgeToOmit, mutantID);

                    boolean detected = detector.isFaultDetected(mutant);

                    if (detected) {
                        detectedMutants++;
                        kt[0]++;
                        int stepsWalked = detector.getEventsWalked();
                        double percentageWalked = totalEventsInSuite > 0
                                ? ((double) stepsWalked / totalEventsInSuite) * 100.0
                                : 0.0;
                        stepsToDetectList.add(stepsWalked);
                        percentagesToDetectList.add(percentageWalked);
                        penalizedPercentagesList.add(percentageWalked);

                        String reason = detector.getLastDetectionReason();
                        if (FaultDetector.REASON_EDGE_MISSING.equals(reason)) {
                            killsByEdgeMissing++;
                        } else if (FaultDetector.REASON_VERTEX_MISSING.equals(reason)) {
                            killsByVertexMissing++;
                        }
                    } else {
                        penalizedPercentagesList.add(100.0);
                    }
                    mutant = null;
                }

                double mutationScore = totalMutants > 0 ? ((double) detectedMutants / totalMutants) * 100.0 : 0.0;
                double medianEventsToDetect = calculateMedian(stepsToDetectList);
                double medianPercentageOfSuite = calculateMedianDouble(percentagesToDetectList);
                double penalizedMedianPercentage = calculateMedianDouble(penalizedPercentagesList);

                int distinctFE = featureHistogram.size();
                String histogramStr = FaultDetectionResultRecorder.encodeFeatureHistogram(featureHistogram);
                int affectedEdgesTotal = totalMutants;

                FaultDetectionResultRecorder.writeRQ3PerProductLog(deterministicLogPath, SPLName, productName,
                        "EdgeOmission", approach, totalMutants, detectedMutants, mutationScore,
                        totalEventsInSuite, medianEventsToDetect, medianPercentageOfSuite,
                        penalizedMedianPercentage, killsByEdgeMissing, killsByVertexMissing,
                        distinctFE, histogramStr, affectedEdgesTotal);
            }

            for (long seed : SEEDS) {

                // =====================================================================
                // IN-MEMORY MODE — MULTI-SEED RW SUITE SOURCING BEGIN
                // =====================================================================
                Set<EventSequence> loadedTestSuites;
                if (inMemoryMode) {
                    loadedTestSuites = TestSuiteFactory.generateRandomWalkSuite(
                            productESGFx, IN_MEMORY_RW_DAMPING, seed);
                } else {
                    loadedTestSuites = FileToTestSuiteConverter
                            .loadTestSequencesFromFile(productName, "ESG-Fx_L0", productESGFx, seed);
                }
                // =====================================================================
                // IN-MEMORY MODE — MULTI-SEED RW SUITE SOURCING END
                // =====================================================================

                if (loadedTestSuites == null || loadedTestSuites.isEmpty()) {
                    continue;
                }

                FaultDetector detector = new FaultDetector(loadedTestSuites, productESGFx);
                int totalEventsInSuite = detector.getTotalEventsInSuite();

                int detectedMutants = 0;
                int killsByEdgeMissing = 0;
                int killsByVertexMissing = 0;
                List<Integer> stepsToDetectList = new ArrayList<>();
                List<Double> percentagesToDetectList = new ArrayList<>();
                List<Double> penalizedPercentagesList = new ArrayList<>();
                Map<String, int[]> featureHistogram = new LinkedHashMap<>();

                int mutantID = 0;
                for (Edge edgeToOmit : productESGFxEdges) {
                    mutantID++;
                    String srcFE = FaultDetectionResultRecorder.extractFeatureExpressionKey(edgeToOmit.getSource());
                    String tgtFE = FaultDetectionResultRecorder.extractFeatureExpressionKey(edgeToOmit.getTarget());
                    String feKey = srcFE + " >> " + tgtFE;
                    int[] kt = featureHistogram.get(feKey);
                    if (kt == null) { kt = new int[]{0, 0}; featureHistogram.put(feKey, kt); }
                    kt[1]++;

                    ESG mutant = edgeOmitter.createSingleMutant(productESGFx, edgeToOmit, mutantID);

                    boolean detected = detector.isFaultDetected(mutant);

                    if (detected) {
                        detectedMutants++;
                        kt[0]++;
                        int stepsWalked = detector.getEventsWalked();
                        double percentageWalked = totalEventsInSuite > 0
                                ? ((double) stepsWalked / totalEventsInSuite) * 100.0
                                : 0.0;
                        stepsToDetectList.add(stepsWalked);
                        percentagesToDetectList.add(percentageWalked);
                        penalizedPercentagesList.add(percentageWalked);

                        String reason = detector.getLastDetectionReason();
                        if (FaultDetector.REASON_EDGE_MISSING.equals(reason)) {
                            killsByEdgeMissing++;
                        } else if (FaultDetector.REASON_VERTEX_MISSING.equals(reason)) {
                            killsByVertexMissing++;
                        }
                    } else {
                        penalizedPercentagesList.add(100.0);
                    }
                    mutant = null;
                }

                double mutationScore = totalMutants > 0 ? ((double) detectedMutants / totalMutants) * 100.0 : 0.0;
                double medianEventsToDetect = calculateMedian(stepsToDetectList);
                double medianPercentageOfSuite = calculateMedianDouble(percentagesToDetectList);
                double penalizedMedianPercentage = calculateMedianDouble(penalizedPercentagesList);

                int distinctFE = featureHistogram.size();
                String histogramStr = FaultDetectionResultRecorder.encodeFeatureHistogram(featureHistogram);
                int affectedEdgesTotal = totalMutants;

                FaultDetectionResultRecorder.writeRQ3MultiSeedPerProductLog(multiSeedLogPath, SPLName, productName,
                        "EdgeOmission", seed, totalMutants, detectedMutants, mutationScore,
                        totalEventsInSuite, medianEventsToDetect, medianPercentageOfSuite,
                        penalizedMedianPercentage, killsByEdgeMissing, killsByVertexMissing,
                        distinctFE, histogramStr, affectedEdgesTotal);
            }

            productESGFx = null;
            if (handledProducts % 50 == 0) {
                System.gc();
            }
        }

        System.out.println("RQ3 FAULT DETECTION (EDGE OMISSION, MULTI-SEED RW) - Shard " + CURRENT_SHARD + " FINISHED");
    }

    private double calculateMedian(List<Integer> values) {
        if (values == null || values.isEmpty()) return 0.0;
        Collections.sort(values);
        int size = values.size();
        if (size % 2 == 1) return values.get(size / 2);
        return (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0;
    }

    private double calculateMedianDouble(List<Double> values) {
        if (values == null || values.isEmpty()) return 0.0;
        Collections.sort(values);
        int size = values.size();
        if (size % 2 == 1) return values.get(size / 2);
        return (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0;
    }
}
