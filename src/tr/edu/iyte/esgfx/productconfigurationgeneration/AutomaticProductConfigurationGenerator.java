package tr.edu.iyte.esgfx.productconfigurationgeneration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.List;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

import tr.edu.iyte.esgfx.model.featureexpression.Conjunction;
import tr.edu.iyte.esgfx.model.featureexpression.Disjunction;
import tr.edu.iyte.esgfx.model.featureexpression.ExclusiveDisjunction;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.Negation;

import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;

import tr.edu.iyte.esgfx.model.featureexpression.Implication;

public class AutomaticProductConfigurationGenerator {

	public Set<Map<String, FeatureExpression>> getAllProductConfigurations(FeatureModel featureModel,
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel)
			throws ContradictionException, TimeoutException {

		Set<Map<String, FeatureExpression>> setOfFeatureExpressionMaps = new LinkedHashSet<>();
		SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();

		List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);
		printFeatureExpressionList(featureExpressionList);
		System.out.println("-----------------------------------------------");

		ISolver solver = new ModelIterator(SolverFactory.newDefault());
		satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel,
				featureExpressionList);

		System.out.println("-----------------------------------------------");

		int productID = 0;
		while (solver.isSatisfiable()) {
			productID++;
//			String productName = "P";
//			if (productID < 10)
//				productName = "P0";
//			
//			String ESGFxName = productName + Integer.toString(productID);
//
//			String productConfiguration = ESGFxName + ": <";
//			int numberOfFeatures = 0;
			
			int[] model = solver.model();
			for (int i = 0; i < model.length; i++) {
				FeatureExpression featureExpression = featureExpressionList.get(i);
				String featureName = featureExpression.getFeature().getName();
				if (model[i] > 0) {
//					System.out.println(featureName + " = true");
					featureExpression.setTruthValue(true);
//					productConfiguration += featureName + ", ";
//					numberOfFeatures++;
				} else {
//					System.out.println(featureName + " = false");
					featureExpression.setTruthValue(false);
				}
			}
//			productConfiguration = productConfiguration.substring(0, productConfiguration.length() - 2);
//			productConfiguration += ">:" + numberOfFeatures  + " features";
//			System.out.println(productConfiguration);
//			System.out.println("-----------------------------------");
			
			// Add a clause to block the current model to find the next one
			VecInt blockingClause = new VecInt();
			for (int i = 0; i < model.length; i++) {
				blockingClause.push(-model[i]);
			}
			solver.addClause(blockingClause);

		}

		System.out.println("Number of Products: " + productID);
		return setOfFeatureExpressionMaps;

	}

//	private FeatureExpression cloneFeatureExpression(FeatureExpression featureExpression, boolean truthValue) {
//
//		if (featureExpression instanceof Conjunction) {
////				System.out.println("Conjunction");
//			FeatureExpression conjunction = new Conjunction();
//			for (FeatureExpression operand : ((Conjunction) featureExpression).getOperands()) {
//				((Conjunction) conjunction).addOperand(operand);
//			}
//			conjunction.setTruthValue(truthValue);
//			return conjunction;
//		} else if (featureExpression instanceof Disjunction) {
////				System.out.println("Disjunction");
//			FeatureExpression disjunction = new Disjunction();
//			for (FeatureExpression operand : ((Disjunction) featureExpression).getOperands()) {
//				((Disjunction) disjunction).addOperand(operand);
//			}
//			disjunction.setTruthValue(truthValue);
//			return disjunction;
//		} else if (featureExpression instanceof ExclusiveDisjunction) {
////				System.out.println("ExclusiveDisjunction");
//			FeatureExpression exclusiveDisjunction = new ExclusiveDisjunction();
//			for (FeatureExpression operand : ((ExclusiveDisjunction) featureExpression).getOperands()) {
//				((ExclusiveDisjunction) exclusiveDisjunction).addOperand(operand);
//			}
//			exclusiveDisjunction.setTruthValue(truthValue);
//			return exclusiveDisjunction;
//		} else if (featureExpression instanceof Implication) {
////				System.out.println("Implication");
//			FeatureExpression implication = new Implication(((Implication) featureExpression).getLeftOperand(),
//					((Implication) featureExpression).getRightOperand());
//			implication.setTruthValue(truthValue);
//			return implication;
//		} else if (featureExpression instanceof Negation) {
////				System.out.println("Negation");
//			FeatureExpression negation = new Negation(featureExpression);
//			negation.setTruthValue(truthValue);
//		}
////			System.out.println("FeatureExpression");
//		return new FeatureExpression(featureExpression.getFeature(), truthValue);
//	}

	/*
	 * This method puts feature expression objects into an array list starting from
	 * index 0 to use the indices as the variable in SAT problem
	 */
	public List<FeatureExpression> getFeatureExpressionList(
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {

		Set<Entry<String, FeatureExpression>> entrySet = featureExpressionMapFromFeatureModel.entrySet();
		Iterator<Entry<String, FeatureExpression>> entrySetIterator = entrySet.iterator();
		List<FeatureExpression> featureExpressionList = new ArrayList<FeatureExpression>(
				featureExpressionMapFromFeatureModel.size() + 1);

		int index = 0;
		while (entrySetIterator.hasNext()) {

			Entry<String, FeatureExpression> entry = entrySetIterator.next();
			String featureName = entry.getKey();
			FeatureExpression featureExpression = entry.getValue();
			if (!featureName.contains("!") && !(featureExpression == null)
					&& !(featureExpression.getFeature().getName() == null)) {
				featureExpressionList.add(index, featureExpression);
//				System.out.println(featureName + " - " + (index));
				index++;
			}

		}
//		System.out.println("------------------------------");

		return featureExpressionList;
	}

	private void printFeatureExpressionList(List<FeatureExpression> featureExpressionList) {

		Iterator<FeatureExpression> featureExpressionListIterator = featureExpressionList.iterator();

		while (featureExpressionListIterator.hasNext()) {
			FeatureExpression featureExpression = featureExpressionListIterator.next();
			int index = featureExpressionList.indexOf(featureExpression);
			System.out.println(featureExpression.getFeature().getName() + " - " + (index + 1));

		}

	}

	public void matchFeatureExpressions(Map<String, FeatureExpression> featureExpressionMapFromFeatureModel,
			Map<String, FeatureExpression> productConfigurationMap) {
		for (Entry<String, FeatureExpression> entry : productConfigurationMap.entrySet()) {
			FeatureExpression featureExpressionInProductConfiguration = entry.getValue();
			String featureName = entry.getKey();
//				System.out.println("Feature name: " + featureName + " - " + featureExpressionInProductConfiguration.evaluate());
//				if (featureName.equals("b") || featureName.equals("d") || featureName.equals("w")) {
//					featureExpressionMapFromFeatureModel.get(featureName)
//					.setTruthValue(true);
//					System.out.println("HERE");
//				}else
			featureExpressionMapFromFeatureModel.get(featureName)
					.setTruthValue(featureExpressionInProductConfiguration.evaluate());
		}
	}

}
