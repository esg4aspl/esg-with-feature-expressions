package tr.edu.iyte.esgfx.testgeneration.eventcoverage;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

public class EventCoverageAnalyser {

    public void esgEventSequenceSetPrinter(Set<EventSequence> composedSequences) {
        for (EventSequence es : composedSequences) {
            System.out.println(es);
        }
        System.out.println();
    }

    public double analyseEventCoverage(ESG ESGFx, Set<EventSequence> CESsOfESGFx,
            Map<String, FeatureExpression> featureExpressionMap) {

        Set<String> mustCoveredEvents = detectMustCoveredEvents(ESGFx);
        Set<String> coveredEvents = detectCoveredEvents(CESsOfESGFx);

        Set<String> uncoveredEvents = new LinkedHashSet<>(mustCoveredEvents);
        uncoveredEvents.removeAll(coveredEvents);

        return percentageOfCoverage(mustCoveredEvents, uncoveredEvents);
    }

    // --- Normalization (same logic as EdgeCoverageAnalyser) ---

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

    // --- Detection methods (now return Sets with normalized names) ---

    private Set<String> detectMustCoveredEvents(ESG ESGFx) {
        Set<String> mustCoveredEvents = new LinkedHashSet<>();
        Iterator<Vertex> vertexIterator = ESGFx.getVertexList().iterator();

        while (vertexIterator.hasNext()) {
            Vertex vertex = vertexIterator.next();
            if (!vertex.isPseudoStartVertex() && !vertex.isPseudoEndVertex()) {
                VertexRefinedByFeatureExpression vRefined = (VertexRefinedByFeatureExpression) vertex;
                FeatureExpression featureExpression = vRefined.getFeatureExpression();
                if (featureExpression.evaluate()) {
                    String cleanName = getCleanEventName(vRefined.getEvent().getName().trim());
                    mustCoveredEvents.add(cleanName);
                }
            }
        }
        return mustCoveredEvents;
    }

    private Set<String> detectCoveredEvents(Set<EventSequence> CESsOfESGFx) {
        Set<String> coveredEventSet = new LinkedHashSet<>();
        if (CESsOfESGFx == null) return coveredEventSet;

        for (EventSequence eventSequence : CESsOfESGFx) {
            for (Vertex vertex : eventSequence.getEventSequence()) {
                String normalized = normalizeCompositeName(vertex.getEvent().getName().trim());
                // A composite name may expand to multiple individual event names
                for (String part : normalized.split(",")) {
                    if (!part.isEmpty()) {
                        coveredEventSet.add(part);
                    }
                }
            }
        }
        return coveredEventSet;
    }

    // --- Coverage calculation (fixed denominator) ---

    private static double percentageOfCoverage(Set<String> mustCoveredEvents, Set<String> uncoveredEvents) {
        if (mustCoveredEvents.isEmpty()) return 100.0;
        if (uncoveredEvents.isEmpty()) return 100.0;

        double coverage = ((double) uncoveredEvents.size()) / ((double) mustCoveredEvents.size()) * 100.0;
        return 100.0 - coverage;
    }
}