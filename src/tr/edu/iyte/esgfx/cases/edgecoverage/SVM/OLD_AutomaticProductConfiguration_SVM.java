package tr.edu.iyte.esgfx.cases.edgecoverage.SVM;

import java.util.Map.Entry;

import tr.edu.iyte.esgfx.model.featureexpression.Conjunction;
import tr.edu.iyte.esgfx.model.featureexpression.Disjunction;
import tr.edu.iyte.esgfx.model.featureexpression.ExclusiveDisjunction;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.Implication;
import tr.edu.iyte.esgfx.model.featureexpression.Negation;

import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;
import org.sat4j.core.VecInt;
import org.sat4j.tools.ModelIterator;
import java.util.*;

public class OLD_AutomaticProductConfiguration_SVM {

	public static Set<Map<String, FeatureExpression>> getAllProductConfigurations(Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {

		Set<Map<String, FeatureExpression>> setOfFeatureExpressionMaps = new LinkedHashSet<>();
		
		Map<FeatureExpression, Integer> variableMap = new HashMap<>();

//		variableMap to represent each FeatureExpression with a unique integer ID
		int ID = 0;
		for (Entry<String, FeatureExpression> entry : featureExpressionMapFromFeatureModel.entrySet()) {
			String featureName = entry.getKey();
			FeatureExpression featureExpression = entry.getValue();

			if (!featureName.equals("!f")) {
				variableMap.put(featureExpression, ++ID);
			}
//			System.out.print(featureName + " - " + featureExpression.evaluate() + "\n");
		}

		int numberOfProducts = 0;
		try {

//			FeatureModel is converted to CNF and added to the solver
			ISolver solver = new ModelIterator(SolverFactory.newDefault());
			solver.addClause(new VecInt(
					new int[] { getIDByFeatureName("s", variableMap), getIDByFeatureName("t", variableMap) })); // s OR
																												// t
			solver.addClause(
					new VecInt(new int[] { getIDByFeatureName("s", variableMap), getIDByFeatureName("t", variableMap),
							getIDByFeatureName("f", variableMap), getIDByFeatureName("c", variableMap) })); // (s OR t)
																											// OR f OR c
			while (solver.isSatisfiable()) {
				int[] model = solver.model();
				for (int i : model) {
					for (Map.Entry<FeatureExpression, Integer> entry : variableMap.entrySet()) {
						if (Math.abs(i) == entry.getValue()) {
							entry.getKey().setTruthValue(i > 0);
						}
					}
				}

				Map<String, FeatureExpression> productConfiguration = new LinkedHashMap<>();
				for (Entry<String, FeatureExpression> entry : featureExpressionMapFromFeatureModel.entrySet()) {
					String featureName = entry.getKey();
//					System.out.println("Feature Name: " + featureName);
					FeatureExpression featureExpression = entry.getValue();
					
					if(!featureName.equals("!f"))
							productConfiguration.put(featureName,cloneFeatureExpression(featureExpression, featureExpression.evaluate()));
//					System.out.print(featureName + " - " + featureExpression.evaluate() + "\n");
				}
				setOfFeatureExpressionMaps.add(productConfiguration);

//				System.out.println("-----");
				numberOfProducts++;
			}
		} catch (ContradictionException | TimeoutException e) {
			e.printStackTrace();
		}

//		System.out.println("Number of products: " + numberOfProducts);
//		for (Map<String, FeatureExpression> pc : setOfFeatureExpressionMaps) {
//			System.out.println("Product Configuration: ");
//			for (Entry<String, FeatureExpression> e : pc.entrySet()) {
//				System.out.print(e.getKey() + " - " + e.getValue().evaluate() + "\n");
//			}
//			System.out.println("-----");
//		}

		return setOfFeatureExpressionMaps;

	}

	private static FeatureExpression cloneFeatureExpression(FeatureExpression featureExpression, boolean truthValue) {

		if (featureExpression instanceof Conjunction) {
//			System.out.println("Conjunction");
			FeatureExpression conjunction = new Conjunction();
			for (FeatureExpression operand : ((Conjunction) featureExpression).getOperands()) {
				((Conjunction) conjunction).addOperand(operand);
			}
			conjunction.setTruthValue(truthValue);
			return conjunction;
		} else if (featureExpression instanceof Disjunction) {
//			System.out.println("Disjunction");
			FeatureExpression disjunction = new Disjunction();
			for (FeatureExpression operand : ((Disjunction) featureExpression).getOperands()) {
				((Disjunction) disjunction).addOperand(operand);
			}
			disjunction.setTruthValue(truthValue);
			return disjunction;
		} else if (featureExpression instanceof ExclusiveDisjunction) {
//			System.out.println("ExclusiveDisjunction");
			FeatureExpression exclusiveDisjunction = new ExclusiveDisjunction();
			for (FeatureExpression operand : ((ExclusiveDisjunction) featureExpression).getOperands()) {
				((ExclusiveDisjunction) exclusiveDisjunction).addOperand(operand);
			}
			exclusiveDisjunction.setTruthValue(truthValue);
			return exclusiveDisjunction;
		} else if (featureExpression instanceof Implication) {
//			System.out.println("Implication");
			FeatureExpression implication = new Implication(((Implication) featureExpression).getLeftOperand(),
					((Implication) featureExpression).getRightOperand());
			implication.setTruthValue(truthValue);
			return implication;
		} else if (featureExpression instanceof Negation) {
//			System.out.println("Negation");
			FeatureExpression negation = new Negation(featureExpression);
			negation.setTruthValue(truthValue);
		}
//		System.out.println("FeatureExpression");
		return new FeatureExpression(featureExpression.getFeature(), truthValue);
	}

	private static Integer getIDByFeatureName(String featureName, Map<FeatureExpression, Integer> variableMap) {
		for (Entry<FeatureExpression, Integer> entry : variableMap.entrySet()) {
			if (entry.getKey().getFeature().getName().equals(featureName)) {
				return entry.getValue();
			}
		}
		return null;
	}

	private static FeatureExpression getFeatureExpressionByFeatureName(String featureName,
			Map<FeatureExpression, Integer> variableMap) {
		for (Entry<FeatureExpression, Integer> entry : variableMap.entrySet()) {
			if (entry.getKey().getFeature().getName().equals(featureName)) {
				return entry.getKey();
			}
		}
		return null;
	}
	
	public static void matchFeatureExpressions(Map<String, FeatureExpression> featureExpressionMapFromFeatureModel,
			Map<String, FeatureExpression> productConfigurationMap) {
		for (Entry<String, FeatureExpression> entry : productConfigurationMap.entrySet()) {
			FeatureExpression featureExpressionInProductConfiguration = entry.getValue();
			String featureName = entry.getKey();
			featureExpressionMapFromFeatureModel.get(featureName)
					.setTruthValue(featureExpressionInProductConfiguration.evaluate());
		}
	}

}
