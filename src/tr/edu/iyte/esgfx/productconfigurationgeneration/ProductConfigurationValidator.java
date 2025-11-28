package tr.edu.iyte.esgfx.productconfigurationgeneration;

import java.util.Iterator;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featuremodel.Feature;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;

public class ProductConfigurationValidator {

	public boolean validate(FeatureModel featureModel,
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {

		boolean isValid = true;

		isValid = isValid && isAllMandatoryFeaturesSelected(featureModel, featureExpressionMapFromFeatureModel);

//		System.out.println("isValid-1 " + isValid);

		if (hasMandatoryOrFeature(featureModel)) {
			isValid = isValid
					&& atLeastOneMandatoryORFeatureSelected(featureModel, featureExpressionMapFromFeatureModel);
		}

//		System.out.println("isValid-2 " + isValid);

		if (hasXORFeature(featureModel)) {
			isValid = isValid && XORFeatureSelectionIsValid(featureModel, featureExpressionMapFromFeatureModel);
		}

//		System.out.println("isValid-3 " + isValid);

		return isValid;
	}

	private boolean isAllMandatoryFeaturesSelected(FeatureModel featureModel,
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {

		Iterator<Feature> featureSetIterator = featureModel.getFeatureSet().iterator();
		boolean isAllMandatoryFeaturesSelected = true;

		while (featureSetIterator.hasNext()) {
			Feature feature = featureSetIterator.next();
//			System.out.println("Feature " + feature.getName() );
			if (feature.isMandatory() && !feature.isAbstract()) {
				FeatureExpression featureExpression = featureExpressionMapFromFeatureModel.get(feature.getName());
				isAllMandatoryFeaturesSelected = isAllMandatoryFeaturesSelected && featureExpression.evaluate();
			}
		}

//		System.out.println("All mandatory features selected? " + isAllMandatoryFeaturesSelected);
		return isAllMandatoryFeaturesSelected;
	}

	private boolean hasMandatoryOrFeature(FeatureModel featureModel) {
		boolean hasMandatoryOrFeature = featureModel.getORFeatures().entrySet().stream()
				.anyMatch(entry -> entry.getKey().isMandatory());

//		if (hasMandatoryOrFeature) {
//			System.out.println("Has mandatory OR feature");
//		} else {
//			System.out.println("Does not have mandatory OR feature");
//		}
		return hasMandatoryOrFeature;
	}

	private boolean atLeastOneMandatoryORFeatureSelected(FeatureModel featureModel,
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {

		Iterator<Entry<Feature, Set<Feature>>> featureSetIterator = featureModel.getORFeatures().entrySet().iterator();

		boolean isAtLeastOneMandatoryORFeatureSelected = false;
		while (featureSetIterator.hasNext()) {
			Entry<Feature, Set<Feature>> entry = featureSetIterator.next();
			Feature orFeature = entry.getKey();

			if (orFeature.isMandatory()) {
				Set<Feature> orFeatureSet = entry.getValue();

				boolean atLeastOneMandatoryFeatureSelected = atLeastOneMandatoryFeatureSelected(orFeatureSet,
						featureExpressionMapFromFeatureModel);

//				if (!atLeastOneMandatoryFeatureSelected) {
//					System.out.println("At least one mandatory OR feature selected? " + orFeature.getName() + " false");
//				}

				isAtLeastOneMandatoryORFeatureSelected = isAtLeastOneMandatoryORFeatureSelected
						|| atLeastOneMandatoryFeatureSelected;
			}

		}

		return isAtLeastOneMandatoryORFeatureSelected;

	}

	private boolean atLeastOneMandatoryFeatureSelected(Set<Feature> featureSet,
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {

		boolean isAtLeastOneMandatoryFeatureSelected = false;
		Iterator<Feature> orFeatureSetIterator = featureSet.iterator();
		while (orFeatureSetIterator.hasNext()) {
			Feature feature = orFeatureSetIterator.next();
			FeatureExpression featureExpression = featureExpressionMapFromFeatureModel.get(feature.getName());
			boolean mandatoryFeatureSelected = featureExpression.evaluate();

//			if (!mandatoryFeatureSelected) {
//				System.out.println("feature " + feature.getName() + " is not selected. - its parent is mandatory");
//			}

			isAtLeastOneMandatoryFeatureSelected = isAtLeastOneMandatoryFeatureSelected || mandatoryFeatureSelected;

		}
		return isAtLeastOneMandatoryFeatureSelected;

	}

	public boolean hasMandatoryXORFeature(FeatureModel featureModel) {
		boolean hasMandatoryXORFeature = featureModel.getXORFeatures().entrySet().stream()
				.anyMatch(entry -> entry.getKey().isMandatory());

//		if (hasMandatoryXORFeature) {
//			System.out.println("Has mandatory OR feature");
//		} else {
//			System.out.println("Does not have mandatory OR feature");
//		}
		return hasMandatoryXORFeature;
	}

	private boolean hasXORFeature(FeatureModel featureModel) {
		boolean hasXORFeature = !featureModel.getXORFeatures().isEmpty();

//		if (hasXORFeature) {
//			System.out.println("Has XOR feature");
//		} else {
//			System.out.println("Does not have XOR feature");
//		}
		return hasXORFeature;
	}

	private boolean XORFeatureSelectionIsValid(FeatureModel featureModel,
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {
		Iterator<Entry<Feature, Set<Feature>>> featureSetIterator = featureModel.getXORFeatures().entrySet().iterator();

		boolean XORFeatureSelectionIsValid = true;
		while (featureSetIterator.hasNext()) {
			Entry<Feature, Set<Feature>> entry = featureSetIterator.next();
			Feature xorFeature = entry.getKey();
			Set<Feature> xorSubFeaturesSet = entry.getValue();

			boolean isOnlyOneXORFeatureSelected = onlyOneXORFeatureSelected(xorSubFeaturesSet,
					featureExpressionMapFromFeatureModel);

//			if (!isOnlyOneXORFeatureSelected) {
//				System.out.println("Only one XOR feature selected? " + xorFeature.getName() + " false");
//			}

			XORFeatureSelectionIsValid = XORFeatureSelectionIsValid && isOnlyOneXORFeatureSelected;

			if (xorFeature.isMandatory()) {

//				System.out.println("XORFeatureSelectionIsValid-1 " + XORFeatureSelectionIsValid);

				boolean atLeastOneMandatoryFeatureSelected = atLeastOneMandatoryFeatureSelected(xorSubFeaturesSet,
						featureExpressionMapFromFeatureModel);

//				if (!atLeastOneMandatoryFeatureSelected) {
//					System.out.println("atLeastOneMandatoryFeatureSelected " + xorFeature.getName() + " is false");
//				}

				XORFeatureSelectionIsValid = XORFeatureSelectionIsValid && atLeastOneMandatoryFeatureSelected;

//				System.out.println("XORFeatureSelectionIsValid-2 " + XORFeatureSelectionIsValid);

			} else {
				boolean onlyOneOrNoneOptionalXORFeatureIsSelected = isOnlyOneXORFeatureSelected
						|| noneOptionalXORFeatureSelected(xorSubFeaturesSet, featureExpressionMapFromFeatureModel);
				
//				if (!onlyOneOrNoneOptionalXORFeatureIsSelected) {
//					System.out
//							.println("onlyOneOrNoneOptionalXORFeatureIsSelected " + xorFeature.getName() + " is false");
//				}
				XORFeatureSelectionIsValid = XORFeatureSelectionIsValid && onlyOneOrNoneOptionalXORFeatureIsSelected;
			}
		}

//		if (!XORFeatureSelectionIsValid) {
//			System.out.println("XORFeatureSelectionIsValid is false");
//		}
		return XORFeatureSelectionIsValid;
	}

	private boolean onlyOneXORFeatureSelected(Set<Feature> featureSet,
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {
		boolean isOnlyOneXORFeatureSelected = true;

		Iterator<Feature> xorFeatureSetIterator = featureSet.iterator();
		while (xorFeatureSetIterator.hasNext()) {
			Feature feature = xorFeatureSetIterator.next();
			FeatureExpression featureExpression = featureExpressionMapFromFeatureModel.get(feature.getName());
			boolean featureSelected = featureExpression.evaluate();
			Iterator<Feature> xorFeatureSetIterator2 = featureSet.iterator();

			if (featureSelected) {
				while (xorFeatureSetIterator2.hasNext()) {
					Feature feature2 = xorFeatureSetIterator2.next();
					FeatureExpression featureExpression2 = featureExpressionMapFromFeatureModel.get(feature2.getName());
					boolean featureSelected2 = featureExpression2.evaluate();
					if (!feature.equals(feature2)) {
//						System.out.println("Feature " + feature.getName() + " " + featureSelected);
//						System.out.println("Feature2 " + feature2.getName() + " " + featureSelected2);
						featureSelected = featureSelected && !featureSelected2;

//						System.out.println("Current featureSelected " + featureSelected);
					}
				}
				isOnlyOneXORFeatureSelected = isOnlyOneXORFeatureSelected && featureSelected;
			}

		}
//		System.out.println("isOnlyOneXORFeatureSelected " + isOnlyOneXORFeatureSelected);

		return isOnlyOneXORFeatureSelected;
	}

	private boolean noneOptionalXORFeatureSelected(Set<Feature> featureSet,
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {
		boolean noneOptionalXORFeatureSelected = true;

		Iterator<Feature> xorFeatureSetIterator = featureSet.iterator();
		while (xorFeatureSetIterator.hasNext()) {
			Feature feature = xorFeatureSetIterator.next();
			FeatureExpression featureExpression = featureExpressionMapFromFeatureModel.get(feature.getName());
			boolean featureSelected = featureExpression.evaluate();
			Iterator<Feature> xorFeatureSetIterator2 = featureSet.iterator();

			if (!featureSelected) {
				while (xorFeatureSetIterator2.hasNext()) {
					Feature feature2 = xorFeatureSetIterator2.next();
					FeatureExpression featureExpression2 = featureExpressionMapFromFeatureModel.get(feature2.getName());
					boolean featureSelected2 = featureExpression2.evaluate();
					if (!feature.equals(feature2)) {
//						System.out.println("Feature " + feature.getName() + " " + featureSelected);
//						System.out.println("Feature2 " + feature2.getName() + " " + featureSelected2);
						featureSelected = !featureSelected && !featureSelected2;

//						System.out.println("Current featureSelected " + featureSelected);
					}
				}
				noneOptionalXORFeatureSelected = noneOptionalXORFeatureSelected && featureSelected;
			}

		}
//		System.out.println("noneOptionalXORFeatureSelected " + noneOptionalXORFeatureSelected);

		return noneOptionalXORFeatureSelected;
	}

	public boolean onlyOneOrNoneOptionalXORFeatureIsSelected(Set<Feature> featureSet,
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {

		boolean onlyOneOrNoneOptionalXORFeatureIsSelected = true;

		Iterator<Feature> xorFeatureSetIterator = featureSet.iterator();
		while (xorFeatureSetIterator.hasNext()) {
			Feature feature = xorFeatureSetIterator.next();
			FeatureExpression featureExpression = featureExpressionMapFromFeatureModel.get(feature.getName());
			boolean featureSelected = featureExpression.evaluate();
			Iterator<Feature> xorFeatureSetIterator2 = featureSet.iterator();

			if (featureSelected) {

				while (xorFeatureSetIterator2.hasNext()) {
					Feature feature2 = xorFeatureSetIterator2.next();
					FeatureExpression featureExpression2 = featureExpressionMapFromFeatureModel.get(feature2.getName());
					boolean featureSelected2 = featureExpression2.evaluate();

					if (!feature.equals(feature2)) {
						featureSelected = featureSelected && !featureSelected2;
//						System.out.println("Feature " + feature.getName() + " " + featureSelected);
//						System.out.println("Feature2 " + feature2.getName() + " " + featureSelected2);
//						System.out.println("Current featureSelected " + featureSelected);
					}
				}
				onlyOneOrNoneOptionalXORFeatureIsSelected = onlyOneOrNoneOptionalXORFeatureIsSelected
						&& featureSelected;
			} else {
//				System.out.println("ELSE");
				while (xorFeatureSetIterator2.hasNext()) {
					Feature feature2 = xorFeatureSetIterator2.next();
					FeatureExpression featureExpression2 = featureExpressionMapFromFeatureModel.get(feature2.getName());
					boolean featureSelected2 = featureExpression2.evaluate();

					if (!feature.equals(feature2)) {
						featureSelected = !featureSelected && !featureSelected2;
//						System.out.println("Feature " + feature.getName() + " " + featureSelected);
//						System.out.println("Feature2 " + feature2.getName() + " " + featureSelected2);
//						System.out.println("Current featureSelected " + featureSelected);
					}
				}
				onlyOneOrNoneOptionalXORFeatureIsSelected = onlyOneOrNoneOptionalXORFeatureIsSelected
						&& featureSelected;

			}
		}

		return onlyOneOrNoneOptionalXORFeatureIsSelected;

	}

}
