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
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.conversion.dot.DOTFileToESGFxConverter;
import tr.edu.iyte.esgfx.mutationtesting.faultdetection.FaultDetector;
import tr.edu.iyte.esgfx.mutationtesting.mutationoperators.EventOmitter;
import tr.edu.iyte.esgfx.mutationtesting.resultutils.FaultDetectionResultRecorder;
import tr.edu.iyte.esgfx.testgeneration.FileToTestSuiteConverter;

/**
 * Damping factor sensitivity analysis: Event Omission fault detection.
 * Same A2 + A3 + A4 + AffectedEdges instrumentation as RQ3_FaultDetection_EventOmitter.
 */
public class RQ3_DampingSensitivity_FaultDetection_EventOmitter extends CaseStudyUtilities {

    private static final double[] DAMPING_FACTORS = {0.80, 0.85, 0.90};

    public void evaluateFaultDetection() throws Exception {
        System.out.println("DAMPING SENSITIVITY (EVENT OMISSION) - SPL: " + SPLName + " STARTED");

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

        String sensitivityDir = faultDetectionFolder + "sensitivity/";
        new File(sensitivityDir).mkdirs();

        String logPath = sensitivityDir + SPLName
                + "_DampingSensitivity_EventOmission_shard" + String.format("%02d", CURRENT_SHARD) + ".csv";

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

            // Precompute incident-edge count per vertex
            List<Edge> allEdges = productESGFx.getEdgeList();
            Map<Integer, Integer> incidentEdgeCount = new LinkedHashMap<>();
            for (Edge e : allEdges) {
                int srcId = e.getSource().getID();
                int tgtId = e.getTarget().getID();
                incidentEdgeCount.merge(srcId, 1, Integer::sum);
                if (srcId != tgtId) {
                    incidentEdgeCount.merge(tgtId, 1, Integer::sum);
                }
            }

            for (double dampingFactor : DAMPING_FACTORS) {

                Set<EventSequence> loadedTestSuites = FileToTestSuiteConverter
                        .loadTestSequencesForDamping(productName, productESGFx, dampingFactor);

                if (loadedTestSuites == null || loadedTestSuites.isEmpty()) {
                    continue;
                }

                FaultDetector detector = new FaultDetector(loadedTestSuites,productESGFx);
                int totalEventsInSuite = detector.getTotalEventsInSuite();

                int detectedMutants = 0;
                int killsByEdgeMissing = 0;
                int killsByVertexMissing = 0;
                int affectedEdgesTotal = 0;
                List<Integer> stepsToDetectList = new ArrayList<>();
                List<Double> percentagesToDetectList = new ArrayList<>();
                List<Double> penalizedPercentagesList = new ArrayList<>();
                Map<String, int[]> featureHistogram = new LinkedHashMap<>();

                int mutantID = 0;
                for (Vertex eventToOmit : productESGFxVertices) {
                    mutantID++;
                    String feKey = FaultDetectionResultRecorder.extractFeatureExpressionKey(eventToOmit);
                    int[] kt = featureHistogram.get(feKey);
                    if (kt == null) { kt = new int[]{0, 0}; featureHistogram.put(feKey, kt); }
                    kt[1]++;

                    affectedEdgesTotal += incidentEdgeCount.getOrDefault(eventToOmit.getID(), 0);

                    ESG mutant = eventOmitter.createSingleMutant(productESGFx, eventToOmit, mutantID);

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

                FaultDetectionResultRecorder.writeRQ3DampingSensitivityLog(logPath, SPLName, productName,
                        "EventOmission", dampingFactor, totalMutants, detectedMutants, mutationScore,
                        totalEventsInSuite, medianEventsToDetect, medianPercentageOfSuite,
                        penalizedMedianPercentage, killsByEdgeMissing, killsByVertexMissing,
                        distinctFE, histogramStr, affectedEdgesTotal);
            }

            productESGFx = null;
            if (handledProducts % 50 == 0) {
                System.gc();
            }
        }

        System.out.println("DAMPING SENSITIVITY (EVENT OMISSION) - Shard " + CURRENT_SHARD + " FINISHED");
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