package tr.edu.iyte.esgfx.mutationtesting.mutationoperators;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esg.model.validation.ESGValidator;
import tr.edu.iyte.esgfx.model.ESGFx;

public class EventOmitter extends MutationOperator {

    private Map<String, ESG> eventMutantMap;
    private int mutantID;

    public EventOmitter() {
        super();
        name = "Event Omitter";
        eventMutantMap = new LinkedHashMap<String, ESG>();
        mutantID = 0;
    }

    public Map<String, ESG> getEventMutantMap() {
        return eventMutantMap;
    }

    // --- LEGACY METHOD (High RAM Usage - Not used in optimized loop) ---
    @Override
    public void generateMutantESGFxSets(ESG ESGFx) {
        ESG cloneESGFx = new ESGFx(ESGFx);
        Set<Vertex> vertexSet = new LinkedHashSet<Vertex>();
        vertexSet.addAll(cloneESGFx.getRealVertexList());

        Iterator<Vertex> vertexSetIterator = vertexSet.iterator();

        while (vertexSetIterator.hasNext()) {
            Vertex vertex = vertexSetIterator.next();
            ESG mutantESGFx = omitEvent(cloneESGFx, vertex);
            eventMutantMap.put(vertex.toString(), mutantESGFx);
        }
    }

    // --- NEW OPTIMIZED METHOD (RAM Friendly & Safe) ---
    public ESG createSingleMutant(ESG originalESGFx, Vertex vertexToOmit, int currentMutantID) {

        // 1. Clone the graph
        ESG mutantESGFx = new ESGFx(originalESGFx);
        ((ESGFx) mutantESGFx).setID(currentMutantID);

        // 2. Safely remove edges (Prevent ConcurrentModificationException)
        // We create a copy (Snapshot) of the edge list to iterate over it safely.
        List<Edge> edgesToIterate = new ArrayList<>(mutantESGFx.getEdgeList());

        for (Edge edge : edgesToIterate) {
            // If the source or target of the edge is the vertex to be omitted, remove the edge
            if (edge.getSource().equals(vertexToOmit) || edge.getTarget().equals(vertexToOmit)) {
                mutantESGFx.removeEdge(edge);
            }
        }

        // 3. Remove the vertex and its event
        mutantESGFx.removeVertex(vertexToOmit);
        mutantESGFx.removeEvent(vertexToOmit.getEvent());

        return mutantESGFx;
    }
    
    // Helper method to check validity from external classes
    public boolean isValidMutant(ESG mutant) {
        ESGValidator validator = new ESGValidator();
        return validator.isValid(mutant);
    }

    // Internal helper for legacy method
    private ESG omitEvent(ESG cloneESGFx, Vertex vertex) {
        ESG mutantESGFx = new ESGFx(cloneESGFx);
        ((ESGFx) mutantESGFx).setID(++mutantID);
        ESGValidator ESGValidator = new ESGValidator();

        // Apply safe removal logic here as well
        List<Edge> edgesSnapshot = new ArrayList<>(cloneESGFx.getEdgeList());
        
        for (Edge edge : edgesSnapshot) {
            if (edge.getSource().equals(vertex) || edge.getTarget().equals(vertex)) {
                mutantESGFx.removeEdge(edge);
            }
        }
        
        mutantESGFx.removeVertex(vertex);
        mutantESGFx.removeEvent(vertex.getEvent());

        if (ESGValidator.isValid(mutantESGFx))
            getValidMutantESGFxSet().add(mutantESGFx);
        else
            getInvalidMutantESGFxSet().add(mutantESGFx);

        return mutantESGFx;
    }
}