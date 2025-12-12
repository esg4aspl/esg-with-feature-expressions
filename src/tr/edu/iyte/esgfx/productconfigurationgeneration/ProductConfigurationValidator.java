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

//      System.out.println("isValid-1 " + isValid);

        if (hasMandatoryOrFeature(featureModel)) {
            isValid = isValid
                    && atLeastOneMandatoryORFeatureSelected(featureModel, featureExpressionMapFromFeatureModel);
        }

//      System.out.println("isValid-2 " + isValid);

        if (hasXORFeature(featureModel)) {
            isValid = isValid && XORFeatureSelectionIsValid(featureModel, featureExpressionMapFromFeatureModel);
        }

//      System.out.println("isValid-3 " + isValid);

        return isValid;
    }

    private boolean isAllMandatoryFeaturesSelected(FeatureModel featureModel,
            Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {

        Iterator<Feature> featureSetIterator = featureModel.getFeatureSet().iterator();
        boolean isAllMandatoryFeaturesSelected = true;

        while (featureSetIterator.hasNext()) {
            Feature feature = featureSetIterator.next();
//          System.out.println("Feature " + feature.getName() );
            if (feature.isMandatory() && !feature.isAbstract()) {
                FeatureExpression featureExpression = featureExpressionMapFromFeatureModel.get(feature.getName());
                isAllMandatoryFeaturesSelected = isAllMandatoryFeaturesSelected && featureExpression.evaluate();
                
                // Optimization: Fail fast
                if (!isAllMandatoryFeaturesSelected) return false;
            }
        }

//      System.out.println("All mandatory features selected? " + isAllMandatoryFeaturesSelected);
        return isAllMandatoryFeaturesSelected;
    }

    private boolean hasMandatoryOrFeature(FeatureModel featureModel) {
        return featureModel.getORFeatures().entrySet().stream()
                .anyMatch(entry -> entry.getKey().isMandatory());
    }

    private boolean atLeastOneMandatoryORFeatureSelected(FeatureModel featureModel,
            Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {

        Iterator<Entry<Feature, Set<Feature>>> featureSetIterator = featureModel.getORFeatures().entrySet().iterator();

        while (featureSetIterator.hasNext()) {
            Entry<Feature, Set<Feature>> entry = featureSetIterator.next();
            Feature orFeature = entry.getKey();

            if (orFeature.isMandatory()) {
                Set<Feature> orFeatureSet = entry.getValue();

                boolean atLeastOneMandatoryFeatureSelected = atLeastOneMandatoryFeatureSelected(orFeatureSet,
                        featureExpressionMapFromFeatureModel);

//              if (!atLeastOneMandatoryFeatureSelected) {
//                  System.out.println("At least one mandatory OR feature selected? " + orFeature.getName() + " false");
//              }

                if (!atLeastOneMandatoryFeatureSelected) return false; // Optimization: Fail fast
            }
        }
        return true;
    }

    private boolean atLeastOneMandatoryFeatureSelected(Set<Feature> featureSet,
            Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {

        for (Feature feature : featureSet) {
            FeatureExpression featureExpression = featureExpressionMapFromFeatureModel.get(feature.getName());
            if (featureExpression.evaluate()) {
                return true; // Optimization: Found one, return immediately
            }
        }
        return false;
    }

    public boolean hasMandatoryXORFeature(FeatureModel featureModel) {
        return featureModel.getXORFeatures().entrySet().stream()
                .anyMatch(entry -> entry.getKey().isMandatory());
    }

    private boolean hasXORFeature(FeatureModel featureModel) {
        return !featureModel.getXORFeatures().isEmpty();
    }

    private boolean XORFeatureSelectionIsValid(FeatureModel featureModel,
            Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {
        Iterator<Entry<Feature, Set<Feature>>> featureSetIterator = featureModel.getXORFeatures().entrySet().iterator();

        while (featureSetIterator.hasNext()) {
            Entry<Feature, Set<Feature>> entry = featureSetIterator.next();
            Feature xorFeature = entry.getKey();
            Set<Feature> xorSubFeaturesSet = entry.getValue();

            // OPTIMIZATION: Count selected features once
            int selectedCount = countSelectedFeatures(xorSubFeaturesSet, featureExpressionMapFromFeatureModel);
            
            // Logic:
            // 1. Mandatory XOR: Exactly 1 selected.
            // 2. Optional XOR: 0 or 1 selected.

            if (xorFeature.isMandatory()) {
                if (selectedCount != 1) {
                    // System.out.println("Mandatory XOR failed for: " + xorFeature.getName());
                    return false;
                }
            } else {
                if (selectedCount > 1) {
                    // System.out.println("Optional XOR failed (multiple selected) for: " + xorFeature.getName());
                    return false;
                }
            }
        }
        return true;
    }

    // --- NEW OPTIMIZED HELPER METHOD ---
    // Replaces nested loops with a single pass O(N) counter
    private int countSelectedFeatures(Set<Feature> featureSet, Map<String, FeatureExpression> featureExpressionMap) {
        int count = 0;
        for (Feature feature : featureSet) {
            FeatureExpression expr = featureExpressionMap.get(feature.getName());
            if (expr != null && expr.evaluate()) {
                count++;
            }
        }
        return count;
    }

    // --- LEGACY METHODS REFACTORED TO USE COUNTER ---
    // Kept method signatures for compatibility but optimized internal logic.

    private boolean onlyOneXORFeatureSelected(Set<Feature> featureSet,
            Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {
        // Replacing O(N^2) logic with O(N) count check
        return countSelectedFeatures(featureSet, featureExpressionMapFromFeatureModel) == 1;
    }

    private boolean noneOptionalXORFeatureSelected(Set<Feature> featureSet,
            Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {
        // Replacing O(N^2) logic with O(N) count check
        return countSelectedFeatures(featureSet, featureExpressionMapFromFeatureModel) == 0;
    }

    public boolean onlyOneOrNoneOptionalXORFeatureIsSelected(Set<Feature> featureSet,
            Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {
        int count = countSelectedFeatures(featureSet, featureExpressionMapFromFeatureModel);
        return count == 0 || count == 1;
    }
}