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

        int totalEdgesInGraph = graph.getEdgeList().size();
        if (totalEdgesInGraph == 0) {
            // Edge case: Graph with no edges is implicitly 100% covered but generates no sequences.
             return cesSet; 
        }

        Vertex startVertex = findPseudoStartVertex();
        if (startVertex == null) throw new IllegalStateException("Graph does not contain '['.");

        Vertex current = startVertex;
        // Buffer starts empty (we don't store '[')

        int steps = 0;
        double currentCoverage = 0.0;
        boolean coverageSatisfied = false;

        // MODIFIED LOOP CONDITION:
        // We continue if steps are within limit. 
        // We decide to BREAK inside the loop only when coverage is met AND sequence is finished.
        while (steps < maxStepsSafetyLimit) {
            
            Vertex next = getNextVertex(current, startVertex);
            
            // --- Coverage Logic ---
            Integer edgeID = getRealEdgeID(current, next);
            if (edgeID != null) {
                visitedEdgeIDs.add(edgeID);
            }
            
            // Update coverage
            currentCoverage = (double) visitedEdgeIDs.size() / totalEdgesInGraph * 100.0;
            
            // Check if we reached the target coverage
            if (currentCoverage >= targetCoveragePercentage) {
                coverageSatisfied = true;
            }

            // --- Sequence Construction Logic ---
            
            if (current.isPseudoEndVertex()) {
                // SCENARIO 1: Reached ']' (Sequence Complete)
                
                EventSequence ces = new EventSequence();
                ces.setEventSequence(new ArrayList<>(currentSequenceBuffer));
                cesSet.add(ces);
                
                currentSequenceBuffer.clear();
                
                // CRITICAL FIX:
                // Only stop if we have enough coverage AND we just finished a sequence.
                // This prevents stopping in the middle of a path.
                if (coverageSatisfied) {
                    break; 
                }
                
            } else if (next.isPseudoStartVertex() && !current.isPseudoEndVertex()) {
                // SCENARIO 2: Teleportation (Broken Sequence)
                currentSequenceBuffer.clear();
                
            } else {
                // SCENARIO 3: Normal Traversal
                if (!next.isPseudoEndVertex()) {
                    currentSequenceBuffer.add(next);
                }
            }

            current = next;
            steps++;
        }

//        // Logging
//        System.out.println("Generation finished.");
//        System.out.println("Total Steps: " + steps);
//        System.out.println("Edge Coverage: " + String.format("%.2f", currentCoverage) + "%");
//        System.out.println("Generated " + cesSet.size() + " unique Complete Event Sequences.");

        return cesSet;
    }

    // --- Helper Methods (Unchanged) ---

    private Vertex getNextVertex(Vertex current, Vertex startVertex) {
        if (current.isPseudoEndVertex()) return startVertex;
        if (random.nextDouble() > dampingFactor) return startVertex;
        
        List<Vertex> neighbors = graph.getAdjacencyMap().get(current);
        if (neighbors == null || neighbors.isEmpty()) return startVertex; 
        
        return neighbors.get(random.nextInt(neighbors.size()));
    }

    private Integer getRealEdgeID(Vertex source, Vertex target) {
        for (Edge edge : graph.getEdgeList()) {
            if (edge.getSource().equals(source) && edge.getTarget().equals(target)) {
                return edge.getID();
            }
        }
        return null;
    }

    private Vertex findPseudoStartVertex() {
        for (Vertex v : graph.getVertexList()) {
            if (v.isPseudoStartVertex()) return v;
        }
        return null;
    }
}