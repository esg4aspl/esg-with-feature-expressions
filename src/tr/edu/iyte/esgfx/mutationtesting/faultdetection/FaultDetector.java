package tr.edu.iyte.esgfx.mutationtesting.faultdetection;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.ESGFx;

public class FaultDetector {

    // --- Kill reason constants (A3) ---
    // Exposed so callers can classify each kill as edge-missing vs vertex-missing.
    public static final String REASON_NOT_DETECTED = "NOT_DETECTED";
    public static final String REASON_VERTEX_MISSING = "VERTEX_MISSING";
    public static final String REASON_EDGE_MISSING = "EDGE_MISSING";

    private Set<EventSequence> CESsOfESG;
    private int currentCES;
    private int eventsWalked;
    private int totalEventsInSuite;

    // A3: records the reason for the LAST isFaultDetected() call.
    // One of the REASON_* constants above.
    private String lastDetectionReason = REASON_NOT_DETECTED;

    public FaultDetector() {
        CESsOfESG = new LinkedHashSet<>();
    }

    public FaultDetector(Set<EventSequence> CESsOfESG) {
        setCESsOfESG(CESsOfESG);
    }

    public Set<EventSequence> getCESsOfESG() {
        return CESsOfESG;
    }

    public void setCESsOfESG(Set<EventSequence> cESsOfESG) {
        CESsOfESG = cESsOfESG;
        calculateTotalEventsInSuite();
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

    /**
     * A3: Returns the reason why the last isFaultDetected() call returned true/false.
     * Values: REASON_NOT_DETECTED, REASON_VERTEX_MISSING, REASON_EDGE_MISSING.
     */
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
        // A3: reset reason at the start of every call
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
                lastDetectionReason = REASON_VERTEX_MISSING;
                return true;
            }

            for (int i = 1; i < sequenceVertices.size(); i++) {
                int currentVertexID = sequenceVertices.get(i).getID();
                Vertex mutantCurrentVertex = mutantESGFx.getVertexByID(currentVertexID);

                eventsWalked++;

                if (mutantCurrentVertex == null) {
                    lastDetectionReason = REASON_VERTEX_MISSING;
                    return true;
                }

                boolean edgeExists = false;
                List<Vertex> adjacencyList = ((ESGFx) mutantESGFx).getAdjacencyList(mutantPreviousVertex);

                if (adjacencyList != null) {
                    for (Vertex adj : adjacencyList) {
                        if (adj.getID() == currentVertexID) {
                            edgeExists = true;
                            break;
                        }
                    }
                }

                if (!edgeExists) {
                    lastDetectionReason = REASON_EDGE_MISSING;
                    return true;
                }

                previousVertexID = currentVertexID;
                mutantPreviousVertex = mutantCurrentVertex;
            }
        }

        return false;
    }
}