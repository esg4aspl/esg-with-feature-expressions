package tr.edu.iyte.esgfx.testgeneration.edgecoverage;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Map.Entry;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

public class EdgeCoverageAnalyser {

    public void esgEventSequenceSetPrinter(Set<EventSequence> composedSequences) {
        for (EventSequence es : composedSequences) {
            System.out.println(es);
        }
        System.out.println();
    }

    public double analyseEdgeCoverage(ESG ESGFx, Set<EventSequence> CESsOfESGFx,
            Map<String, FeatureExpression> featureExpressionMap) {

        Set<String> mustCoveredEdges = detectMustCoveredEdges(ESGFx);
        List<String> coveredEdges = detectCoveredEdges(CESsOfESGFx);

        Map<String, Integer> edgeCoverageMap = edgeCoverageMap(mustCoveredEdges, coveredEdges);
        Set<String> uncoveredEdges = detectUncoveredEdges(edgeCoverageMap);

        return percentageOfCoverage(mustCoveredEdges.size(), uncoveredEdges.size());
    }

    public static Set<String> detectUncoveredEdges(Map<String, Integer> edgeCoverageMap) {
        Set<String> uncoveredEdgeSet = new LinkedHashSet<>();
        for (Entry<String, Integer> entry : edgeCoverageMap.entrySet()) {
            if (entry.getValue() == 0) {
                uncoveredEdgeSet.add(entry.getKey());
            }
        }
        return uncoveredEdgeSet;
    }

    private static String getCleanEventName(String rawName) {
        String clean = rawName;
        int featureSlashIndex = clean.indexOf('/');
        if (featureSlashIndex != -1) {
            clean = clean.substring(0, featureSlashIndex);
        }
        clean = clean.replaceAll("^\\[", "").replaceAll("\\]$", "");
        clean = clean.replaceAll("\\s+", "");
        int lastUnderscore = clean.lastIndexOf('_');
        if (lastUnderscore != -1) {
            String possibleNum = clean.substring(lastUnderscore + 1);
            if (possibleNum.matches("\\d+")) {
                clean = clean.substring(0, lastUnderscore);
            }
        }
        return clean;
    }

    private static String normalizeCompositeName(String compositeName) {
        String normalized = compositeName.replace(" -> ", ",");
        normalized = normalized.replace(" AND ", ",");
        normalized = normalized.replace(":", ",");
        String[] parts = normalized.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            sb.append(getCleanEventName(parts[i].trim()));
            if (i < parts.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    // --- FIX: Set kullanarak duplicate edge'leri önle ---
    private static Set<String> detectMustCoveredEdges(ESG ESGFx) {
        Set<String> edgeSet = new LinkedHashSet<>();
        for (Edge edge : ESGFx.getRealEdgeList()) {
            Vertex source = edge.getSource();
            Vertex target = edge.getTarget();
            if (!source.isPseudoEndVertex() && !target.isPseudoStartVertex()) {
                VertexRefinedByFeatureExpression sourceVertex = (VertexRefinedByFeatureExpression) source;
                VertexRefinedByFeatureExpression targetVertex = (VertexRefinedByFeatureExpression) target;
                if (sourceVertex.getFeatureExpression().evaluate() 
                        && targetVertex.getFeatureExpression().evaluate()) {
                    String sourceName = getCleanEventName(sourceVertex.getEvent().getName().trim());
                    String targetName = getCleanEventName(targetVertex.getEvent().getName().trim());
                    edgeSet.add(sourceName + "," + targetName);
                }
            }
        }
        return edgeSet;
    }

    private List<String> detectCoveredEdges(Set<EventSequence> CESsOfESGFx) {
        List<String> lineList = new LinkedList<>();
        if (CESsOfESGFx == null) return lineList;
        for (EventSequence es : CESsOfESGFx) {
            StringBuilder rawSequenceBuilder = new StringBuilder();
            for (int i = 0; i < es.length(); i++) {
                Vertex event = es.getEventSequence().get(i);
                String rawStr = normalizeCompositeName(event.getEvent().getName().trim());
                rawSequenceBuilder.append(rawStr);
                if (i < es.length() - 1) {
                    rawSequenceBuilder.append(",");
                }
            }
            String finalLine = rawSequenceBuilder.toString().replaceAll(",+", ",");
            lineList.add(finalLine);
        }
        return lineList;
    }

    // --- FIX: Sentinel key kaldırıldı, Set input kullanılıyor ---
    private static Map<String, Integer> edgeCoverageMap(Set<String> edgeSet, List<String> lineList) {
        Map<String, Integer> edgeCoverageMap = new LinkedHashMap<>();
        for (String edge : edgeSet) {
            int counter = 0;
            for (String line : lineList) {
                counter += countBounded(line, edge);
            }
            edgeCoverageMap.put(edge, counter);
        }
        return edgeCoverageMap;
    }

    private static int countBounded(String text, String find) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(find, index)) != -1) {
            int end = index + find.length();
            boolean startOk = (index == 0 || text.charAt(index - 1) == ',');
            boolean endOk = (end == text.length() || text.charAt(end) == ',');
            if (startOk && endOk) {
                count++;
            }
            index += find.length();
        }
        return count;
    }


    public static double percentageOfCoverage(int mustCoveredCount, int uncoveredCount) {
        if (mustCoveredCount == 0) return 100.0;
        if (uncoveredCount == 0) return 100.0;
        return 100.0 - ((double) uncoveredCount / (double) mustCoveredCount * 100.0);
    }
}