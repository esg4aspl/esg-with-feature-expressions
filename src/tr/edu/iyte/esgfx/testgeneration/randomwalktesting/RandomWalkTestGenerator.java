package tr.edu.iyte.esgfx.testgeneration.randomwalktesting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.security.SecureRandom;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.ESGFx;

public class RandomWalkTestGenerator {

    private final ESGFx graph;
    private final SecureRandom random;
    private final double dampingFactor; 

    public RandomWalkTestGenerator(ESGFx graph, double dampingFactor) {
        this.graph = graph;
        this.dampingFactor = dampingFactor;
        this.random = new SecureRandom();
    }

    public Set<EventSequence> generateWalkUntilEdgeCoverage(double targetCoveragePercentage, int maxStepsSafetyLimit) {
        
        Set<EventSequence> cesSet = new HashSet<>();
        Set<Integer> visitedEdgeIDs = new HashSet<>();
        List<Vertex> currentSequenceBuffer = new ArrayList<>();

        // Optimization: Use list size directly (O(1) in ArrayList)
        int totalEdgesInGraph = graph.getEdgeList().size();
        
        if (totalEdgesInGraph == 0) {
             return cesSet; 
        }

        Vertex startVertex = findPseudoStartVertex();
        if (startVertex == null) throw new IllegalStateException("Graph does not contain '['.");

        Vertex current = startVertex;
        int steps = 0;
        boolean coverageSatisfied = false;

        while (steps < maxStepsSafetyLimit) {
            
            Vertex next = getNextVertex(current, startVertex);
            
            // --- Coverage Logic (Optimized) ---
            Integer edgeID = getRealEdgeID(current, next);
            if (edgeID != null) {
                visitedEdgeIDs.add(edgeID);
            }
            
            // Only re-calculate coverage if a new edge was found (Micro-optimization)
            if (visitedEdgeIDs.size() >= (totalEdgesInGraph * targetCoveragePercentage / 100.0)) {
                coverageSatisfied = true;
            }

            // --- Sequence Construction Logic ---
            
            if (current.isPseudoEndVertex()) {
                // Sequence Complete
                
                // Only create sequence object if we have steps
                if (!currentSequenceBuffer.isEmpty()) {
                    EventSequence ces = new EventSequence();
                    ces.setEventSequence(new ArrayList<>(currentSequenceBuffer));
                    cesSet.add(ces);
                }
                
                currentSequenceBuffer.clear();
                
                // Stop if coverage met AND sequence finished
                if (coverageSatisfied) {
                    break; 
                }
                
            } else if (next.isPseudoStartVertex() && !current.isPseudoEndVertex()) {
                // Broken Sequence (Teleport)
                currentSequenceBuffer.clear();
                
            } else {
                // Normal Traversal
                if (!next.isPseudoEndVertex()) {
                    currentSequenceBuffer.add(next);
                }
            }

            current = next;
            steps++;
        }

        return cesSet;
    }

    // --- Helper Methods ---

    private Vertex getNextVertex(Vertex current, Vertex startVertex) {
        if (current.isPseudoEndVertex()) return startVertex;
        if (random.nextDouble() > dampingFactor) return startVertex;
        
        // Optimization: getAdjacencyMap().get() is O(1)
        List<Vertex> neighbors = graph.getAdjacencyMap().get(current);
        
        if (neighbors == null || neighbors.isEmpty()) return startVertex; 
        
        return neighbors.get(random.nextInt(neighbors.size()));
    }

    // --- CRITICAL OPTIMIZATION: O(1) Lookup ---
    private Integer getRealEdgeID(Vertex source, Vertex target) {
        // Use the Cache-based lookup method we optimized in ESG/ESGFx class
        Edge edge = graph.getEdgeBySourceEventNameTargetEventName(
                source.getEvent().getName(), 
                target.getEvent().getName()
        );
        
        if (edge != null) {
            return edge.getID();
        }
        return null;
    }

    private Vertex findPseudoStartVertex() {
        // Faster: Use the optimized method from ESG class if available
        // return graph.getPseudoStartVertex(); 
        
        // Or manual iteration (acceptable since it runs once)
        for (Vertex v : graph.getVertexList()) {
            if (v.isPseudoStartVertex()) return v;
        }
        return null;
    }
}