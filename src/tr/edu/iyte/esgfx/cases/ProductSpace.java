package tr.edu.iyte.esgfx.cases;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.staticanalysis.DimacsPhiD;
import tr.edu.iyte.esgfx.staticanalysis.PhiD;
import tr.edu.iyte.esgfx.staticanalysis.ProductEnumerator;

/**
 * The products of a product line, enumerated from the DIMACS file of its
 * feature model.
 *
 * The clauses are those of the DIMACS file (the semantics of the feature
 * model), together with one unit clause for every feature named as
 * mandatory by the case study. Two configurations that differ only in
 * abstract features have the same product-level model, so a product is a
 * distinct assignment to the concrete features: after each product a
 * clause over the concrete variables is added that excludes it.
 *
 * next() also sets the truth value of every feature expression of the
 * family-level model to the value the product gives its feature.
 */
public final class ProductSpace {

	private final PhiD phiD;
	private final ISolver solver;
	private final int[] concreteVariables;
	private final FeatureExpression[] expressionOfVariable;
	private int[] model;
	private boolean exhausted;

	private ProductSpace(PhiD phiD, ISolver solver, FeatureExpression[] expressionOfVariable) {
		this.phiD = phiD;
		this.solver = solver;
		this.concreteVariables = phiD.getConcreteVariables();
		this.expressionOfVariable = expressionOfVariable;
	}

	public static ProductSpace open(String dimacsFile, String mappingFile, String featureModelFile,
			Map<String, FeatureExpression> featureExpressionMap, String... mandatoryFeatures) throws Exception {
		PhiD phiD = DimacsPhiD.read(new File(dimacsFile), new File(mappingFile), new File(featureModelFile));
		ISolver solver = ProductEnumerator.solverFor(phiD);
		if (solver == null) {
			throw new IllegalStateException("The clauses of " + dimacsFile + " are contradictory");
		}
		if (mandatoryFeatures != null) {
			for (String feature : mandatoryFeatures) {
				Integer variable = phiD.variableOf(feature);
				if (variable == null) {
					throw new IllegalStateException("Mandatory feature " + feature + " is not in " + mappingFile);
				}
				solver.addClause(new VecInt(new int[] { variable }));
			}
		}
		FeatureExpression[] expressionOfVariable = new FeatureExpression[phiD.getVariableCount() + 1];
		List<String> unknown = new ArrayList<>();
		for (Map.Entry<String, FeatureExpression> entry : featureExpressionMap.entrySet()) {
			String name = entry.getKey();
			if (name.contains("!") || entry.getValue() == null) {
				continue;
			}
			Integer variable = phiD.variableOf(name);
			if (variable == null) {
				unknown.add(name);
			} else {
				expressionOfVariable[variable] = entry.getValue();
			}
		}
		if (!unknown.isEmpty()) {
			throw new IllegalStateException(
					"Features of the family-level model that are not in " + mappingFile + ": " + unknown);
		}
		return new ProductSpace(phiD, solver, expressionOfVariable);
	}

	/**
	 * Moves to the next product. Returns false when there is none left. The
	 * product found is excluded only when exclude() is called.
	 */
	public boolean next() {
		if (exhausted) {
			return false;
		}
		boolean satisfiable;
		try {
			satisfiable = solver.isSatisfiable();
		} catch (TimeoutException e) {
			throw new IllegalStateException(e);
		}
		if (!satisfiable) {
			exhausted = true;
			return false;
		}
		// Sat4j leaves out a variable that occurs in no clause; it is completed with false.
		int[] full = new int[phiD.getVariableCount()];
		for (int variable = 1; variable <= full.length; variable++) {
			full[variable - 1] = -variable;
		}
		for (int literal : solver.model()) {
			full[Math.abs(literal) - 1] = literal;
		}
		model = full;
		for (int variable = 1; variable <= full.length; variable++) {
			if (expressionOfVariable[variable] != null) {
				expressionOfVariable[variable].setTruthValue(full[variable - 1] > 0);
			}
		}
		return true;
	}

	/** Excludes the product found by the last call of next(). */
	public void exclude() {
		if (concreteVariables.length == 0) {
			exhausted = true;
			return;
		}
		VecInt blocking = new VecInt(concreteVariables.length);
		for (int variable : concreteVariables) {
			blocking.push(-model[variable - 1]);
		}
		try {
			solver.addClause(blocking);
		} catch (ContradictionException e) {
			exhausted = true;
		}
	}

	/** The concrete features selected in the product found by the last call of next(). */
	public List<String> selectedConcreteFeatures() {
		return phiD.concreteSelection(model);
	}

	public int getVariableCount() {
		return phiD.getVariableCount();
	}

	public int getConcreteVariableCount() {
		return concreteVariables.length;
	}
}
