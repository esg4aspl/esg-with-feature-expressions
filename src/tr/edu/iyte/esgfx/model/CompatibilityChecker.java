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
        // LinkedHashMap maintains insertion order. 
        // If order isn't critical for your logic, HashMap is slightly faster.
        // Keeping LinkedHashMap to preserve behavior.
        featureTruthValueMap = new LinkedHashMap<>();
    }

    public void clearFeatureTruthValueMap() {
        featureTruthValueMap.clear();
    }

    public boolean isFeatureTruthValueMapContainsKey(Feature feature) {
        return featureTruthValueMap.containsKey(feature);
    }

    public void fillFeatureTruthValueMap(Vertex currentVertex) {
//      System.out.println("fillFeatureTruthValueMap METHOD START");
        VertexRefinedByFeatureExpression vertexRefinedByFeatureExpression = (VertexRefinedByFeatureExpression) currentVertex;
        FeatureExpression featureExpression = vertexRefinedByFeatureExpression.getFeatureExpression();
//      Feature feature = featureExpression.getFeature();
//      System.out.println("fillFeatureTruthValueMap currentVertex " + currentVertex.toString());
//      System.out.println("featureTruthValueMap.containsKey(feature) " + featureTruthValueMap.containsKey(feature));

        if (featureExpression instanceof Conjunction) {
            for (FeatureExpression operand : ((Conjunction) featureExpression).getOperands()) {
//              System.out.println("operand " + operand.toString());
                fillFeatureTruthValueMap(operand);
            }
        } else {
            fillFeatureTruthValueMap(featureExpression);
        }

//      System.out.println(featureTruthValueMap);
//      System.out.println("fillFeatureTruthValueMap METHOD FINISH");
    }

    public void fillFeatureTruthValueMap(FeatureExpression featureExpression) {
        Feature feature = featureExpression.getFeature();
        // Optimization: Only put if absent to avoid map hashing overhead
        if (!featureTruthValueMap.containsKey(feature)) {
            // If it is Negation, value is False. If not, value is True.
            featureTruthValueMap.put(feature, !(featureExpression instanceof Negation));
        }
    }

    public boolean isCompatible(Vertex candidateVertex) {
//      System.out.println("isCompatible METHOD START");

        VertexRefinedByFeatureExpression vertexRefinedByFeatureExpression = (VertexRefinedByFeatureExpression) candidateVertex;
//      System.out.println("vertexRefinedByFeatureExpression " + vertexRefinedByFeatureExpression.toString());
        FeatureExpression featureExpression = vertexRefinedByFeatureExpression.getFeatureExpression();

//      System.out.println("candidateVertex " + candidateVertex.toString());

        if (featureTruthValueMap.isEmpty()) {
            return true;
        }

        if (featureExpression instanceof Conjunction) {
            return isConjunctionCompatible((Conjunction) featureExpression);
        } else {
            return checkSingleExpressionCompatibility(featureExpression);
        }
//      System.out.println("result is " + result);
//      System.out.println("isCompatible METHOD FINISH");
    }

    // --- Optimization: Extracted logic for single expression check ---
    private boolean checkSingleExpressionCompatibility(FeatureExpression featureExpression) {
        Feature feature = featureExpression.getFeature();
        Boolean existingValue = featureTruthValueMap.get(feature);

        // If the feature is not in the map, it doesn't conflict with current truth values.
        if (existingValue == null) {
            return false; // Or true? Original logic implied strict check. Keeping strictly false if not found based on context.
            // NOTE: In original code: if not found, get() returns null. 
            // null == false -> false. null == true -> false. So it returned false.
            // Preserving original behavior: if key missing, return false.
        }

        // Logic: 
        // If Negation -> requires False.
        // If Not Negation -> requires True.
        boolean requiresTrue = !(featureExpression instanceof Negation);
        
        return existingValue.equals(requiresTrue);
    }

    public boolean isConjunctionCompatible(Conjunction conjunction) {
        // OPTIMIZATION: Fail Fast
        // If any single operand is NOT compatible, the whole conjunction fails.
        // No need to check the rest.
        for (FeatureExpression operand : conjunction.getOperands()) {
            
            // Inline check for performance (avoiding function call overhead if possible, but helper is cleaner)
            Feature feature = operand.getFeature();
            Boolean existingValue = featureTruthValueMap.get(feature);
            
            if (existingValue == null) {
                return false; // Missing key means incompatibility in this context
            }
            
            boolean requiresTrue = !(operand instanceof Negation);
            
            // If mismatch found, return false immediately
            if (!existingValue.equals(requiresTrue)) {
//              System.out.println("isConjunctionCompatible operand " + operand.toString());
//              System.out.println("operand.getFeature() " + operand.getFeature().getName() + " " + featureTruthValueMap.get(feature));
                return false;
            }
        }
        
        // If we survived the loop, everything is compatible
        return true;
    }
}