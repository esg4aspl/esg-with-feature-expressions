package tr.edu.iyte.esgfx.testgeneration.edgecoverage;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.CompatibilityChecker;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

public class EulerCycleGeneratorForEdgeCoverage {

    private List<Vertex> vertexStack;
    private List<Vertex> eulerCycle;
    private Map<Vertex, List<Vertex>> adjacencyMap;

    // OPTIMIZATION 1: Make this a field to avoid creating it repeatedly
    private final CompatibilityChecker compatibilityChecker;

    public EulerCycleGeneratorForEdgeCoverage() {
        this.vertexStack = new Stack<>();
        this.eulerCycle = new LinkedList<>();
        this.adjacencyMap = new LinkedHashMap<>();
        this.compatibilityChecker = new CompatibilityChecker();
    }

    public void generateEulerCycle(ESG stronglyConnectedBalancedESGFx) {
        // 1. Create a clone because Euler Cycle algorithm destroys edges
        ESG localESGFx = new ESGFx(stronglyConnectedBalancedESGFx);
        
        // OPTIMIZATION 2: Direct Reference instead of copying (putAll)
        // Since 'localESGFx' is already a fresh copy, we can use its map directly.
        // This saves iterating over the map and duplicating the list references.
        this.adjacencyMap = ((ESGFx) localESGFx).getAdjacencyMap();

        // Ensure compatibility checker is clean
        compatibilityChecker.clearFeatureTruthValueMap();

        Vertex currentVertex = localESGFx.getPseudoStartVertex();
        ((Stack<Vertex>) vertexStack).push(currentVertex);

        while (!vertexStack.isEmpty()) {
            List<Vertex> adjacentVertexList = adjacencyMap.get(currentVertex);
            
            // Safety check for null adjacency
            if (adjacentVertexList == null) {
                 // Should not happen in a balanced graph, but safety first
                 eulerCycle.add(currentVertex);
                 currentVertex = ((Stack<Vertex>) vertexStack).pop();
                 continue;
            }

            Iterator<Vertex> adjacentVertexListIterator = adjacentVertexList.iterator();

            if (currentVertex.isPseudoStartVertex()) {
                compatibilityChecker.clearFeatureTruthValueMap();
            } else if (!currentVertex.isPseudoStartVertex() && !currentVertex.isPseudoEndVertex()) {
                if (!compatibilityChecker.isFeatureTruthValueMapContainsKey(
                        ((VertexRefinedByFeatureExpression) currentVertex).getFeatureExpression().getFeature())) {
                    compatibilityChecker.fillFeatureTruthValueMap(currentVertex);
                }
            }

            if (!adjacentVertexList.isEmpty()) {
                ((Stack<Vertex>) vertexStack).push(currentVertex);
                Vertex nextVertex = null;
                
                if (currentVertex.isPseudoEndVertex()) {
                    Vertex candidateVertex = adjacentVertexListIterator.next();
                    nextVertex = candidateVertex;
                } else {
                    while (adjacentVertexListIterator.hasNext()) {
                        Vertex candidateVertex = adjacentVertexListIterator.next();

                        if (candidateVertex instanceof VertexRefinedByFeatureExpression) {
                            FeatureExpression featureExpression = ((VertexRefinedByFeatureExpression) candidateVertex)
                                    .getFeatureExpression();
                            if (!featureExpression.evaluate()) {
                                continue;
                            }
                        }

                        if (!candidateVertex.isPseudoStartVertex() && !candidateVertex.isPseudoEndVertex()) {
                            if (!compatibilityChecker.isFeatureTruthValueMapContainsKey(((VertexRefinedByFeatureExpression) candidateVertex)
                                    .getFeatureExpression().getFeature())) {
                                compatibilityChecker.fillFeatureTruthValueMap(candidateVertex);
                            }
                        }
                        if (candidateVertex.isPseudoEndVertex()) {
                            nextVertex = candidateVertex;
                            break;
                        } else if (compatibilityChecker.isCompatible(candidateVertex)) {
                            nextVertex = candidateVertex;
                            break;
                        }
                    }
                }
                
                // Remove the edge we just traversed (Destructive step)
                adjacentVertexListIterator.remove();
                
                if (nextVertex != null) {
                    currentVertex = nextVertex;
                } else {
                    // Backtrack
                    currentVertex = ((Stack<Vertex>) vertexStack).pop();
                }
            } else {
                // No neighbors left, add to cycle
                eulerCycle.add(currentVertex);
                currentVertex = ((Stack<Vertex>) vertexStack).pop();
            }
        }
        
        // Explicitly nullify the local graph reference to help GC
        localESGFx = null; 
    }

    // --- Reset Method (CRITICAL for Reuse) ---
    public void reset() {
        if (this.vertexStack != null) this.vertexStack.clear();
        if (this.eulerCycle != null) this.eulerCycle.clear();
        
        // We don't need to clear adjacencyMap because we overwrite the reference 
        // in generateEulerCycle. But setting it to null is good practice.
        this.adjacencyMap = null; 
        
        if (this.compatibilityChecker != null) this.compatibilityChecker.clearFeatureTruthValueMap();
    }
    
    // --- Getters / Setters ---
    public List<Vertex> getVertexStack() { return vertexStack; }
    public void setVertexStack(List<Vertex> vertexStack) { this.vertexStack = vertexStack; }
    public List<Vertex> getEulerCycle() { return eulerCycle; }
    public void setEulerCycle(List<Vertex> eulerCycle) { this.eulerCycle = eulerCycle; }
    public Map<Vertex, List<Vertex>> getAdjacencyMap() { return adjacencyMap; }
}