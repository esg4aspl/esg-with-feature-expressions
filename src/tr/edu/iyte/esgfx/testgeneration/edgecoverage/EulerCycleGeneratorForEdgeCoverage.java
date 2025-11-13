package tr.edu.iyte.esgfx.testgeneration.edgecoverage;

import java.util.Map;

import tr.edu.iyte.esg.model.ESG;

import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.CompatibilityChecker;

import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

import tr.edu.iyte.esgfx.model.ESGFx;

import java.util.Iterator;
import java.util.LinkedHashMap;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class EulerCycleGeneratorForEdgeCoverage {


	private List<Vertex> vertexStack;
	private List<Vertex> eulerCycle;
	private Map<Vertex, List<Vertex>> adjacencyMap;

	// private Map<String, FeatureExpression> featureExpressionMap;

	public EulerCycleGeneratorForEdgeCoverage() {

		this.vertexStack = new Stack<>();
		this.eulerCycle = new LinkedList<>();
		this.adjacencyMap = new LinkedHashMap<>();
		// this.featureExpressionMap = featureExpressionMap;

	}

	public void generateEulerCycle(ESG stronglyConnectedBalancedESGFx) {
		ESG ESGFx = new ESGFx(stronglyConnectedBalancedESGFx);
		CompatibilityChecker compatibilityChecker = new CompatibilityChecker();
		adjacencyMap.putAll(((ESGFx) ESGFx).getAdjacencyMap());
		Vertex currentVertex = ESGFx.getPseudoStartVertex();
		((Stack<Vertex>) vertexStack).push(currentVertex);

		while (!vertexStack.isEmpty()) {
//        System.out.println("currentVertex " + currentVertex);
			List<Vertex> adjacentVertexList = adjacencyMap.get(currentVertex);
			Iterator<Vertex> adjacentVertexListIterator = adjacentVertexList.iterator();

//        System.out.println("adjacentVertexList " + adjacentVertexList);

			if (currentVertex.isPseudoStartVertex()) {
//            System.out.println("currentVertex isPseudoStartVertex " + currentVertex.isPseudoStartVertex());
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
//								System.out.println("candidateVertex " + candidateVertex.toString());
								compatibilityChecker.fillFeatureTruthValueMap(candidateVertex);
							}
						}
						if (candidateVertex.isPseudoEndVertex()) {
							nextVertex = candidateVertex;
							break;
						} else if (compatibilityChecker.isCompatible(candidateVertex)) {
//							System.out.println("nextVertex " + candidateVertex.toString());

							nextVertex = candidateVertex;

							break;
						}
					}
				}
				adjacentVertexListIterator.remove();
				if (nextVertex != null) {
					currentVertex = nextVertex;
				} else {
					currentVertex = ((Stack<Vertex>) vertexStack).pop();
				}
			} else {
				eulerCycle.add(currentVertex);
				currentVertex = ((Stack<Vertex>) vertexStack).pop();
			}
		}
	}

	public List<Vertex> getVertexStack() {
		return vertexStack;
	}

	public void setVertexStack(List<Vertex> vertexStack) {
		this.vertexStack = vertexStack;
	}

	public List<Vertex> getEulerCycle() {
		return eulerCycle;
	}

	public void setEulerCycle(List<Vertex> eulerCycle) {
		this.eulerCycle = eulerCycle;
	}

	public Map<Vertex, List<Vertex>> getAdjacencyMap() {
		return adjacencyMap;
	}
}
