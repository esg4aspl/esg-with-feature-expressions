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

/**
 * Edge Omission fault detection with multi-seed Random Walk support.
 *
 * Deterministic approaches (ESG-Fx L0-4, EFG L2-4): run once per approach.
 * Random Walk (ESG-Fx_L0): ALSO iterated over 10 seeds to capture stochastic variation.
 *
 * IMPORTANT: ESG-Fx_L0 appears in BOTH deterministic and multi-seed loops:
 * - Deterministic: Uses seed (42+productID) from /L0/PXXXX_RandomWalk.txt
 * - Multi-seed: Uses seeds 42-51 from /L0/seedYY/PXXXX_RandomWalk.txt
 *
 * Extensions (A2, A3, A4, AffectedEdges):
 *  - A2: PenalizedPercentageOfSuiteToDetect(%) treats missed mutants as if the
 *        full test suite was walked (100%), producing an unbiased median over
 *        all mutants rather than only the detected ones.
 *  - A3: KillsByEdgeMissing / KillsByVertexMissing split DetectedMutants by the
 *        kill branch that fired in FaultDetector.
 *  - A4: Per-feature histogram maps each feature expression key to its (kills,
 *        total) pair. For Edge Omission the key is "srcFE >> tgtFE".
 *  - AffectedEdgesTotal: for Edge Omission this is always equal to totalMutants
 *        (every mutant removes exactly one edge); recorded for symmetry with
 *        Event Omission.
 */
public class RQ3_FaultDetection_EdgeOmitter extends CaseStudyUtilities {

    private static final long[] SEEDS = {42L, 43L, 44L, 45L, 46L, 47L, 48L, 49L, 50L, 51L};

    public void evaluateFaultDetection() throws Exception {
        System.out.println("RQ3 FAULT DETECTION (EDGE OMISSION, MULTI-SEED RW) - SPL: " + SPLName + " STARTED");

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

            ESG productESGFx = DOTFileToESGFxConverter.parseDOTFileForESGFxCreation(
                    dotFile.getAbsolutePath(), featureExpressionMapFromFeatureModel);
            List<Edge> productESGFxEdges = productESGFx.getRealEdgeList();
            int totalMutants = productESGFxEdges.size();

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
                int killsByEdgeMissing = 0;
                int killsByVertexMissing = 0;
                List<Integer> stepsToDetectList = new ArrayList<>();
                List<Double> percentagesToDetectList = new ArrayList<>();
                List<Double> penalizedPercentagesList = new ArrayList<>();
                // A4: feature-expression histogram keyed by "srcFE >> tgtFE"
                // Value: int[]{kills, total}
                Map<String, int[]> featureHistogram = new LinkedHashMap<>();

                int mutantID = 0;
                for (Edge edgeToOmit : productESGFxEdges) {
                    mutantID++;
                    // A4: derive feature-expression key for this edge
                    String srcFE = FaultDetectionResultRecorder.extractFeatureExpressionKey(edgeToOmit.getSource());
                    String tgtFE = FaultDetectionResultRecorder.extractFeatureExpressionKey(edgeToOmit.getTarget());
                    String feKey = srcFE + " >> " + tgtFE;
                    int[] kt = featureHistogram.get(feKey);
                    if (kt == null) { kt = new int[]{0, 0}; featureHistogram.put(feKey, kt); }
                    kt[1]++; // increment total

                    ESG mutant = edgeOmitter.createSingleMutant(productESGFx, edgeToOmit, mutantID);

                    boolean detected = detector.isFaultDetected(mutant);

                    if (detected) {
                        detectedMutants++;
                        kt[0]++; // increment kills
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
                // Edge Omission: each mutant removes exactly one edge
                int affectedEdgesTotal = totalMutants;

                FaultDetectionResultRecorder.writeRQ3PerProductLog(deterministicLogPath, SPLName, productName,
                        "EdgeOmission", approach, totalMutants, detectedMutants, mutationScore,
                        totalEventsInSuite, medianEventsToDetect, medianPercentageOfSuite,
                        penalizedMedianPercentage, killsByEdgeMissing, killsByVertexMissing,
                        distinctFE, histogramStr, affectedEdgesTotal);
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