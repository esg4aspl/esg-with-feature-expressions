package tr.edu.iyte.esgfx.testgeneration.edgecoverage;

import java.util.Map;

import tr.edu.iyte.esg.model.ESG;

import tr.edu.iyte.esg.model.Vertex;

import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.Negation;
import tr.edu.iyte.esgfx.model.featuremodel.Feature;
import tr.edu.iyte.esgfx.model.ESGFx;

import java.util.Iterator;
import java.util.LinkedHashMap;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class EulerCycleGeneratorForEdgeCoverage {

	private Map<Feature, Boolean> featureTruthValueMap;
	private List<Vertex> vertexStack;
	private List<Vertex> eulerCycle;
	private Map<Vertex, List<Vertex>> adjacencyMap;

	// private Map<String, FeatureExpression> featureExpressionMap;

	public EulerCycleGeneratorForEdgeCoverage() {

		this.featureTruthValueMap = new LinkedHashMap<>();
		this.vertexStack = new Stack<>();
		this.eulerCycle = new LinkedList<>();
		this.adjacencyMap = new LinkedHashMap<>();
		// this.featureExpressionMap = featureExpressionMap;

	}



public void generateEulerCycle(ESG stronglyConnectedBalancedESGFx) {
    ESG ESGFx = new ESGFx(stronglyConnectedBalancedESGFx);
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
            featureTruthValueMap.clear();
        } else if (!currentVertex.isPseudoStartVertex() && !currentVertex.isPseudoEndVertex()) {
            if (!featureTruthValueMap.containsKey(
                    ((VertexRefinedByFeatureExpression) currentVertex).getFeatureExpression().getFeature())) {
                fillFeatureTruthValueMap(currentVertex);
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
                        FeatureExpression featureExpression = ((VertexRefinedByFeatureExpression) candidateVertex).getFeatureExpression();
                        if (!featureExpression.evaluate()) {
                            continue;
                        }
                    }

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
//                        System.out.println("nextVertex " + candidateVertex.toString());

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
