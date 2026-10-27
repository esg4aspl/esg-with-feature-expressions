package tr.edu.iyte.esgfx.staticanalysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.sat4j.core.VecInt;
import org.sat4j.specs.ISolver;

import tr.edu.iyte.esgfx.api.LoadedSplModel;
import tr.edu.iyte.esgfx.model.featuremodel.Feature;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;

/**
 * Step 0 for both sources of Phi_d (brief, Section 3a): per product line the
 * number of configurations (models over all variables) and of products
 * (distinct assignments to the concrete variables), the guard-feature check,
 * the UniGen subset check, and the set-equality check between the DIMACS and
 * the engine's clauses.
 *
 * <p>Usage: {@code PhiDSourceCheck [model ...]}; all eight when none is named.
 */
public final class PhiDSourceCheck {

	/** Above this many variables the solver-free cross-check of the counts is skipped. */
	private static final int EXHAUSTIVE_LIMIT = 24;

	public static void main(String[] args) throws Exception {
		for (CaseStudy study : CaseStudy.select(Arrays.asList(args))) {
			LoadedSplModel model = study.load();
			System.out.println("== " + study.name);
			PhiD engine = PhiDSource.ENGINE.load(study, model);
			PhiD dimacs = PhiDSource.DIMACS.load(study, model);
			for (PhiD phiD : Arrays.asList(engine, dimacs)) {
				counts(study, model, phiD);
			}
			if (study.samples().exists()) {
				for (PhiD phiD : Arrays.asList(engine, dimacs)) {
					samples(study, phiD);
				}
			}
			// Hockerty: the per-clause parts (a) and (b) only; its models cannot be enumerated.
			setEquality(model, engine, dimacs, study.isEnumerable());
		}
	}

	// ------------------------------------------------------------- counts

	private static void counts(CaseStudy study, LoadedSplModel model, PhiD phiD) throws Exception {
		GuardFeatureCheck guards = GuardFeatureCheck.check(study.name, model.getEsgFx(), phiD);
		System.out.println("  [" + phiD.getSource() + "] variables " + phiD.getVariableCount() + ", concrete "
				+ phiD.getConcreteVariables().length + ", clauses " + phiD.getClauses().size() + "; guard features "
				+ guards.guardFeatures.size() + ", abstract and forced true: " + guards.abstractForced);
		if (!study.isEnumerable()) {
			return;
		}
		long started = System.nanoTime();
		long configurations = ProductEnumerator.configurations(phiD, null, ProductEnumerator.NO_LIMIT);
		long configurationsMs = (System.nanoTime() - started) / 1000000;
		started = System.nanoTime();
		long products = ProductEnumerator.products(phiD, null, ProductEnumerator.NO_LIMIT);
		long productsMs = (System.nanoTime() - started) / 1000000;
		String expected = phiD.getSource().equals(PhiDSource.DIMACS.label())
				? "expected " + study.configurations + " / " + study.products
				: "Chapter 6 " + study.chapter6;
		System.out.println("  [" + phiD.getSource() + "] configurations " + configurations + " (" + configurationsMs
				+ " ms), products " + products + " (" + productsMs + " ms); " + expected);
		if (phiD.getSource().equals(PhiDSource.DIMACS.label())
				&& (configurations != study.configurations || products != study.products)) {
			System.out.println("  [" + phiD.getSource() + "] MISMATCH with the expected counts");
		}
		if (phiD.getVariableCount() <= EXHAUSTIVE_LIMIT) {
			long[] exhaustive = exhaustiveCounts(phiD);
			System.out.println("  [" + phiD.getSource() + "] solver-free cross-check over 2^"
					+ phiD.getVariableCount() + " assignments: configurations " + exhaustive[0] + ", products "
					+ exhaustive[1] + (exhaustive[0] == configurations && exhaustive[1] == products ? " (agree)"
							: " (DISAGREE)"));
		}
	}

	/** Configurations and products by trying every assignment, no solver involved. */
	static long[] exhaustiveCounts(PhiD phiD) {
		int n = phiD.getVariableCount();
		long concreteMask = 0;
		for (int variable : phiD.getConcreteVariables()) {
			concreteMask |= 1L << (variable - 1);
		}
		long configurations = 0;
		Set<Long> products = new HashSet<Long>();
		for (long bits = 0; bits < (1L << n); bits++) {
			if (satisfies(phiD.getClauses(), bits)) {
				configurations++;
				products.add(bits & concreteMask);
			}
		}
		return new long[] { configurations, products.size() };
	}

	private static boolean satisfies(List<int[]> clauses, long bits) {
		for (int[] clause : clauses) {
			boolean satisfied = false;
			for (int literal : clause) {
				if (((bits >> (Math.abs(literal) - 1)) & 1L) == (literal > 0 ? 1L : 0L)) {
					satisfied = true;
					break;
				}
			}
			if (!satisfied) {
				return false;
			}
		}
		return true;
	}

	// ------------------------------------------------------------- samples

	/** Every UniGen sample (DIMACS ids) checked against Phi_d with all its variables fixed. */
	private static void samples(CaseStudy study, PhiD phiD) throws Exception {
		TreeMap<Integer, String> mapping = DimacsPhiD.readMapping(study.dimacsMapping());
		ISolver solver = ProductEnumerator.solverFor(phiD);
		int total = 0;
		int accepted = 0;
		List<Integer> rejected = new ArrayList<Integer>();
		Set<List<String>> products = new LinkedHashSet<List<String>>();
		try (BufferedReader reader = new BufferedReader(new FileReader(study.samples()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().isEmpty()) {
					continue;
				}
				total++;
				VecInt assumptions = new VecInt();
				Set<Integer> fixed = new HashSet<Integer>();
				for (String token : line.trim().split("\\s+")) {
					int literal = Integer.parseInt(token);
					if (literal == 0) {
						continue;
					}
					Integer variable = phiD.variableOf(mapping.get(Math.abs(literal)));
					if (variable != null) {
						assumptions.push(literal > 0 ? variable : -variable);
						fixed.add(variable);
					}
				}
				if (fixed.size() != phiD.getVariableCount()) {
					throw new IllegalStateException(study.name + " sample " + total + " fixes " + fixed.size() + " of "
							+ phiD.getVariableCount() + " variables");
				}
				if (solver != null && solver.isSatisfiable(assumptions)) {
					accepted++;
				} else {
					rejected.add(total);
				}
				int[] model = new int[assumptions.size()];
				for (int i = 0; i < model.length; i++) {
					model[i] = assumptions.get(i);
				}
				products.add(phiD.concreteSelection(sorted(model)));
			}
		}
		System.out.println("  [" + phiD.getSource() + "] UniGen samples accepted " + accepted + " / " + total
				+ (rejected.isEmpty() ? "" : ", rejected lines " + rejected) + "; distinct products among samples "
				+ products.size());
	}

	private static int[] sorted(int[] literals) {
		Integer[] boxed = new Integer[literals.length];
		for (int i = 0; i < literals.length; i++) {
			boxed[i] = literals[i];
		}
		Arrays.sort(boxed, (a, b) -> Integer.compare(Math.abs(a), Math.abs(b)));
		int[] result = new int[literals.length];
		for (int i = 0; i < literals.length; i++) {
			result[i] = boxed[i];
		}
		return result;
	}

	// -------------------------------------------------------- set equality

	/**
	 * E = the engine's clauses, D = the DIMACS, x = the clause of
	 * {@code addOptionalConcreteFeatureClauses}. Checks E == D and x projected
	 * onto the engine's variables: every clause of E is implied by D and x, and
	 * every model of E extends to a model of D and x. The DIMACS clauses over
	 * shared variables only are also checked one by one against E.
	 */
	private static void setEquality(LoadedSplModel model, PhiD engine, PhiD dimacs, boolean enumerate)
			throws Exception {
		int[] toDimacs = new int[engine.getVariableCount() + 1];
		for (int variable = 1; variable <= engine.getVariableCount(); variable++) {
			Integer d = dimacs.variableOf(engine.featureOf(variable));
			if (d == null) {
				throw new IllegalStateException("Engine variable " + engine.featureOf(variable) + " is not in the DIMACS");
			}
			toDimacs[variable] = d;
		}
		List<int[]> e = new ArrayList<int[]>();
		for (int[] clause : engine.getClauses()) {
			int[] translated = new int[clause.length];
			for (int i = 0; i < clause.length; i++) {
				translated[i] = clause[i] > 0 ? toDimacs[clause[i]] : -toDimacs[-clause[i]];
			}
			e.add(translated);
		}
		Set<String> xNames = extraClauseFeatures(model);
		int[] x = null;
		int xCount = 0;
		for (int[] clause : e) {
			Set<String> names = new TreeSet<String>();
			boolean positive = true;
			for (int literal : clause) {
				positive &= literal > 0;
				names.add(dimacs.featureOf(Math.abs(literal)));
			}
			if (positive && names.equals(xNames)) {
				x = clause;
				xCount++;
			}
		}
		System.out.println("  [set equality] engine clauses " + e.size() + ", DIMACS clauses "
				+ dimacs.getClauses().size() + ", shared variables " + engine.getVariableCount() + " of "
				+ dimacs.getVariableCount() + "; extra clause x = " + (xNames.isEmpty() ? "none" : "OR" + xNames)
				+ (xNames.isEmpty() ? "" : xCount == 1 ? " (found once among the engine's clauses)"
						: " (found " + xCount + " times)"));
		if (!xNames.isEmpty() && xCount != 1) {
			throw new IllegalStateException("Extra clause not identified uniquely");
		}

		List<int[]> dx = new ArrayList<int[]>(dimacs.getClauses());
		if (x != null) {
			dx.add(x);
		}
		ISolver dSolver = solverOf(dimacs.getVariableCount(), dimacs.getClauses());
		ISolver dxSolver = solverOf(dimacs.getVariableCount(), dx);
		ISolver eSolver = solverOf(dimacs.getVariableCount(), e);

		// (a) every clause of E implied by D and x; which are not implied by D alone.
		List<String> notByDx = new ArrayList<String>();
		List<String> notByD = new ArrayList<String>();
		for (int[] clause : e) {
			if (!implied(dxSolver, clause)) {
				notByDx.add(names(dimacs, clause));
			}
			if (!implied(dSolver, clause)) {
				notByD.add(names(dimacs, clause));
			}
		}
		System.out.println("  [set equality] (a) engine clauses not implied by D and x: " + notByDx.size()
				+ (notByDx.isEmpty() ? "" : " " + notByDx) + "; not implied by D alone: " + notByD.size()
				+ (notByD.isEmpty() ? "" : " " + notByD));

		// (b) DIMACS clauses over shared variables, one by one against E.
		Set<Integer> shared = new HashSet<Integer>();
		for (int variable = 1; variable <= engine.getVariableCount(); variable++) {
			shared.add(toDimacs[variable]);
		}
		int sharedOnly = 0;
		List<String> notByE = new ArrayList<String>();
		for (int[] clause : dimacs.getClauses()) {
			boolean onlyShared = true;
			for (int literal : clause) {
				onlyShared &= shared.contains(Math.abs(literal));
			}
			if (onlyShared) {
				sharedOnly++;
				if (!implied(eSolver, clause)) {
					notByE.add(names(dimacs, clause));
				}
			}
		}
		System.out.println("  [set equality] (b) DIMACS clauses over shared variables only: " + sharedOnly
				+ ", not implied by E: " + notByE.size() + (notByE.isEmpty() ? "" : " " + notByE)
				+ "; DIMACS clauses with a DIMACS-only variable: " + (dimacs.getClauses().size() - sharedOnly));
		if (!enumerate) {
			return;
		}

		// (c) every model of E extends to a model of D and x, and the projection of D and x
		// onto the shared variables has as many elements as E has models.
		int[] sharedVariables = new int[shared.size()];
		int k = 0;
		for (int variable = 1; variable <= engine.getVariableCount(); variable++) {
			sharedVariables[k++] = toDimacs[variable];
		}
		final ISolver extension = solverOf(dimacs.getVariableCount(), dx);
		final long[] notExtending = new long[1];
		long eModels = ProductEnumerator.enumerate(solverOf(dimacs.getVariableCount(), e), new int[0],
				sharedVariables, modelOfE -> {
					VecInt assumptions = new VecInt();
					for (int literal : modelOfE) {
						if (shared.contains(Math.abs(literal))) {
							assumptions.push(literal);
						}
					}
					if (!extension.isSatisfiable(assumptions)) {
						notExtending[0]++;
					}
				}, ProductEnumerator.NO_LIMIT);
		long dxProjected = ProductEnumerator.enumerate(solverOf(dimacs.getVariableCount(), dx), new int[0],
				sharedVariables, null, ProductEnumerator.NO_LIMIT);
		boolean equal = notByDx.isEmpty() && notExtending[0] == 0 && eModels == dxProjected;
		System.out.println("  [set equality] (c) models of E over its variables " + eModels
				+ ", of which not extending to D and x: " + notExtending[0]
				+ "; projections of D and x onto them: " + dxProjected + " => E "
				+ (equal ? "==" : "!=") + " (D and x) projected onto the engine's variables");
	}

	/**
	 * The features of the clause {@code addOptionalConcreteFeatureClauses} adds,
	 * selected as {@code SATSolverGenerationFromFeatureModel.addFeatureClauses}
	 * selects them: children of OR groups (mandatory or optional) and optional
	 * concrete features whose parent is an AND feature.
	 */
	static Set<String> extraClauseFeatures(LoadedSplModel model) {
		FeatureModel featureModel = model.getFeatureModel();
		Set<Feature> features = new LinkedHashSet<Feature>(featureModel.getFeatureSet());
		features.addAll(featureModel.getORFeatures().keySet());
		features.addAll(featureModel.getXORFeatures().keySet());
		Set<String> names = new TreeSet<String>();
		for (Feature feature : features) {
			if (featureModel.isORFeature(feature)) {
				for (Feature child : featureModel.getChildORFeatures(feature)) {
					names.add(child.getName());
				}
			} else if (!feature.isMandatory() && !featureModel.isXORFeature(feature) && !feature.isAbstract()
					&& feature.getParent() != null && featureModel.isANDFeature(feature.getParent())) {
				names.add(feature.getName());
			}
		}
		return names;
	}

	private static ISolver solverOf(int variables, List<int[]> clauses) throws Exception {
		ISolver solver = org.sat4j.minisat.SolverFactory.newDefault();
		solver.newVar(variables);
		for (int[] clause : clauses) {
			solver.addClause(new VecInt(clause));
		}
		return solver;
	}

	private static boolean implied(ISolver solver, int[] clause) throws Exception {
		int[] negated = new int[clause.length];
		for (int i = 0; i < clause.length; i++) {
			negated[i] = -clause[i];
		}
		return !solver.isSatisfiable(new VecInt(negated));
	}

	private static String names(PhiD phiD, int[] clause) {
		StringBuilder text = new StringBuilder("(");
		for (int i = 0; i < clause.length; i++) {
			text.append(i == 0 ? "" : " | ").append(clause[i] < 0 ? "!" : "")
					.append(phiD.featureOf(Math.abs(clause[i])));
		}
		return text.append(")").toString();
	}
}
