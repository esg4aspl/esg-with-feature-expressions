package tr.edu.iyte.esgfx.productconfigurationgeneration;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;

import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featuremodel.Connector;
import tr.edu.iyte.esgfx.model.featuremodel.ConnectorAND;
import tr.edu.iyte.esgfx.model.featuremodel.ConnectorOR;
import tr.edu.iyte.esgfx.model.featuremodel.Feature;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;
import tr.edu.iyte.esgfx.model.featuremodel.Implicant;
import tr.edu.iyte.esgfx.model.featuremodel.Implication;

public class SATSolverGenerationFromFeatureModel {

	public void addSATClauses(ISolver solver, FeatureModel featureModel,
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel,
			List<FeatureExpression> featureExpressionList) throws ContradictionException {

		addFeatureClauses(solver, featureModel, featureExpressionMapFromFeatureModel, featureExpressionList);
		addConnectorConstraintClauses(solver, featureModel, featureExpressionMapFromFeatureModel,
				featureExpressionList);
		addImplicationConstraintClauses(solver, featureModel, featureExpressionMapFromFeatureModel,
				featureExpressionList);

	}

	private void addFeatureClauses(ISolver solver, FeatureModel featureModel,
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel,
			List<FeatureExpression> featureExpressionList) throws ContradictionException {

		Set<Feature> featureSetofFeatureModel = featureModel.getFeatureSet();

		Set<Feature> clonedFeatureSet = new LinkedHashSet<>();
		clonedFeatureSet.addAll(featureSetofFeatureModel);

		clonedFeatureSet.addAll(featureModel.getORFeatures().keySet());
		clonedFeatureSet.addAll(featureModel.getXORFeatures().keySet());

		Iterator<Feature> featureIterator = clonedFeatureSet.iterator();

		Set<Feature> optionalConcreteFeatures = new LinkedHashSet<>();

		while (featureIterator.hasNext()) {
			Feature feature = featureIterator.next();
			String featureName = feature.getName();

//			System.out.println("Feature " + featureName);

			if (featureModel.getRoot().equals(feature)) {
//				System.out.println("Feature " + featureName + " is root");
				if (isRootFeatureInFeatureESGFx(featureModel, featureExpressionMapFromFeatureModel)) {
					VecInt vecInt = new VecInt();
					vecInt.push(getIDByFeatureName(featureName, featureExpressionList));
					solver.addClause(vecInt);

//					System.out.println("VecInt " + featureName + " " + vecInt);
				}
			}
			/*
			 * abstract/concrete mandatory/optional OR/XOR
			 */

			if (feature.isMandatory()) {
				if (featureModel.isORFeature(feature)) {
//					System.out.println("Feature " + featureName + " is concrete, mandatory OR feature");
					addMandatoryORFeatureClauses(solver, featureModel, feature, featureExpressionList,
							optionalConcreteFeatures);
//					System.out.println("------------------------------------------------------");
				} else if (featureModel.isXORFeature(feature)) {
//					System.out.println("Feature " + featureName + " is concrete, mandatory XOR feature");
					addMandatoryXORFeatureClauses(solver, featureModel, feature, featureExpressionList);
//					System.out.println("------------------------------------------------------");
				} else if (!feature.isAbstract()) {
//					System.out.println("Feature " + featureName + " is concrete and mandatory");
					VecInt vecInt = new VecInt();
					vecInt.push(getIDByFeatureName(featureName, featureExpressionList));
					solver.addClause(vecInt);
//					printVecInt(vecInt, featureName);
//					System.out.println("------------------------------------------------------");
				}
			} else {
				if (featureModel.isORFeature(feature)) {
//					System.out.println("Feature " + featureName + " is concrete, optional OR feature");
					addOptionalORFeatureClauses(solver, featureModel, feature, featureExpressionList,
							optionalConcreteFeatures);
//					System.out.println("------------------------------------------------------");
				} else if (featureModel.isXORFeature(feature)) {
//					System.out.println("Feature " + featureName + " is concrete, optional XOR feature");
					addOptionalXORFeatureClauses(solver, featureModel, feature, featureExpressionList);
//					System.out.println("------------------------------------------------------");
				} else if (!feature.isAbstract()) {
					Feature parent = feature.getParent();
					if (featureModel.isANDFeature(parent)) {
//						System.out.println("Feature " + featureName + " is concrete and optional");
						optionalConcreteFeatures.add(feature);
//						System.out.println("------------------------------------------------------");
					}

				}
			}

		}

		addOptionalConcreteFeatureClauses(solver, optionalConcreteFeatures, featureExpressionList);
//		System.out.println("------------------------------------------------------");

	}

	private void addMandatoryORFeatureClauses(ISolver solver, FeatureModel featureModel, Feature feature,
			List<FeatureExpression> featureExpressionList, Set<Feature> optionalConcreteFeatures)
			throws ContradictionException {

		Set<Feature> childORFeatures = featureModel.getChildORFeatures(feature);

		if (childORFeatures.size() > 0) {
			VecInt vecInt = new VecInt(childORFeatures.size());
			StringBuilder sb = new StringBuilder();

			Iterator<Feature> childORFeaturesIterator = childORFeatures.iterator();

			while (childORFeaturesIterator.hasNext()) {
				Feature childFeature = childORFeaturesIterator.next();
//				System.out.println("Child OR feature: " + childFeature.getName());
				vecInt.push(getIDByFeatureName(childFeature.getName(), featureExpressionList));
				sb.append(childFeature.getName() + " ");

				optionalConcreteFeatures.add(childFeature);
			}
//			printVecInt(vecInt, sb.toString());
			solver.addClause(vecInt);
		}
	}

	private void printVecInt(VecInt vecInt, String s) {

		System.out.println("VecInt " + s + " " + vecInt);

	}

	private void addMandatoryXORFeatureClauses(ISolver solver, FeatureModel featureModel, Feature feature,
			List<FeatureExpression> featureExpressionList) throws ContradictionException {

//		System.out.println("Feature " + feature.getName() + " in addMandatoryXORFeatureClauses");
		Set<Feature> childXORFeatures = featureModel.getChildXORFeatures(feature);

		if (childXORFeatures.size() > 0) {

			Iterator<Feature> childXORFeaturesIterator = childXORFeatures.iterator();
			StringBuilder sb = new StringBuilder();
			VecInt vecInt = new VecInt(childXORFeatures.size());

			while (childXORFeaturesIterator.hasNext()) {

				Feature childFeature = childXORFeaturesIterator.next();
//				System.out.println("childFeature " + childFeature.getName());

				sb.append(childFeature.getName() + " ");
				vecInt.push(getIDByFeatureName(childFeature.getName(), featureExpressionList));

				Iterator<Feature> cloneChildXORFeaturesIterator = childXORFeatures.iterator();

				while (cloneChildXORFeaturesIterator.hasNext()) {

					Feature nextChildFeature = cloneChildXORFeaturesIterator.next();
//					System.out.println("nextChildFeature " + nextChildFeature.getName());
					if (nextChildFeature.hashCode() > childFeature.hashCode()) {
						StringBuilder sb2 = new StringBuilder();
						VecInt vecInt2 = new VecInt(2);

						sb2.append("!" + childFeature.getName() + " ");
						vecInt2.push(-getIDByFeatureName(childFeature.getName(), featureExpressionList));
						sb2.append("!" + nextChildFeature.getName() + " ");
						vecInt2.push(-getIDByFeatureName(nextChildFeature.getName(), featureExpressionList));

//						printVecInt(vecInt2, sb2.toString());
						solver.addClause(vecInt2);
					}
				}
			}

//			printVecInt(vecInt, sb.toString());
			solver.addClause(vecInt);
		}
	}

	private void addOptionalORFeatureClauses(ISolver solver, FeatureModel featureModel, Feature feature,
			List<FeatureExpression> featureExpressionList, Set<Feature> optionalConcreteFeatures)
			throws ContradictionException {
		Set<Feature> childORFeatures = featureModel.getChildORFeatures(feature);

		if (childORFeatures.size() > 0) {
			optionalConcreteFeatures.addAll(childORFeatures);
		}

	}

	private void addOptionalXORFeatureClauses(ISolver solver, FeatureModel featureModel, Feature feature,
			List<FeatureExpression> featureExpressionList) throws ContradictionException {
		Set<Feature> childXORFeatures = featureModel.getChildXORFeatures(feature);

		if (childXORFeatures.size() > 0) {
			VecInt vecInt = new VecInt(childXORFeatures.size());
			StringBuilder sb = new StringBuilder();

			Iterator<Feature> childXORFeaturesIterator = childXORFeatures.iterator();

			while (childXORFeaturesIterator.hasNext()) {
				Feature childFeature = childXORFeaturesIterator.next();
//				System.out.println("Child XOR feature: " + childFeature.getName());
				sb.append(childFeature.getName() + " ");
				vecInt.push(-getIDByFeatureName(childFeature.getName(), featureExpressionList));
			}
//			printVecInt(vecInt, sb.toString());
			solver.addClause(vecInt);
		}
	}

	private void addOptionalConcreteFeatureClauses(ISolver solver, Set<Feature> optionalConcreteFeatures,
			List<FeatureExpression> featureExpressionList) throws ContradictionException {

		if (optionalConcreteFeatures.size() > 0) {
			StringBuilder sb = new StringBuilder();
			VecInt vecInt = new VecInt(optionalConcreteFeatures.size());

			Iterator<Feature> optionalConcreteFeaturesIterator = optionalConcreteFeatures.iterator();
			while (optionalConcreteFeaturesIterator.hasNext()) {
				Feature optionalConcreteFeature = optionalConcreteFeaturesIterator.next();
				sb.append(optionalConcreteFeature.getName() + " ");
				vecInt.push(getIDByFeatureName(optionalConcreteFeature.getName(), featureExpressionList));

			}
//			printVecInt(vecInt, sb.toString());
			solver.addClause(vecInt);

		}

	}

	private boolean isRootFeatureInFeatureESGFx(FeatureModel featureModel,
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {
		boolean isRootFeatureInFeatureESGFx = false;

		Iterator<Entry<String, FeatureExpression>> entrySetIterator = featureExpressionMapFromFeatureModel.entrySet()
				.iterator();
		while (entrySetIterator.hasNext()) {
			Entry<String, FeatureExpression> entry = entrySetIterator.next();
			FeatureExpression value = entry.getValue();
			Feature feature = value.getFeature();
			if (feature.equals(featureModel.getRoot())) {
				isRootFeatureInFeatureESGFx = true;
				break;
			}
		}

		return isRootFeatureInFeatureESGFx;
	}

	private Integer getIDByFeatureName(String featureName, List<FeatureExpression> featureExpressionList) {

		Iterator<FeatureExpression> featureExpressionListIterator = featureExpressionList.iterator();
		boolean isNegation = false;
		if (featureName.contains("!")) {
			
			featureName = featureName.substring(1);
			isNegation = true;
		}
		while (featureExpressionListIterator.hasNext()) {
			FeatureExpression featureExpression = featureExpressionListIterator.next();
			String name = featureExpression.getFeature().getName();
			if (name.equals(featureName)) {
				if (isNegation) {
					return (featureExpressionList.indexOf(featureExpression) + 1) * (-1);
				} else
					return featureExpressionList.indexOf(featureExpression) + 1;
			}

		}
//		System.out.println("Feature " + featureName + " NOT found in featureExpressionList");
		return 0;
	}

	private void addConnectorConstraintClauses(ISolver solver, FeatureModel featureModel,
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel,
			List<FeatureExpression> featureExpressionList) throws ContradictionException {

		Set<Connector> connConstraints = featureModel.getConnConstraints();
		Iterator<Connector> connConstraintIterator = connConstraints.iterator();

		while (connConstraintIterator.hasNext()) {
			Connector connConstraint = connConstraintIterator.next();
			Set<Feature> featureSet = connConstraint.getFeatureSet();
			String operator = connConstraint.getOperator();

			if (operator.equals("AND")) {
				for (Feature feature : featureSet) {
					VecInt vecInt = new VecInt();
					vecInt.push(getIDByFeatureName(feature.getName(), featureExpressionList));
					solver.addClause(vecInt);
				}

			} else if (operator.equals("OR")) {
				VecInt vecInt = new VecInt(featureSet.size());
				for (Feature feature : featureSet) {
					vecInt.push(getIDByFeatureName(feature.getName(), featureExpressionList));
				}
				solver.addClause(vecInt);
			}
		}

	}

	private void addIffConstraintClauses(Set<Implication> impConstraints, FeatureModel featureModel) {

		Set<Implication> iffConstraints = featureModel.getIffConstraints();

		if (iffConstraints.isEmpty() || iffConstraints == null) {
			return;
		}
		Iterator<Implication> iffConstraintIterator = iffConstraints.iterator();

		while (iffConstraintIterator.hasNext()) {
			Implication iffConstraint = iffConstraintIterator.next();
			String iffLHSType = iffConstraint.getLHStype();
			String iffRHSType = iffConstraint.getRHStype();
			Implicant lhsImplicant = iffConstraint.getLeftHandSide();
			Implicant rhsImplicant = iffConstraint.getRightHandSide();

			Implication implication1 = new Implication(lhsImplicant, rhsImplicant);
			implication1.setLHStype(iffLHSType);
			implication1.setRHStype(iffRHSType);

			Implication implication2 = new Implication(rhsImplicant, lhsImplicant);
			implication2.setLHStype(iffRHSType);
			implication2.setRHStype(iffLHSType);

			impConstraints.add(implication1);
			impConstraints.add(implication2);
		}

	}

	private void addImplicationConstraintClauses(ISolver solver, FeatureModel featureModel,
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel,
			List<FeatureExpression> featureExpressionList) throws ContradictionException {

		Set<Implication> impConstraints = featureModel.getImpConstraints();
		addIffConstraintClauses(impConstraints, featureModel);
		Iterator<Implication> impConstraintIterator = impConstraints.iterator();

		while (impConstraintIterator.hasNext()) {
			Implication impConstraint = impConstraintIterator.next();

//			System.out.println("Implication: " + impConstraint.toString());

			String lhsType = impConstraint.getLHStype();
			String rhsType = impConstraint.getRHStype();

			String lhsName = impConstraint.getLeftHandSide().implicantToString();
			String rhsName = impConstraint.getRightHandSide().implicantToString();

			Implicant lhsImplicant = impConstraint.getLeftHandSide();
			Implicant rhsImplicant = impConstraint.getRightHandSide();

			if (lhsType.equals("var") && rhsType.equals("var")) {
//				System.out.println("Implication: Feature->Feature");

				Feature lhsFeature = (Feature) lhsImplicant;
				Feature rhsFeature = (Feature) rhsImplicant;

				VecInt vecInt = new VecInt(2);
				vecInt.push(-getIDByFeatureName(lhsFeature.getName(), featureExpressionList));
				vecInt.push(getIDByFeatureName(rhsFeature.getName(), featureExpressionList));
				solver.addClause(vecInt);

//				printVecInt(vecInt, "!" + lhsFeature.getName() + " OR " + rhsFeature.getName());
			} else if (lhsType.equals("var") && rhsType.equals("disj")) {
//				System.out.println("Implication: Feature->Disjunction");

				Feature lhsFeature = (Feature) lhsImplicant;
				Connector rhsDisjunction = (ConnectorOR) rhsImplicant;

				Set<Feature> disjunctionFeatures = rhsDisjunction.getFeatureSet();

				VecInt vecInt = new VecInt(disjunctionFeatures.size() + 1);
				vecInt.push(-getIDByFeatureName(lhsFeature.getName(), featureExpressionList));

				for (Feature disjunctionFeature : disjunctionFeatures) {
					vecInt.push(getIDByFeatureName(disjunctionFeature.getName(), featureExpressionList));
				}
				solver.addClause(vecInt);

//				printVecInt(vecInt, "!" + lhsName + " OR " + rhsName);

			} else if (lhsType.equals("var") && rhsType.equals("conj")) {
//				System.out.println("Implication: Feature->Conjunction");
				Feature lhsFeature = (Feature) lhsImplicant;
				Connector rhsConjunction = (ConnectorAND) rhsImplicant;

				Set<Feature> conjunctionFeatures = rhsConjunction.getFeatureSet();

				for (Feature conjunctionFeature : conjunctionFeatures) {
					VecInt vecInt = new VecInt();
					vecInt.push(-getIDByFeatureName(lhsFeature.getName(), featureExpressionList));
					vecInt.push(getIDByFeatureName(conjunctionFeature.getName(), featureExpressionList));
					solver.addClause(vecInt);
//					printVecInt(vecInt, "!" + lhsFeature.getName() + " OR " + conjunctionFeature.getName());
				}

			} else if (lhsType.equals("disj") && rhsType.equals("var")) {
//				System.out.println("Implication: Disjunction->Feature");

				Connector lhsDisjunction = (ConnectorOR) lhsImplicant;
//				System.out.println("lhsDisjunction " + lhsDisjunction.toString());
				Set<Feature> disjunctionFeatures = lhsDisjunction.getFeatureSet();

				Feature rhsFeature = (Feature) rhsImplicant;
//				System.out.println("rhsFeature " + rhsFeature.getName());

				for (Feature disjunctionFeature : disjunctionFeatures) {
					if (disjunctionFeature.isAbstract()) {
						Set<Feature> childFeatures = getChildFeatures(featureModel, disjunctionFeature);
						for (Feature childFeature : childFeatures) {
							VecInt vecInt = new VecInt();
							vecInt.push(-getIDByFeatureName(childFeature.getName(), featureExpressionList));
							vecInt.push(getIDByFeatureName(rhsFeature.getName(), featureExpressionList));
//							printVecInt(vecInt, "!" + childFeature.getName() + " OR " + rhsFeature.getName());
							solver.addClause(vecInt);
						}
					} else {
						VecInt vecInt = new VecInt();
						vecInt.push(-getIDByFeatureName(disjunctionFeature.getName(), featureExpressionList));
						vecInt.push(getIDByFeatureName(rhsFeature.getName(), featureExpressionList));
//						printVecInt(vecInt, "!" + disjunctionFeature.getName() + " OR " + rhsFeature.getName());
						solver.addClause(vecInt);
					}

				}

			} else if (lhsType.equals("disj") && rhsType.equals("disj")) {
//				System.out.println("Implication: Disjunction->Disjunction");

				Connector lhsDisjunction = (ConnectorOR) lhsImplicant;
				Set<Feature> lhsDisjunctionFeatures = lhsDisjunction.getFeatureSet();

				Connector rhsDisjunction = (ConnectorOR) rhsImplicant;
				Set<Feature> rhsDisjunctionFeatures = rhsDisjunction.getFeatureSet();

				for (Feature lhsDisjunctionFeature : lhsDisjunctionFeatures) {
					StringBuilder sb = new StringBuilder();
					VecInt vecInt = new VecInt(1 + rhsDisjunctionFeatures.size());

					sb.append("!" + lhsDisjunctionFeature.getName());
					vecInt.push(-getIDByFeatureName(lhsDisjunctionFeature.getName(), featureExpressionList));

					for (Feature rhsDisjunctionFeature : rhsDisjunctionFeatures) {
						sb.append(" OR " + rhsDisjunctionFeature.getName());
						vecInt.push(getIDByFeatureName(rhsDisjunctionFeature.getName(), featureExpressionList));
					}
//					printVecInt(vecInt, sb.toString());
					solver.addClause(vecInt);
				}

			} else if (lhsType.equals("disj") && rhsType.equals("conj")) {
//				System.out.println("Implication: Disjunction->Conjunction");
				Connector lhsDisjunction = (ConnectorOR) lhsImplicant;
				Set<Feature> lhsDisjunctionFeatures = lhsDisjunction.getFeatureSet();

				Connector rhsConjunction = (ConnectorAND) rhsImplicant;
				Set<Feature> rhsConjunctionFeatures = rhsConjunction.getFeatureSet();

				for (Feature lhsDisjunctionFeature : lhsDisjunctionFeatures) {

					for (Feature rhsConjunctionFeature : rhsConjunctionFeatures) {
						StringBuilder sb = new StringBuilder();
						VecInt vecInt = new VecInt();

						sb.append("!" + lhsDisjunctionFeature.getName());
						vecInt.push(-getIDByFeatureName(lhsDisjunctionFeature.getName(), featureExpressionList));

						sb.append(" OR " + rhsConjunctionFeature.getName());
						vecInt.push(getIDByFeatureName(rhsConjunctionFeature.getName(), featureExpressionList));

//						printVecInt(vecInt, sb.toString());
						solver.addClause(vecInt);
					}
				}

			} else if (lhsType.equals("conj") && rhsType.equals("var")) {
//				System.out.println("Implication: Conjunction->Feature");
				Connector lhsConjunction = (ConnectorAND) lhsImplicant;
				Set<Feature> lhsConjunctionFeatures = lhsConjunction.getFeatureSet();

				Feature rhsFeature = (Feature) rhsImplicant;

				StringBuilder sb = new StringBuilder();
				VecInt vecInt = new VecInt(lhsConjunctionFeatures.size() + 1);

				for (Feature lhsConjunctionFeature : lhsConjunctionFeatures) {
					sb.append("!" + lhsConjunctionFeature.getName() + " OR ");
					vecInt.push(-getIDByFeatureName(lhsConjunctionFeature.getName(), featureExpressionList));
				}

				sb.append(rhsFeature.getName());
				vecInt.push(getIDByFeatureName(rhsFeature.getName(), featureExpressionList));

//				printVecInt(vecInt, sb.toString());
				solver.addClause(vecInt);

			} else if (lhsType.equals("conj") && rhsType.equals("disj")) {
//				System.out.println("Implication: Conjunction->Disjunction");
				Connector lhsConjunction = (ConnectorAND) lhsImplicant;
				Set<Feature> lhsConjunctionFeatures = lhsConjunction.getFeatureSet();

				Connector rhsDisjunction = (ConnectorOR) rhsImplicant;
				Set<Feature> rhsDisjunctionFeatures = rhsDisjunction.getFeatureSet();

				StringBuilder sb = new StringBuilder();
				VecInt vecInt = new VecInt(lhsConjunctionFeatures.size() + rhsDisjunctionFeatures.size());
				for (Feature lhsConjunctionFeature : lhsConjunctionFeatures) {
					sb.append("!" + lhsConjunctionFeature.getName() + " OR ");
					vecInt.push(-getIDByFeatureName(lhsConjunctionFeature.getName(), featureExpressionList));
				}

				for (Feature rhsDisjunctionFeature : rhsDisjunctionFeatures) {
					sb.append(rhsDisjunctionFeature.getName() + " OR ");
					vecInt.push(getIDByFeatureName(rhsDisjunctionFeature.getName(), featureExpressionList));
				}
//				printVecInt(vecInt, sb.toString());
				solver.addClause(vecInt);

			} else if (lhsType.equals("conj") && rhsType.equals("conj")) {
//				System.out.println("Implication: Conjunction->Conjunction");
				Connector lhsConjunction = (ConnectorAND) lhsImplicant;
				Set<Feature> lhsConjunctionFeatures = lhsConjunction.getFeatureSet();

				Connector rhsConjunction = (ConnectorAND) rhsImplicant;
				Set<Feature> rhsConjunctionFeatures = rhsConjunction.getFeatureSet();

				for (Feature rhsConjunctionFeature : rhsConjunctionFeatures) {
					StringBuilder sb = new StringBuilder();
					VecInt vecInt = new VecInt(lhsConjunctionFeatures.size() + 1);

					for (Feature lhsConjunctionFeature : lhsConjunctionFeatures) {
						sb.append("!" + lhsConjunctionFeature.getName() + " OR ");
						vecInt.push(-getIDByFeatureName(lhsConjunctionFeature.getName(), featureExpressionList));
					}
					sb.append(rhsConjunctionFeature.getName());
					vecInt.push(getIDByFeatureName(rhsConjunctionFeature.getName(), featureExpressionList));

//					printVecInt(vecInt, sb.toString());
					solver.addClause(vecInt);
				}
			}

		}

	}

	private Set<Feature> getChildFeatures(FeatureModel featureModel, Feature abstractFeature) {

		if (featureModel.isORFeature(abstractFeature)) {
			return featureModel.getORFeatures().get(abstractFeature);
		} else if (featureModel.isXORFeature(abstractFeature)) {
			return featureModel.getXORFeatures().get(abstractFeature);
		} else {
			return new LinkedHashSet<>();
		}

	}

}
