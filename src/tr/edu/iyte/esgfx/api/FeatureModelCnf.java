package tr.edu.iyte.esgfx.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.IVecInt;
import org.sat4j.tools.SolverDecorator;

import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;

/**
 * A feature model as CNF, in the form an external SAT-based sampler reads.
 *
 * <p>The clauses are not rebuilt here. They are recorded as
 * {@link SATSolverGenerationFromFeatureModel} hands them to a solver, which is
 * the same constraint set {@code countValidConfigurations} and the
 * all-products enumeration run against — so the CNF cannot drift away from what
 * the rest of the API calls a valid configuration.
 *
 * <p>Variable {@code i + 1} is the feature at index {@code i} of the feature
 * expression list, matching how a solver model is read back into truth values.
 */
public final class FeatureModelCnf {

	private final List<int[]> clauses;
	private final List<String> featureNames;

	private FeatureModelCnf(List<int[]> clauses, List<String> featureNames) {
		this.clauses = clauses;
		this.featureNames = featureNames;
	}

	public static FeatureModelCnf of(LoadedSplModel model) throws ContradictionException {
		synchronized (model) {
			Map<String, FeatureExpression> featureExpressionMap = model.getFeatureExpressionMap();
			List<FeatureExpression> featureExpressionList =
					SingleProductTestGenerationAPI.buildFeatureExpressionList(featureExpressionMap);

			ClauseRecorder recorder = new ClauseRecorder(SolverFactory.newDefault());
			new SATSolverGenerationFromFeatureModel().addSATClauses(recorder, model.getFeatureModel(),
					featureExpressionMap, featureExpressionList);

			List<String> names = new ArrayList<String>(featureExpressionList.size());
			for (FeatureExpression featureExpression : featureExpressionList) {
				names.add(featureExpression.getFeature().getName());
			}
			return new FeatureModelCnf(recorder.getClauses(), names);
		}
	}

	public int getVariableCount() {
		return featureNames.size();
	}

	public List<String> getFeatureNames() {
		return featureNames;
	}

	/** The feature a variable stands for; variables are 1-based. */
	public String featureOf(int variable) {
		return featureNames.get(variable - 1);
	}

	/**
	 * DIMACS CNF with the sampling set named on a {@code c ind} line. Every
	 * variable is a feature — there are no auxiliary variables — so the sampling
	 * set is all of them, and a sample is already a whole configuration.
	 */
	public String toDimacs() {
		StringBuilder text = new StringBuilder();
		text.append("c DIMACS generated from the feature model's SAT clauses\n");

		text.append("c ind");
		for (int variable = 1; variable <= getVariableCount(); variable++) {
			text.append(' ').append(variable);
		}
		text.append(" 0\n");

		text.append("p cnf ").append(getVariableCount()).append(' ').append(clauses.size()).append('\n');
		for (int[] clause : clauses) {
			for (int literal : clause) {
				text.append(literal).append(' ');
			}
			text.append("0\n");
		}
		return text.toString();
	}

	/** Turns one sampled assignment into the truth-value map the generation API takes. */
	public Map<String, Boolean> selectionOf(int[] assignment) {
		Map<String, Boolean> selection = new java.util.LinkedHashMap<String, Boolean>();
		for (int literal : assignment) {
			int variable = Math.abs(literal);
			if (variable >= 1 && variable <= getVariableCount()) {
				selection.put(featureOf(variable), literal > 0);
			}
		}
		return selection;
	}

	/** Passes every clause through to a real solver while keeping a copy. */
	private static final class ClauseRecorder extends SolverDecorator<ISolver> {

		private static final long serialVersionUID = 1L;

		private final List<int[]> clauses = new ArrayList<int[]>();

		ClauseRecorder(ISolver decorated) {
			super(decorated);
		}

		@Override
		public IConstr addClause(IVecInt literals) throws ContradictionException {
			int[] copy = new int[literals.size()];
			for (int i = 0; i < literals.size(); i++) {
				copy[i] = literals.get(i);
			}
			clauses.add(copy);
			return super.addClause(literals);
		}

		List<int[]> getClauses() {
			return clauses;
		}
	}
}
