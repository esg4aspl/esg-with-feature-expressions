package tr.edu.iyte.esgfx.cases.edgecoverage.BankAccountv2;

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

public class OLD_AutomaticProductConfiguration_BAv2 {

	public static Set<Map<String, FeatureExpression>> getAllProductConfigurations(
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {

		Set<Map<String, FeatureExpression>> setOfFeatureExpressionMaps = new LinkedHashSet<>();

		Map<FeatureExpression, Integer> variableMap = new HashMap<>();
		// variableMap to represent each FeatureExpression with a unique integer ID
		int ID = 0;
		for (Entry<String, FeatureExpression> entry : featureExpressionMapFromFeatureModel.entrySet()) {
			String featureName = entry.getKey();

			FeatureExpression featureExpression = entry.getValue();
			variableMap.put(featureExpression, ++ID);
		}

//		for (Entry<FeatureExpression, Integer> entry : variableMap.entrySet()) {
//			System.out.print(entry.getKey() + " - " + entry.getValue() + "\n");
//		}

		int numberOfProducts = 0;
		try {
			// FeatureModel is converted to CNF and added to the solver
			ISolver solver = new ModelIterator(SolverFactory.newDefault());
			
	        // Ensure b, w, and d are true 
	        solver.addClause(new VecInt(new int[] { getIDByFeatureName("b", variableMap), getIDByFeatureName("w", variableMap), getIDByFeatureName("d", variableMap) })); // b 


			// Ensure exactly one of tl, eu, us is true
			solver.addClause(new VecInt(new int[] { getIDByFeatureName("tl", variableMap),
					-getIDByFeatureName("eu", variableMap), -getIDByFeatureName("us", variableMap) })); // tl XOR eu XOR
																										// us
			solver.addClause(new VecInt(new int[] { -getIDByFeatureName("tl", variableMap),
					getIDByFeatureName("eu", variableMap), -getIDByFeatureName("us", variableMap) })); // tl XOR eu XOR
																										// us
			solver.addClause(new VecInt(new int[] { -getIDByFeatureName("tl", variableMap),
					-getIDByFeatureName("eu", variableMap), getIDByFeatureName("us", variableMap) })); // tl XOR eu XOR
																										// us

			solver.addClause(new VecInt(new int[] { -getIDByFeatureName("c", variableMap),
					-getIDByFeatureName("o", variableMap) })); //c  XOR o 
			//(since they are optional, they could be false at the same time, they cannot be true at the same time)
			
			
			
			solver.addClause(new VecInt(new int[] { getIDByFeatureName("up", variableMap),
					getIDByFeatureName("t", variableMap), getIDByFeatureName("cd", variableMap),
					getIDByFeatureName("cw", variableMap), getIDByFeatureName("i", variableMap),
					getIDByFeatureName("ie", variableMap), getIDByFeatureName("dl", variableMap) })); // u OR t OR (cd
																										// OR cw) OR  i
																										// OR ie OR dl

			// Add the new clause (ie implies i) which is equivalent to (!ie OR i)
			solver.addClause(new VecInt(
					new int[] { -getIDByFeatureName("ie", variableMap), getIDByFeatureName("i", variableMap) }));

			// Add the new clause (dl implies (w AND cw)) which is equivalent to (!dl OR w)
			// AND (!dl OR cw)
			solver.addClause(new VecInt(
					new int[] { -getIDByFeatureName("dl", variableMap), getIDByFeatureName("cw", variableMap) }));

			// Add the new clause (o implies (cw AND dl)) which is equivalent to (!o OR cw)
			// AND (!o OR dl)
			solver.addClause(new VecInt(
					new int[] { -getIDByFeatureName("o", variableMap), getIDByFeatureName("cw", variableMap) }));
			solver.addClause(new VecInt(
					new int[] { -getIDByFeatureName("o", variableMap), getIDByFeatureName("dl", variableMap) }));

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
					FeatureExpression featureExpression = entry.getValue();
					productConfiguration.put(featureName,
							cloneFeatureExpression(featureExpression, featureExpression.evaluate()));
				}
				setOfFeatureExpressionMaps.add(productConfiguration);

				numberOfProducts++;
			}
		} catch (ContradictionException | TimeoutException e) {
			e.printStackTrace();
		}

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
//			System.out.println("Feature name: " + featureName + " - " + featureExpressionInProductConfiguration.evaluate());
//			if (featureName.equals("b") || featureName.equals("d") || featureName.equals("w")) {
//				featureExpressionMapFromFeatureModel.get(featureName)
//				.setTruthValue(true);
//				System.out.println("HERE");
//			}else
			featureExpressionMapFromFeatureModel.get(featureName)
					.setTruthValue(featureExpressionInProductConfiguration.evaluate());
		}
	}

}
