package tr.edu.iyte.esgfx.testexecution;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.ESGFx;

public class TestExecutor {

    private Set<EventSequence> CESsOfESG;

    public TestExecutor() {
        CESsOfESG = new LinkedHashSet<>();
    }

    public TestExecutor(Set<EventSequence> CESsOfESG) {
        setCESsOfESG(CESsOfESG);
    }

    public Set<EventSequence> getCESsOfESG() {
        return CESsOfESG;
    }

    public void setCESsOfESG(Set<EventSequence> cESsOfESG) {
        CESsOfESG = cESsOfESG;
    }

    public void executeAllTests(ESG productESGFx) {
        if (CESsOfESG == null || CESsOfESG.isEmpty()) {
            return;
        }

        for (EventSequence eventSequence : CESsOfESG) {
            traverseESGForEventSequence(productESGFx, eventSequence);
        }
    }

    public boolean traverseESGForEventSequence(ESG productESGFx, EventSequence eventSequence) {
        List<Vertex> sequenceVertices = eventSequence.getEventSequence();

        if (sequenceVertices == null || sequenceVertices.isEmpty()) {
            return false;
        }

        int previousVertexID = sequenceVertices.get(0).getID();
        Vertex productPreviousVertex = productESGFx.getVertexByID(previousVertexID);

        if (productPreviousVertex == null) {
            return false;
        }

        for (int i = 1; i < sequenceVertices.size(); i++) {
            int currentVertexID = sequenceVertices.get(i).getID();
            Vertex productCurrentVertex = productESGFx.getVertexByID(currentVertexID);

            if (productCurrentVertex == null) {
                return false;
            }

            boolean edgeExists = false;
            List<Vertex> adjacencyList = ((ESGFx) productESGFx).getAdjacencyList(productPreviousVertex);

            if (adjacencyList != null) {
                for (Vertex adj : adjacencyList) {
                    if (adj.getID() == currentVertexID) {
                        edgeExists = true;
                        break;
                    }
                }
            }

            if (!edgeExists) {
                return false;
            }

            previousVertexID = currentVertexID;
            productPreviousVertex = productCurrentVertex;
        }

        return true; 
    }
}