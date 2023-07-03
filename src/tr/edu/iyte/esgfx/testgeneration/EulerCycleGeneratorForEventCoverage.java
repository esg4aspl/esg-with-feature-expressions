package tr.edu.iyte.esgfx.testgeneration;

import java.util.Map;
import java.util.Set;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esg.conversion.ESGToJgraphConverter;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.Negation;
import tr.edu.iyte.esgfx.model.featuremodel.Feature;
import tr.edu.iyte.esgfx.model.ESGFx;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.ArrayList;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;

public class EulerCycleGeneratorForEventCoverage {

	private Set<Vertex> toCover;
	private Map<Feature, Boolean> featureTruthValueMap;
	private List<Vertex> vertexStack;
	private List<Vertex> eulerCycle;
	private Map<Vertex, List<Vertex>> adjacencyMap;
	private Map<String, FeatureExpression> featureExpressionMap;

	public EulerCycleGeneratorForEventCoverage(Map<String, FeatureExpression> featureExpressionMap) {
		this.toCover = new LinkedHashSet<>();
		this.featureTruthValueMap = new LinkedHashMap<>();
		this.vertexStack = new Stack<>();
		this.eulerCycle = new LinkedList<>();
		this.adjacencyMap = new LinkedHashMap<>();
		this.featureExpressionMap = featureExpressionMap;

	}

	private boolean containsOnlyOnePseudoEnd(List<Vertex> adjacentVertexList) {

		// System.out.println("containsOnlyOnePseudoEnd");
		if (adjacentVertexList.size() == 1) {
			if (adjacentVertexList.get(0).isPseudoEndVertex()) {
//				System.out.println("containsOnlyOnePseudoEnd" + "TRUE");
				return true;
			} else
				return false;
		} else
			return false;

	}

	private void findToCover(ESG ESGFx) {
		List<Vertex> vertexList = ESGFx.getRealVertexList();

//		System.out.println("findToCover");
		for (Vertex vertex : vertexList) {
			VertexRefinedByFeatureExpression vertexRefinedByFeatureExpression = (VertexRefinedByFeatureExpression) vertex;
//			System.out.println(vertexRefinedByFeatureExpression.getEvent().getName() + " "	+ vertexRefinedByFeatureExpression.getFeatureExpression().getFeature().getName() + " " + vertexRefinedByFeatureExpression.getFeatureExpression().evaluate());

			if (vertexRefinedByFeatureExpression.getFeatureExpression().evaluate()) {
				toCover.add(vertexRefinedByFeatureExpression);

			}

		}
	}

	private void findAdjMap() {

		Iterator<Vertex> keySetIterator = adjacencyMap.keySet().iterator();

		while (keySetIterator.hasNext()) {
			Vertex key = keySetIterator.next();
			Iterator<Vertex> adjacentVertexListIterator = adjacencyMap.get(key).iterator();
			while (adjacentVertexListIterator.hasNext()) {
				Vertex adjVertex = adjacentVertexListIterator.next();
				// System.out.println("adjVertex " + adjVertex);
				if (!toCover.contains(adjVertex) && !adjVertex.isPseudoStartVertex()
						&& !adjVertex.isPseudoEndVertex()) {
//					System.out.println("adjVertex " + adjVertex + " is removed");
					adjacentVertexListIterator.remove();
				}
			}
		}
		/*
		 * while (keySetIterator.hasNext()) { Vertex key = keySetIterator.next();
		 * System.out.print("Vertex " + key + " -> "); Iterator<Vertex>
		 * adjacentVertexListIterator = adjacencyMap.get(key).iterator(); while
		 * (adjacentVertexListIterator.hasNext()) {
		 * System.out.print(adjacentVertexListIterator.next().getEvent().getName()); }
		 * System.out.println(); }
		 */
	}

	public void generateEulerCycle(ESG stronglyConnectedBalancedESGFx) {
		ESG ESGFx = new ESGFx(stronglyConnectedBalancedESGFx);
		findToCover(ESGFx);
//		System.out.println("toCover " + toCover);
		adjacencyMap.putAll(((ESGFx) ESGFx).getAdjacencyMap());
		findAdjMap();
		Vertex currentVertex = ESGFx.getPseudoStartVertex();
		((Stack<Vertex>) vertexStack).push(currentVertex);
		boolean generationStarted = false;

		while (!vertexStack.isEmpty()) {
//			System.out.println("currentVertex " + currentVertex);
			List<Vertex> adjacentVertexList = adjacencyMap.get(currentVertex);
			Iterator<Vertex> adjacentVertexListIterator = adjacentVertexList.iterator();

//			System.out.println("adjacentVertexList " + adjacentVertexList);
//			System.out.println("toCover " + toCover);

			if (currentVertex.isPseudoStartVertex()) {
//				System.out.println("currentVertex isPseudoStartVertex " + currentVertex.isPseudoStartVertex());
				featureTruthValueMap.clear();
			} else if (!currentVertex.isPseudoStartVertex() && !currentVertex.isPseudoEndVertex()) {
				if (!featureTruthValueMap.containsKey(
						((VertexRefinedByFeatureExpression) currentVertex).getFeatureExpression().getFeature())) {
					fillFeatureTruthValueMap(currentVertex);
				}
			}

			if (!adjacentVertexList.isEmpty() && !toCover.isEmpty()) {
				((Stack<Vertex>) vertexStack).push(currentVertex);
				Vertex nextVertex = null;
				if (currentVertex.isPseudoEndVertex()) {
//					System.out.println("currentVertex isPseudoEndVertex " + currentVertex.isPseudoEndVertex());
//					System.out.println(adjacentVertexList.size());
					Vertex candidateVertex = adjacentVertexListIterator.next();
//					System.out.println(candidateVertex.toString());
					nextVertex = candidateVertex;
					// break;
				} else {
					while (adjacentVertexListIterator.hasNext()) {
						Vertex candidateVertex = adjacentVertexListIterator.next();
//						System.out.println("candidateVertex " + candidateVertex.toString());
						if (!candidateVertex.isPseudoStartVertex() && !candidateVertex.isPseudoEndVertex()) {
							if (!featureTruthValueMap.containsKey(((VertexRefinedByFeatureExpression) candidateVertex)
									.getFeatureExpression().getFeature())) {
								fillFeatureTruthValueMap(candidateVertex);
							}
						}

						if (candidateVertex.isPseudoEndVertex()) {
							nextVertex = candidateVertex;
							break;
						} else if (isCompatible(candidateVertex)) {
							if (allOfAdjacentVerticesCoveredBefore(adjacentVertexList)) {
//								System.out.println("allOfAdjacentVerticesCoveredBefore".toUpperCase());
								nextVertex = findPrior(ESGFx, adjacentVertexList);
								break;
							} else if (anyOfAdjacentVerticesCoveredBefore(adjacentVertexList)) {
//								System.out.println("anyOfAdjacentVerticesCoveredBefore".toUpperCase());
								nextVertex = candidateVertex;
								break;
							} else {
								// List<Vertex> notCoveredList =
								// adjacentVerticesNOTCoveredBefore(adjacentVertexList);
//								System.out.println("someOfAdjacentVerticesCoveredBefore".toUpperCase());
								List<Vertex> verticesNOTcoveredYet = adjacentVerticesNOTCoveredBefore(
										adjacentVertexList);
								nextVertex = verticesNOTcoveredYet.get(0);
								break;
							}
						}
//						break;
					}
				}
				adjacentVertexListIterator.remove();
				if (!currentVertex.isPseudoStartVertex() && !currentVertex.isPseudoEndVertex()) {
					toCover.remove(currentVertex);
				}
				currentVertex = nextVertex;

				// continue;
			} else if (!adjacentVertexList.isEmpty() && toCover.isEmpty()
//					&& containsOnlyOnePseudoEnd(adjacentVertexList)
					&& (!currentVertex.isPseudoStartVertex() && !currentVertex.isPseudoEndVertex())
					&& !generationStarted) {
//				System.out.println("BEKLENEN");
				((Stack<Vertex>) vertexStack).push(currentVertex);
				currentVertex = adjacentVertexListIterator.next();
				adjacentVertexListIterator.remove();
			} else {
//				System.out.println("EULER CYCLE");
				generationStarted = true;

				eulerCycle.add(currentVertex);
				currentVertex = ((Stack<Vertex>) vertexStack).pop();
			}
		}

//		System.out.println(eulerCycle);

	}

	// toCover contains any
	private boolean allOfAdjacentVerticesCoveredBefore(List<Vertex> adjacentVertexList) {
		for (Vertex vertex : adjacentVertexList) {
			if (toCover.contains(vertex)) {
				return false;
			}
		}
		return true;
	}

	// toCover contains all
	private boolean anyOfAdjacentVerticesCoveredBefore(List<Vertex> adjacentVertexList) {

		for (Vertex vertex : adjacentVertexList) {
			if (!toCover.contains(vertex)) {
				return false;
			}
		}
		return true;
	}

	private List<Vertex> adjacentVerticesNOTCoveredBefore(List<Vertex> adjacentVertexList) {
		List<Vertex> notCoveredList = new ArrayList<>();

		for (Vertex vertex : adjacentVertexList) {
			if (toCover.contains(vertex)) {
				notCoveredList.add(vertex);
			}
		}

		return notCoveredList;
	}

	private Vertex findPrior(ESG ESGFx, List<Vertex> adjacentVertexList) {

//		System.out.println("FINDPRIOR METHOD START");
//		System.out.println("adjacentVertexList " + adjacentVertexList);
		ESGToJgraphConverter ESGToJgraphConverter = new ESGToJgraphConverter();
		Graph<Vertex, Edge> jGraph = ESGToJgraphConverter.buildJGraphFromESG(ESGFx);

		FloydWarshallShortestPaths<Vertex, Edge> floydWarshallShortestPaths = new FloydWarshallShortestPaths<Vertex, Edge>(
				jGraph);

		GraphPath<Vertex, Edge> foundPath = null;
		int minLength = Integer.MAX_VALUE;
		for (Vertex source : adjacentVertexList) {
//			System.out.println("source " + source);
			for (Vertex target : toCover) {
//				System.out.println("target " + target);
//				System.out.println("Path between " + source.toString() + " " + target.toString());
				GraphPath<Vertex, Edge> path = floydWarshallShortestPaths.getPath(source, target);
//				System.out.println("Path length " + path.getLength());
				if (path.getLength() < minLength) {
					foundPath = path;
					minLength = path.getLength();
				}
			}
		}

//		System.out.println("FINDPRIOR METHOD FINISH");
		return foundPath.getStartVertex();
	}

	private void fillFeatureTruthValueMap(Vertex currentVertex) {
//		System.out.println("fillFeatureTruthValueMap METHOD START");
		VertexRefinedByFeatureExpression vertexRefinedByFeatureExpression = (VertexRefinedByFeatureExpression) currentVertex;
		FeatureExpression featureExpression = vertexRefinedByFeatureExpression.getFeatureExpression();
		Feature feature = featureExpression.getFeature();
//		System.out.println(currentVertex.toString());
//		System.out.println("featureTruthValueMap.containsKey(feature) " + featureTruthValueMap.containsKey(feature));
		if (featureTruthValueMap.containsKey(feature)) {
//			System.out.println("fillFeatureTruthValueMap METHOD FINISH");
			return;
		} else {
			if (featureExpression instanceof Negation) {
				featureTruthValueMap.put(feature, false);
			} else {
				featureTruthValueMap.put(feature, true);
			}
		}

//		System.out.println(featureTruthValueMap);
//		System.out.println("fillFeatureTruthValueMap METHOD FINISH");
	}

	private boolean isCompatible(Vertex candidateVertex) {
//		System.out.println("isCompatible METHOD START");

		VertexRefinedByFeatureExpression vertexRefinedByFeatureExpression = (VertexRefinedByFeatureExpression) candidateVertex;
		FeatureExpression featureExpression = vertexRefinedByFeatureExpression.getFeatureExpression();
		Feature feature = featureExpression.getFeature();
//		System.out.println(candidateVertex.toString());
		boolean result = false;

		if (featureTruthValueMap.isEmpty()) {
			result = true;
		} else {
			if (((featureExpression instanceof Negation) && (featureTruthValueMap.get(feature) == false))
					|| (!(featureExpression instanceof Negation) && (featureTruthValueMap.get(feature) == true))) {
				result = true;
			}
		}
//		System.out.println("result is " + result);
//		System.out.println("isCompatible METHOD FINISH");
		return result;
	}

	public Set<Vertex> getToCover() {
		return toCover;
	}

	public void setToCover(Set<Vertex> toCover) {
		this.toCover = toCover;
	}

	public Map<Feature, Boolean> getFeatureTruthValueMap() {
		return featureTruthValueMap;
	}

	public void setFeatureTruthValueMap(Map<Feature, Boolean> featureExpressionTruthValueMap) {
		this.featureTruthValueMap = featureExpressionTruthValueMap;
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
