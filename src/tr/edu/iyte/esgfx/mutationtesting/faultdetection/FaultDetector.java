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
    
    private int currentCES;
    

    
    // Optimization Cache: Map Vertex to its possible next vertices based on Test Suite
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
    
    public int getCurrentCES() {
    	return currentCES;
    }

    private void initializer() {
        visitedEventsOnMutant = new LinkedHashSet<String>();
        visitedEdgesOnMutant = new LinkedHashSet<String>();

        insertedEventSet = new LinkedHashSet<String>();
        insertedEdgeSet = new LinkedHashSet<String>();

        omittedEventSet = new LinkedHashSet<String>();
        omittedEdgeSet = new LinkedHashSet<String>();
        
        nextStepsCache = new HashMap<>();

        // Precompute sets and cache for O(1) access
        preComputeTestData();
    }
    
    // Replaces slow setVerticesOnEventSequences & setEdgesOnEventSequences
    private void preComputeTestData() {
        eventsOnEventSequences = new HashSet<>();
        edgesOnEventSequences = new HashSet<>();
        
        for (EventSequence eventSequence : CESsOfESG) {
            List<Vertex> seq = eventSequence.getEventSequence();
            
            for (int i = 0; i < seq.size(); i++) {
                Vertex current = seq.get(i);
                eventsOnEventSequences.add(current.toString());
                
                if (i < seq.size() - 1) {
                    Vertex next = seq.get(i + 1);
                    String edge = "<" + current.toString() + ", " + next.toString() + ">";
                    edgesOnEventSequences.add(edge);
                    
                    // Fill Cache
                    nextStepsCache.computeIfAbsent(current, k -> new HashSet<>()).add(next);
                }
            }
        }
    }

    // Legacy method signatures kept for compatibility, logic moved to preComputeTestData
    public void setVerticesOnEventSequences() { }
    public void setEdgesOnEventSequences() { }

    public boolean isAllEventsOnTheSequenceAreOmitted(ESG mutantESGFx, EventSequence eventSequence) {
        boolean isAllEventsOnTheSequenceAreOmitted = true;
        for (Vertex vertex : eventSequence.getEventSequence()) {
            // Optimized ID-based check via getVertexByID in ESG
            isAllEventsOnTheSequenceAreOmitted = isAllEventsOnTheSequenceAreOmitted
                    && (mutantESGFx.getVertexByID(vertex.getID()) == null);
        }
        return isAllEventsOnTheSequenceAreOmitted;
    }

    public boolean isFaultDetected(ESG mutantESGFx) {
        initializer();

        for (EventSequence eventSequence : CESsOfESG) {
        	currentCES++;
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

    // --- OPTIMIZED SET CHECKS (O(1)) ---

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
    
    // Kept for compatibility
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

//    private boolean isEventOnEventSequenceVisited(String vertexOnEventSequences) {
//        return visitedEventsOnMutant.contains(vertexOnEventSequences);
//    }
    
    // --- OPTIMIZED TRAVERSAL ---

    public boolean traverseESGForEventSequence(ESG mutantESGFx, EventSequence eventSequence) {
        Vertex startVertex = eventSequence.getStartVertex();
        
        // Use ID-based lookup for O(1) speed
        Vertex startVertexOnMutantESGFx = mutantESGFx.getVertexByID(startVertex.getID());

        int start = 0;
        while (startVertexOnMutantESGFx == null && start < eventSequence.length() - 1) {
            start++;
            omittedEventSet.add(startVertex.toString());
            
            startVertex = eventSequence.getEventSequence().get(start);
            startVertexOnMutantESGFx = (VertexRefinedByFeatureExpression) mutantESGFx.getVertexByID(startVertex.getID());
        }

        Vertex endVertex = eventSequence.getEndVertex();
        Vertex endVertexOnMutantESGFx = mutantESGFx.getVertexByID(endVertex.getID());
        
        int eventSequenceLength = eventSequence.length();
        while (endVertexOnMutantESGFx == null && eventSequenceLength > 0) {
            eventSequenceLength--;
            omittedEventSet.add(endVertex.toString());
            if(eventSequenceLength > 0) {
                endVertex = eventSequence.getEventSequence().get(eventSequenceLength-1);
                endVertexOnMutantESGFx = (VertexRefinedByFeatureExpression) mutantESGFx.getVertexByID(endVertex.getID());
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
        if(source == null) return;

        Set<Vertex> visitedVertices = new HashSet<>(); // Faster HashSet
        Queue<Vertex> queue = new LinkedList<>();
        visitedVertices.add(source);
        queue.add(source);
        
        // Use ID lookup
        Vertex sourceOnMutantESGFx = ((ESGFx) mutantESGFx).getVertexByID(source.getID());

        if (sourceOnMutantESGFx == null) {
            omittedEventSet.add(source.toString());
        } else {
            visitedEventsOnMutant.add(sourceOnMutantESGFx.toString());
        }

        while (!queue.isEmpty()) {
            source = queue.poll();
            
            // --- CACHE OPTIMIZATION ---
            // Instead of slow indexOf search, use precomputed cache
            Set<Vertex> validNextSteps = nextStepsCache.get(source);
            
            if(validNextSteps == null || validNextSteps.isEmpty()) continue;

            List<Vertex> adjacencyList = ((ESGFx) mutantESGFx).getAdjacencyList(source);
            
            if(adjacencyList != null) {
                for (Vertex adjacent : adjacencyList) {
                    
                    // O(1) Check
                    if (validNextSteps.contains(adjacent)) {
                        
                        if (!adjacent.isPseudoEndVertex()) {
                            if (!visitedVertices.contains(adjacent)) {
                                visitedVertices.add(adjacent);
                                queue.add(adjacent);
                                String edgeString = "<" + source.toString() + ", " + adjacent.toString() + ">";

                                // Use ID lookup
                                Vertex vertexOnMutant = mutantESGFx.getVertexByID(adjacent.getID());

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
    
    // Legacy method, no longer used but kept for API compatibility
    public List<Vertex> getAdjacentVerticesOfVertex(Set<EventSequence> CESsOfESG, Vertex vertex) {
        return new ArrayList<>(); 
    }
}