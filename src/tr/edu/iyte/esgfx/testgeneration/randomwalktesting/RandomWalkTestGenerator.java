package tr.edu.iyte.esgfx.testgeneration.randomwalktesting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Random;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.ESGFx;

public class RandomWalkTestGenerator {

    private final ESGFx graph;
    
    private final java.util.Map<Long, Integer> edgeLookup;
    
    private final Random random;
    private final double dampingFactor;

    private boolean safetyLimitHit = false;
    private long executionTimeMs = 0;
    private int stepsTaken = 0;
    private double achievedCoverage = 0.0;
    
    private int abortedSequenceCount = 0;

    public RandomWalkTestGenerator(ESGFx graph, double dampingFactor, long seed) {
        this.graph = graph;
        this.dampingFactor = dampingFactor;
        this.random = new Random(seed);
        
        
        this.edgeLookup = new java.util.HashMap<>();
        for (Edge edge : graph.getEdgeList()) {
            long key = ((long) edge.getSource().getID() << 32) 
                     | (edge.getTarget().getID() & 0xFFFFFFFFL);
            edgeLookup.put(key, edge.getID());
        }
    }

    public boolean isSafetyLimitHit() {
        return safetyLimitHit;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public int getStepsTaken() {
        return stepsTaken;
    }

    public double getAchievedCoverage() {
        return achievedCoverage;
    }

    public int getAbortedSequenceCount() {
        return abortedSequenceCount;
    }

    public Set<EventSequence> generateWalkUntilEdgeCoverage(double targetCoveragePercentage, int maxStepsSafetyLimit) {
        long startTime = System.nanoTime();

        Set<EventSequence> cesSet = new HashSet<>();
        Set<Integer> globallyVisitedEdgeIDs = new HashSet<>();
        
        List<Vertex> currentSequenceBuffer = new ArrayList<>();
        Set<Integer> currentSequenceEdgeIDs = new HashSet<>();

        int totalEdgesInGraph = graph.getEdgeList().size();

        if (totalEdgesInGraph == 0) {
            this.achievedCoverage = 100.0;
            this.safetyLimitHit = false;
            this.stepsTaken = 0;
            this.executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            return cesSet;
        }

        Vertex startVertex = findPseudoStartVertex();
        if (startVertex == null)
            throw new IllegalStateException("Graph does not contain '['.");

        Vertex current = startVertex;
        int steps = 0;
        boolean coverageSatisfied = false;

        while (steps < maxStepsSafetyLimit) {

            Vertex next = getNextVertex(current, startVertex);

            Integer edgeID = getRealEdgeID(current, next);
            if (edgeID != null) {
                // FIX: Add to temporary buffer, NOT the global completed list yet
                currentSequenceEdgeIDs.add(edgeID);
            }

            if (current.isPseudoEndVertex()) {
                if (!currentSequenceBuffer.isEmpty()) {
                    EventSequence ces = new EventSequence();
                    ces.setEventSequence(new ArrayList<>(currentSequenceBuffer));
                    cesSet.add(ces);
                    
                    // FIX: Sequence is valid. Now commit its edges to the global coverage
                    globallyVisitedEdgeIDs.addAll(currentSequenceEdgeIDs);
                }

                currentSequenceBuffer.clear();
                currentSequenceEdgeIDs.clear();

                if (globallyVisitedEdgeIDs.size() >= (totalEdgesInGraph * targetCoveragePercentage / 100.0)) {
                    coverageSatisfied = true;
                    break;
                }

            } else if (next.isPseudoStartVertex() && !current.isPseudoEndVertex()) {
                // FIX: Teleport happened. The sequence is aborted.
                // We MUST discard the edges visited in this aborted sequence.
                currentSequenceBuffer.clear();
                currentSequenceEdgeIDs.clear();
                abortedSequenceCount++;

            } else {
                if (!next.isPseudoEndVertex()) {
                    currentSequenceBuffer.add(next);
                }
            }

            current = next;
            steps++;
        }
        
        this.executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;
        this.stepsTaken = steps;

        if (totalEdgesInGraph > 0) {
            this.achievedCoverage = ((double) globallyVisitedEdgeIDs.size() / totalEdgesInGraph) * 100.0;
        } else {
            this.achievedCoverage = 100.0;
        }

        if (!coverageSatisfied && steps >= maxStepsSafetyLimit) {
            this.safetyLimitHit = true;
        } else {
            this.safetyLimitHit = false;
        }

        return cesSet;
    }

    private Vertex getNextVertex(Vertex current, Vertex startVertex) {
        if (current.isPseudoEndVertex())
            return startVertex;
            
        if (random.nextDouble() > dampingFactor)
            return startVertex;

        List<Vertex> neighbors = graph.getAdjacencyMap().get(current);

        if (neighbors == null || neighbors.isEmpty())
            return startVertex;

        return neighbors.get(random.nextInt(neighbors.size()));
    }

    // FIX: Locate edge directly via connected vertices, not by string names
    private Integer getRealEdgeID(Vertex source, Vertex target) {
        long key = ((long) source.getID() << 32) | (target.getID() & 0xFFFFFFFFL);
        return edgeLookup.get(key);
    }

    private Vertex findPseudoStartVertex() {
        for (Vertex v : graph.getVertexList()) {
            if (v.isPseudoStartVertex())
                return v;
        }
        return null;
    }
}