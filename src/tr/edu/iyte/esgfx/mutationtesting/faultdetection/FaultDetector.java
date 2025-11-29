package tr.edu.iyte.esgfx.mutationtesting.faultdetection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.ArrayList;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esg.model.Edge; 
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;

public class FaultDetector {

    private Set<EventSequence> CESsOfESG;

    private Set<String> visitedEventsOnMutant;
    private Set<String> visitedEdgesOnMutant;

    private Set<String> eventsOnEventSequences;
    private Set<String> edgesOnEventSequences;

    private Set<String> insertedEventSet;
    private Set<String> insertedEdgeSet;

    private Set<String> omittedEventSet;
    private Set<String> omittedEdgeSet;
    
    // CACHE: Hangi düğümden sonra hangileri geliyor?
    private Map<Vertex, Set<Vertex>> nextStepsCache;

    public FaultDetector() {
        CESsOfESG = new LinkedHashSet<EventSequence>();
    }

    public FaultDetector(Set<EventSequence> CESsOfESG) {
        setCESsOfESG(CESsOfESG);
    }

    public Set<EventSequence> getCESsOfESG() {
        return CESsOfESG;
    }

    public void setCESsOfESG(Set<EventSequence> cESsOfESG) {
        CESsOfESG = cESsOfESG;
    }

    private void initializer() {
        visitedEventsOnMutant = new LinkedHashSet<String>();
        visitedEdgesOnMutant = new LinkedHashSet<String>();

        insertedEventSet = new LinkedHashSet<String>();
        insertedEdgeSet = new LinkedHashSet<String>();

        omittedEventSet = new LinkedHashSet<String>();
        omittedEdgeSet = new LinkedHashSet<String>();
        
        
        nextStepsCache = new HashMap<>();

       
        setVerticesOnEventSequences();
        setEdgesOnEventSequences();
        
        
        preComputeAdjacencyCache();
    }
    

    private void preComputeAdjacencyCache() {
        for (EventSequence eventSequence : CESsOfESG) {
            List<Vertex> seq = eventSequence.getEventSequence();
            

            for (int i = 0; i < seq.size() - 1; i++) {
                Vertex current = seq.get(i);
                Vertex next = seq.get(i+1);
                
                nextStepsCache.computeIfAbsent(current, k -> new HashSet<>()).add(next);
            }
        }
    }

    public void setVerticesOnEventSequences() {
        eventsOnEventSequences = new LinkedHashSet<String>();
        for (EventSequence eventSequence : CESsOfESG) {
            for (Vertex vertex : eventSequence.getEventSequence()) {
                eventsOnEventSequences.add(vertex.toString());
            }
        }
    }

    public void setEdgesOnEventSequences() {
        edgesOnEventSequences = new LinkedHashSet<String>();
        for (EventSequence eventSequence : CESsOfESG) {
            List<Vertex> seq = eventSequence.getEventSequence();
            for (int i = 0; i < seq.size() - 1; i++) {
               Vertex v1 = seq.get(i);
               Vertex v2 = seq.get(i+1);
               String edge = "<" + v1.toString() + ", " + v2.toString() + ">";
               edgesOnEventSequences.add(edge);
            }
        }
    }

    public boolean isAllEventsOnTheSequenceAreOmitted(ESG mutantESGFx, EventSequence eventSequence) {
        boolean isAllEventsOnTheSequenceAreOmitted = true;
        for (Vertex vertex : eventSequence.getEventSequence()) {
            isAllEventsOnTheSequenceAreOmitted = isAllEventsOnTheSequenceAreOmitted
                    && (mutantESGFx.getVertexByEventName(vertex.toString()) == null);
        }
        return isAllEventsOnTheSequenceAreOmitted;
    }

    public boolean isFaultDetected(ESG mutantESGFx) {
        initializer();

        for (EventSequence eventSequence : CESsOfESG) {
            if(isAllEventsOnTheSequenceAreOmitted(mutantESGFx, eventSequence)){
                continue;
            } else {
                traverseESGForEventSequence(mutantESGFx, eventSequence);
            }
        }
        
        boolean isEdgeInserted = isEdgeInserted();
        boolean isEventInserted = false;
        boolean isEventOmitted = false;

        if (isEdgeInserted) {
            isEventInserted = isEventInserted();
        }

        boolean isEdgeOmitted = isEdgeOmitted();
        if (isEdgeOmitted) {
            isEventOmitted = isEventOmitted();
        }

        return isEdgeInserted || isEdgeOmitted || isEventInserted || isEventOmitted;
    }

   

    public boolean isEdgeInserted() {
        boolean allVisitedEdgesOnEventSequences = true;
        for (String visitedEdge : visitedEdgesOnMutant) {
            
            boolean isVisitedEdgeOnEventSequences = edgesOnEventSequences.contains(visitedEdge);
            
            if (!isVisitedEdgeOnEventSequences) {
                insertedEdgeSet.add(visitedEdge);
            }
            allVisitedEdgesOnEventSequences = allVisitedEdgesOnEventSequences && isVisitedEdgeOnEventSequences;
        }
        return !allVisitedEdgesOnEventSequences;
    }

    // Artık kullanılmıyor ama uyumluluk için durabilir
    private boolean isVisitedEdgeOnEventSequences(String visitedEdge) {
        return edgesOnEventSequences.contains(visitedEdge);
    }

    public boolean isEdgeOmitted() {
        boolean allEdgesOnEventSequenceVisited = true;
        for (String edgeOnEventSequences : edgesOnEventSequences) {
            boolean isEdgeVisited = visitedEdgesOnMutant.contains(edgeOnEventSequences);
            if (!isEdgeVisited) {
                omittedEdgeSet.add(edgeOnEventSequences);
            }
            allEdgesOnEventSequenceVisited = allEdgesOnEventSequenceVisited && isEdgeVisited;
        }
        return !allEdgesOnEventSequenceVisited;
    }

    public boolean isEventInserted() {
        boolean allVisitedVerticesOnEventSequences = true;
        for (String visitedVertex : visitedEventsOnMutant) {
            boolean isVisitedVertexOnEventSequences = eventsOnEventSequences.contains(visitedVertex);
            if (!isVisitedVertexOnEventSequences) {
                insertedEventSet.add(visitedVertex);
            }
            allVisitedVerticesOnEventSequences = allVisitedVerticesOnEventSequences && isVisitedVertexOnEventSequences;
        }
        return !allVisitedVerticesOnEventSequences;
    }

    public boolean isVisitedVertexOnEventSequences(String visitedVertex) {
        return eventsOnEventSequences.contains(visitedVertex);
    }

    public boolean isEventOmitted() {
        boolean allEventsOnEventSequenceVisited = true;
        for (String eventOnEventSequences : eventsOnEventSequences) {
            boolean isEventOnEventSequenceVisited = visitedEventsOnMutant.contains(eventOnEventSequences);
            if (!isEventOnEventSequenceVisited) {
                omittedEventSet.add(eventOnEventSequences);
            }
            allEventsOnEventSequenceVisited = allEventsOnEventSequenceVisited && isEventOnEventSequenceVisited;
        }
        return !allEventsOnEventSequenceVisited;
    }

    private boolean isEventOnEventSequenceVisited(String vertexOnEventSequences) {
        return visitedEventsOnMutant.contains(vertexOnEventSequences);
    }
    
   

    public boolean traverseESGForEventSequence(ESG mutantESGFx, EventSequence eventSequence) {
        Vertex startVertex = eventSequence.getStartVertex();
        Vertex startVertexOnMutantESGFx = mutantESGFx.getVertexByEventName(startVertex.toString());

        int start = 0;
        while (startVertexOnMutantESGFx == null && start < eventSequence.length() - 1) { // Bounds check
            start++;
            omittedEventSet.add(startVertex.toString());
            startVertex = eventSequence.getEventSequence().get(start);
            startVertexOnMutantESGFx = (VertexRefinedByFeatureExpression) mutantESGFx.getVertexByEventName(startVertex.toString());
        }

        Vertex endVertex = eventSequence.getEndVertex();
        Vertex endVertexOnMutantESGFx = mutantESGFx.getVertexByEventName(endVertex.toString());
        
        int eventSequenceLength = eventSequence.length();
        while (endVertexOnMutantESGFx == null && eventSequenceLength > 0) { // Bounds check
            eventSequenceLength--;
            omittedEventSet.add(endVertex.toString());
            if(eventSequenceLength > 0) {
                endVertex = eventSequence.getEventSequence().get(eventSequenceLength-1);
                endVertexOnMutantESGFx = (VertexRefinedByFeatureExpression) mutantESGFx.getVertexByEventName(endVertex.toString());
            }
        }

        for (Vertex vertex : eventSequence.getEventSequence()) {
            if (!vertex.equals(startVertex)) {
                traverseESGFromSourceToTarget(mutantESGFx, startVertexOnMutantESGFx, vertex);
                if(startVertexOnMutantESGFx != null) visitedEventsOnMutant.add(startVertexOnMutantESGFx.toString());
            }

            if (!vertex.equals(endVertex) && !vertex.equals(startVertex)) {
                traverseESGFromSourceToTarget(mutantESGFx, vertex, endVertexOnMutantESGFx);
                if(endVertexOnMutantESGFx != null) visitedEventsOnMutant.add(endVertexOnMutantESGFx.toString());
            }
        }
        return true; 
    }

    public void traverseESGFromSourceToTarget(ESG mutantESGFx, Vertex source, Vertex target) {
        if(source == null) return; // Null safety

        Set<Vertex> visitedVertices = new LinkedHashSet<Vertex>();
        Queue<Vertex> queue = new LinkedList<Vertex>();
        visitedVertices.add(source);
        queue.add(source);
        
        Vertex sourceOnMutantESGFx = ((ESGFx) mutantESGFx).getVertexByEventName(source.toString());

        if (sourceOnMutantESGFx == null) {
            omittedEventSet.add(source.toString());
        } else {
            visitedEventsOnMutant.add(sourceOnMutantESGFx.toString());
        }

        while (!queue.isEmpty()) {
            source = queue.poll();
            
            Set<Vertex> validNextSteps = nextStepsCache.get(source);
            

            if(validNextSteps == null || validNextSteps.isEmpty()) continue;

            List<Vertex> adjacencyList = ((ESGFx) mutantESGFx).getAdjacencyList(source);
            

            if(adjacencyList != null) {
                for (Vertex adjacent : adjacencyList) {
                    

                    if (validNextSteps.contains(adjacent)) {
                        
                        if (!adjacent.isPseudoEndVertex()) {
                            if (!visitedVertices.contains(adjacent)) {
                                visitedVertices.add(adjacent);
                                queue.add(adjacent);
                                String edgeString = "<" + source.toString() + ", " + adjacent.toString() + ">";

                                Vertex vertexOnMutant = mutantESGFx.getVertexByEventName(adjacent.toString());

                                if (vertexOnMutant == null) {
                                    omittedEventSet.add(adjacent.toString());
                                } else {
                                    visitedEventsOnMutant.add(adjacent.toString());
                                }
                                visitedEdgesOnMutant.add(edgeString);
                            }
                        }
                    }
                }
            }
        }
    }
    
    public List<Vertex> getAdjacentVerticesOfVertex(Set<EventSequence> CESsOfESG, Vertex vertex) {
        return new ArrayList<>(); 
    }
}