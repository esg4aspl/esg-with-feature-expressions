package tr.edu.iyte.esgfx.staticanalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sat4j.specs.ContradictionException;

import tr.edu.iyte.esgfx.api.FeatureModelCnf;
import tr.edu.iyte.esgfx.api.LoadedSplModel;

/**
 * The feature model formula Phi_d: clauses over the feature variables 1..n,
 * variable {@code i + 1} standing for feature {@code i} of the name list.
 *
 * <p>Two sources exist (see {@link PhiDSource}): the clauses of
 * {@code SATSolverGenerationFromFeatureModel.addSATClauses}, the encoding the
 * enumeration of Chapter 6 used, and the DIMACS file of the feature model,
 * which holds its CNF under the standard semantics ({@link DimacsPhiD}).
 *
 * <p>A configuration assigns every variable. A product is the restriction of a
 * configuration to the concrete variables, the features that are not abstract
 * in the feature model; products are what the analysis counts, enumerates and
 * reports as witnesses.
 */
public final class PhiD {

	private final String source;
	private final List<String> featureNames;
	private final Map<String, Integer> variableByName;
	private final List<int[]> clauses;
	private final boolean[] concrete;
	private final int[] concreteVariables;

	/** Every variable concrete; the source is "test". */
	public PhiD(List<String> featureNames, List<int[]> clauses) {
		this("test", featureNames, clauses, Collections.<String>emptySet());
	}

	/** @param abstractFeatures names of the abstract features; every other variable is concrete */
	public PhiD(String source, List<String> featureNames, List<int[]> clauses, Collection<String> abstractFeatures) {
		this.source = source;
		this.featureNames = Collections.unmodifiableList(new ArrayList<String>(featureNames));
		this.clauses = Collections.unmodifiableList(new ArrayList<int[]>(clauses));
		Map<String, Integer> byName = new LinkedHashMap<String, Integer>();
		for (int i = 0; i < featureNames.size(); i++) {
			if (byName.put(featureNames.get(i), i + 1) != null) {
				throw new IllegalArgumentException("Feature " + featureNames.get(i) + " occurs twice");
			}
		}
		this.variableByName = Collections.unmodifiableMap(byName);
		for (int[] clause : clauses) {
			for (int literal : clause) {
				if (literal == 0 || Math.abs(literal) > featureNames.size()) {
					throw new IllegalArgumentException("Clause literal " + literal + " outside 1.." + featureNames.size());
				}
			}
		}
		this.concrete = new boolean[featureNames.size() + 1];
		List<Integer> concreteList = new ArrayList<Integer>();
		for (int variable = 1; variable <= featureNames.size(); variable++) {
			concrete[variable] = !abstractFeatures.contains(featureNames.get(variable - 1));
			if (concrete[variable]) {
				concreteList.add(variable);
			}
		}
		this.concreteVariables = new int[concreteList.size()];
		for (int i = 0; i < concreteVariables.length; i++) {
			concreteVariables[i] = concreteList.get(i);
		}
	}

	/** The engine's clauses, every variable concrete. */
	public static PhiD of(LoadedSplModel model) throws ContradictionException {
		return of(FeatureModelCnf.of(model), Collections.<String>emptySet());
	}

	/** The engine's clauses with the given abstract features. */
	public static PhiD of(FeatureModelCnf cnf, Collection<String> abstractFeatures) {
		List<int[]> clauses = new ArrayList<int[]>();
		for (String line : cnf.toDimacs().split("\n")) {
			String trimmed = line.trim();
			if (trimmed.isEmpty() || trimmed.startsWith("c") || trimmed.startsWith("p")) {
				continue;
			}
			String[] tokens = trimmed.split("\\s+");
			// The last token is the DIMACS terminator; a 0 before it would be a real literal 0.
			int[] clause = new int[tokens.length - 1];
			for (int i = 0; i < clause.length; i++) {
				clause[i] = Integer.parseInt(tokens[i]);
				if (clause[i] == 0) {
					throw new IllegalStateException("Feature model clause contains literal 0: " + trimmed);
				}
			}
			clauses.add(clause);
		}
		return new PhiD(PhiDSource.ENGINE.label(), cnf.getFeatureNames(), clauses, abstractFeatures);
	}

	/** "engine", "dimacs", or "test" for a formula built in memory. */
	public String getSource() {
		return source;
	}

	public int getVariableCount() {
		return featureNames.size();
	}

	public List<String> getFeatureNames() {
		return featureNames;
	}

	public List<int[]> getClauses() {
		return clauses;
	}

	/** The variable of a feature, or null when the feature is not in the list. */
	public Integer variableOf(String featureName) {
		return variableByName.get(featureName);
	}

	/** The feature a variable stands for; variables are 1-based. */
	public String featureOf(int variable) {
		return featureNames.get(variable - 1);
	}

	public boolean isConcrete(int variable) {
		return concrete[variable];
	}

	/** The concrete variables in increasing order. */
	public int[] getConcreteVariables() {
		return concreteVariables.clone();
	}

	/** The selected concrete features of a solver model, in variable order. */
	public List<String> concreteSelection(int[] model) {
		List<String> selected = new ArrayList<String>();
		for (int literal : model) {
			if (literal > 0 && literal <= featureNames.size() && concrete[literal]) {
				selected.add(featureOf(literal));
			}
		}
		return selected;
	}

	/** The selected features of a solver model, abstract ones included, in variable order. */
	public List<String> selection(int[] model) {
		List<String> selected = new ArrayList<String>();
		for (int literal : model) {
			if (literal > 0 && literal <= featureNames.size()) {
				selected.add(featureOf(literal));
			}
		}
		return selected;
	}
}
