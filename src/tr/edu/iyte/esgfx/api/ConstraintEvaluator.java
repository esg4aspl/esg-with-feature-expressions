package tr.edu.iyte.esgfx.api;

import java.util.Map;

import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featuremodel.Connector;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;
import tr.edu.iyte.esgfx.model.featuremodel.Implicant;
import tr.edu.iyte.esgfx.model.featuremodel.Implication;
import tr.edu.iyte.esgfx.model.featuremodel.Negation;

/**
 * Evaluates a feature model's cross-tree constraints against a selection.
 *
 * <p>{@code ProductConfigurationValidator} checks only the feature tree —
 * mandatory features, or-groups, alternative-groups. The cross-tree rules
 * (requires, excludes, and the richer implications, bi-implications and
 * and/or connectors a loaded model can carry) live in the feature model's
 * constraint sets and are not checked there, so a selection that breaks one —
 * choosing a feature whose {@code requires} target is left out — was being
 * reported valid. This evaluates those rules over the same truth values the
 * tree check uses, so the two agree with what the SAT-based enumeration already
 * enforces when it counts and samples configurations.
 */
final class ConstraintEvaluator {

    private ConstraintEvaluator() {
    }

    static boolean allSatisfied(FeatureModel model, Map<String, FeatureExpression> selection) {
        for (Implication implication : model.getImpConstraints()) {
            // A requires B, and A excludes B (B negated), are both implications.
            if (evaluate(implication.getLeftHandSide(), selection)
                    && !evaluate(implication.getRightHandSide(), selection)) {
                return false;
            }
        }
        for (Implication iff : model.getIffConstraints()) {
            if (evaluate(iff.getLeftHandSide(), selection)
                    != evaluate(iff.getRightHandSide(), selection)) {
                return false;
            }
        }
        for (Connector connector : model.getConnConstraints()) {
            if (!evaluate(connector, selection)) {
                return false;
            }
        }
        return true;
    }

    private static boolean evaluate(Implicant implicant, Map<String, FeatureExpression> selection) {
        // Negation extends Feature, so it must be tested first.
        if (implicant instanceof Negation) {
            String name = ((Negation) implicant).getName();
            return !isSelected(name.startsWith("!") ? name.substring(1) : name, selection);
        }
        if (implicant instanceof Connector) {
            Connector connector = (Connector) implicant;
            boolean isAnd = "AND".equals(connector.getOperator());
            boolean result = isAnd;
            for (Implicant child : connector.getImplicantSet()) {
                boolean value = evaluate(child, selection);
                result = isAnd ? (result && value) : (result || value);
            }
            return result;
        }
        if (implicant instanceof tr.edu.iyte.esgfx.model.featuremodel.Feature) {
            return isSelected(((tr.edu.iyte.esgfx.model.featuremodel.Feature) implicant).getName(), selection);
        }
        // An implicant shape this does not model must not cause a false rejection.
        return true;
    }

    private static boolean isSelected(String featureName, Map<String, FeatureExpression> selection) {
        FeatureExpression expression = selection.get(featureName);
        return expression != null && expression.evaluate();
    }
}
