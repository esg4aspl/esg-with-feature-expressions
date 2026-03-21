package tr.edu.iyte.esgfx.mutationtesting.faultdetection;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.ESGFx;

public class FaultDetector {

    private Set<EventSequence> CESsOfESG;
    private int currentCES;
    private int eventsWalked;
    private int totalEventsInSuite;

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
                return true; 
            }

            for (int i = 1; i < sequenceVertices.size(); i++) {
                int currentVertexID = sequenceVertices.get(i).getID();
                Vertex mutantCurrentVertex = mutantESGFx.getVertexByID(currentVertexID);
                
                eventsWalked++; 
                
                if (mutantCurrentVertex == null) {
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
                    return true;
                }

                previousVertexID = currentVertexID;
                mutantPreviousVertex = mutantCurrentVertex;
            }
        }
        
        return false;
    }
}