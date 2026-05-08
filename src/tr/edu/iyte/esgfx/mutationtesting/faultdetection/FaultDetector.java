package tr.edu.iyte.esgfx.mutationtesting.faultdetection;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.ESGFx;

public class FaultDetector {

    public static final String REASON_NOT_DETECTED = "NOT_DETECTED";
    public static final String REASON_VERTEX_MISSING = "VERTEX_MISSING";
    public static final String REASON_EDGE_MISSING = "EDGE_MISSING";

    private Set<EventSequence> CESsOfESG;
    private ESG originalESGFx;
    private int currentCES;
    private int eventsWalked;
    private int totalEventsInSuite;

    private String lastDetectionReason = REASON_NOT_DETECTED;

    public FaultDetector(Set<EventSequence> CESsOfESG, ESG originalESGFx) {
        setCESsOfESG(CESsOfESG);
        this.originalESGFx = originalESGFx;
    }

    public Set<EventSequence> getCESsOfESG() {
        return CESsOfESG;
    }

    public void setCESsOfESG(Set<EventSequence> cESsOfESG) {
        CESsOfESG = cESsOfESG;
        calculateTotalEventsInSuite();
    }

    public void setOriginalESGFx(ESG originalESGFx) {
        this.originalESGFx = originalESGFx;
    }

    public ESG getOriginalESGFx() {
        return originalESGFx;
    }

    public int getCurrentCES() {
        return currentCES;
    }

    public int getEventsWalked() {
        return eventsWalked;
    }

    public int getTotalEventsInSuite() {
        return totalEventsInSuite;
    }

    public String getLastDetectionReason() {
        return lastDetectionReason;
    }

    private void calculateTotalEventsInSuite() {
        totalEventsInSuite = 0;
        if (CESsOfESG != null) {
            for (EventSequence es : CESsOfESG) {
                totalEventsInSuite += es.length();
            }
        }
    }

    public boolean isFaultDetected(ESG mutantESGFx) {
        eventsWalked = 0;
        currentCES = 0;
        lastDetectionReason = REASON_NOT_DETECTED;

        if (CESsOfESG == null || CESsOfESG.isEmpty()) {
            return false;
        }

        for (EventSequence testSequence : CESsOfESG) {
            currentCES++;
            List<Vertex> sequenceVertices = testSequence.getEventSequence();

            if (sequenceVertices == null || sequenceVertices.isEmpty()) {
                continue;
            }

            int previousVertexID = sequenceVertices.get(0).getID();
            Vertex mutantPreviousVertex = mutantESGFx.getVertexByID(previousVertexID);

            eventsWalked++;

            if (mutantPreviousVertex == null) {
                if (vertexInOriginal(previousVertexID)) {
                    lastDetectionReason = REASON_VERTEX_MISSING;
                    return true;
                }
                continue;
            }

            boolean abortSequence = false;

            for (int i = 1; i < sequenceVertices.size(); i++) {
                int currentVertexID = sequenceVertices.get(i).getID();
                Vertex mutantCurrentVertex = mutantESGFx.getVertexByID(currentVertexID);

                eventsWalked++;

                if (mutantCurrentVertex == null) {
                    if (vertexInOriginal(currentVertexID)) {
                        lastDetectionReason = REASON_VERTEX_MISSING;
                        return true;
                    }
                    abortSequence = true;
                    break;
                }

                boolean edgeExistsInMutant = adjacencyContains(
                        (ESGFx) mutantESGFx, mutantPreviousVertex, currentVertexID);

                if (!edgeExistsInMutant) {
                    boolean edgeExistedInOriginal = edgeInOriginal(
                            previousVertexID, currentVertexID);

                    if (edgeExistedInOriginal) {
                        lastDetectionReason = REASON_EDGE_MISSING;
                        return true;
                    }

                    abortSequence = true;
                    break;
                }

                previousVertexID = currentVertexID;
                mutantPreviousVertex = mutantCurrentVertex;
            }

            if (abortSequence) {
                continue;
            }
        }

        return false;
    }

    private boolean vertexInOriginal(int vertexID) {
        if (originalESGFx == null) {
            return true;
        }
        return originalESGFx.getVertexByID(vertexID) != null;
    }

    private boolean edgeInOriginal(int srcID, int tgtID) {
        if (originalESGFx == null) {
            return true;
        }
        Vertex origSrc = originalESGFx.getVertexByID(srcID);
        if (origSrc == null) {
            return false;
        }
        return adjacencyContains((ESGFx) originalESGFx, origSrc, tgtID);
    }

    private static boolean adjacencyContains(ESGFx graph, Vertex source, int targetID) {
        List<Vertex> adjacencyList = graph.getAdjacencyList(source);
        if (adjacencyList == null) {
            return false;
        }
        for (Vertex adj : adjacencyList) {
            if (adj.getID() == targetID) {
                return true;
            }
        }
        return false;
    }
}