package tr.edu.iyte.esgfx.model;

import java.util.Map;

import java.util.LinkedHashMap;

import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.featureexpression.Conjunction;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.Negation;
import tr.edu.iyte.esgfx.model.featuremodel.Feature;

public class CompatibilityChecker {

	private Map<Feature, Boolean> featureTruthValueMap;

	public CompatibilityChecker() {
		featureTruthValueMap = new LinkedHashMap<>();

	}

	public void clearFeatureTruthValueMap() {
		featureTruthValueMap.clear();
	}

	public boolean isFeatureTruthValueMapContainsKey(Feature feature) {
		return featureTruthValueMap.containsKey(feature);
	}

	public void fillFeatureTruthValueMap(Vertex currentVertex) {
//		System.out.println("fillFeatureTruthValueMap METHOD START");
		VertexRefinedByFeatureExpression vertexRefinedByFeatureExpression = (VertexRefinedByFeatureExpression) currentVertex;
		FeatureExpression featureExpression = vertexRefinedByFeatureExpression.getFeatureExpression();
//		Feature feature = featureExpression.getFeature();
//		System.out.println("fillFeatureTruthValueMap currentVertex " + currentVertex.toString());
//		System.out.println("featureTruthValueMap.containsKey(feature) " + featureTruthValueMap.containsKey(feature));

		if (featureExpression instanceof Conjunction) {
			for (FeatureExpression operand : ((Conjunction) featureExpression).getOperands()) {

//				System.out.println("operand " + operand.toString());
				fillFeatureTruthValueMap(operand);

			}
		} else {
			fillFeatureTruthValueMap(featureExpression);
		}

//		System.out.println(featureTruthValueMap);
//		System.out.println("fillFeatureTruthValueMap METHOD FINISH");
	}

	public void fillFeatureTruthValueMap(FeatureExpression featureExpression) {
		Feature feature = featureExpression.getFeature();
		if (featureTruthValueMap.containsKey(feature)) {
			return;
		} else {
			if (featureExpression instanceof Negation) {
				featureTruthValueMap.put(feature, false);
			} else {
				featureTruthValueMap.put(feature, true);
			}
		}

	}

	public boolean isCompatible(Vertex candidateVertex) {
//		System.out.println("isCompatible METHOD START");

		VertexRefinedByFeatureExpression vertexRefinedByFeatureExpression = (VertexRefinedByFeatureExpression) candidateVertex;
//		System.out.println("vertexRefinedByFeatureExpression " + vertexRefinedByFeatureExpression.toString());
		FeatureExpression featureExpression = vertexRefinedByFeatureExpression.getFeatureExpression();

//		System.out.println("candidateVertex " + candidateVertex.toString());
		boolean result = false;

		if (featureTruthValueMap.isEmpty()) {
			result = true;
		} else {
			if (featureExpression instanceof Conjunction) {
				return isConjunctionCompatible((Conjunction) featureExpression);

			} else {
				Feature feature = featureExpression.getFeature();
				if (((featureExpression instanceof Negation) && (featureTruthValueMap.get(feature) == false))
						|| (!(featureExpression instanceof Negation) && (featureTruthValueMap.get(feature) == true))) {
					result = true;
				}
			}
		}
//		System.out.println("result is " + result);
//		System.out.println("isCompatible METHOD FINISH");
		return result;
	}

	public boolean isConjunctionCompatible(Conjunction conjunction) {
		boolean result = true;
		for (FeatureExpression operand : conjunction.getOperands()) {
			Feature feature = operand.getFeature();
//			System.out.println("isConjunctionCompatible operand " + operand.toString());
//
//			System.out.println(
//					"operand.getFeature() " + operand.getFeature().getName() + " " + featureTruthValueMap.get(feature));
			result = result && ((operand instanceof Negation) && (featureTruthValueMap.get(feature) == false))
					|| (!(operand instanceof Negation) && (featureTruthValueMap.get(feature) == true));

		}
		return result;
	}

}
