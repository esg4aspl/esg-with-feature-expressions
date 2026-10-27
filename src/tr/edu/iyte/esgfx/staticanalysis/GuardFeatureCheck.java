package tr.edu.iyte.esgfx.staticanalysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.sat4j.core.VecInt;
import org.sat4j.specs.ISolver;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.Conjunction;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.Negation;

/**
 * Products are counted over the concrete features, which is sound only if two
 * configurations that agree on the concrete features have the same projection.
 * This check establishes it: every feature named in a guard is a variable of
 * Phi_d, and is concrete or forced true by Phi_d. A violation fails the run.
 */
public final class GuardFeatureCheck {

	/** Guard features that are variables of Phi_d. */
	public final Set<String> guardFeatures;
	/** Guard features that are abstract; each is forced true by Phi_d. */
	public final Set<String> abstractForced;

	private GuardFeatureCheck(Set<String> guardFeatures, Set<String> abstractForced) {
		this.guardFeatures = guardFeatures;
		this.abstractForced = abstractForced;
	}

	public static GuardFeatureCheck check(String modelName, ESG esg, PhiD phiD) throws Exception {
		Set<String> names = new TreeSet<String>();
		for (Vertex vertex : esg.getVertexList()) {
			if (vertex.isPseudoStartVertex() || vertex.isPseudoEndVertex()) {
				continue;
			}
			collect(((VertexRefinedByFeatureExpression) vertex).getFeatureExpression(), names);
		}
		List<String> missing = new ArrayList<String>();
		Set<String> abstractForced = new TreeSet<String>();
		ISolver solver = ProductEnumerator.solverFor(phiD);
		for (String name : names) {
			Integer variable = phiD.variableOf(name);
			if (variable == null) {
				missing.add(name);
			} else if (!phiD.isConcrete(variable)) {
				if (solver != null && solver.isSatisfiable(new VecInt(new int[] { -variable }))) {
					throw new IllegalStateException(modelName + " (" + phiD.getSource() + "): guard feature " + name
							+ " is abstract and not forced by Phi_d, so products over concrete features are not enough");
				}
				abstractForced.add(name);
			}
		}
		if (!missing.isEmpty()) {
			throw new IllegalStateException(modelName + " (" + phiD.getSource() + "): guard features " + missing
					+ " are not variables of Phi_d");
		}
		return new GuardFeatureCheck(names, abstractForced);
	}

	private static void collect(FeatureExpression expression, Set<String> names) {
		if (expression instanceof Conjunction) {
			for (FeatureExpression operand : ((Conjunction) expression).getOperands()) {
				collect(operand, names);
			}
		} else if (expression instanceof Negation) {
			collect(((Negation) expression).getFeatureExpression(), names);
		} else if (expression.getClass() == FeatureExpression.class) {
			names.add(expression.getFeature().getName());
		} else {
			throw new UnsupportedOperationException("Guard of class " + expression.getClass().getSimpleName()
					+ "; the static analysis supports literals and conjunctions of literals only");
		}
	}
}
