package tr.edu.iyte.esgfx.staticanalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.tools.ModelIterator;

import tr.edu.iyte.esgfx.api.FeatureModelCnf;
import tr.edu.iyte.esgfx.api.LoadedSplModel;
import tr.edu.iyte.esgfx.api.SingleProductTestGenerationAPI;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.productconfigurationgeneration.ProductConfigurationValidator;

/**
 * Step 0 of the static analysis: decides whether the clauses of
 * {@code SATSolverGenerationFromFeatureModel} alone denote the configurations
 * the enumeration produced, or whether the enumeration's validator filter
 * removes some of them.
 *
 * <p>For every enumerable model it counts (a) the models of the clauses alone
 * and (b) the models the enumeration keeps after
 * {@link ProductConfigurationValidator}, and compares both with the product
 * count of the thesis. For Tesla and syngo.via it also checks that every UniGen
 * sample satisfies the clauses.
 */
public class PhiDCountCheck {

	static final class Case {
		final String name;
		final String folder;
		final String prefix;
		final long thesisCount;

		Case(String name, String folder, String prefix, long thesisCount) {
			this.name = name;
			this.folder = folder;
			this.prefix = prefix;
			this.thesisCount = thesisCount;
		}
	}

	private static final long NOT_ENUMERABLE = -1;

	private static final Case[] CASES = new Case[] {
			new Case("SVM", "files/Cases/SodaVendingMachine/", "SVM", 12),
			new Case("eMail", "files/Cases/eMail/", "eM", 23),
			new Case("Elevator", "files/Cases/Elevator/", "El", 42),
			new Case("BankAccount", "files/Cases/BankAccountv2/", "BAv2", 498),
			new Case("StudentAttendance", "files/Cases/StudentAttendanceSystem/", "SAS", 2664),
			new Case("Tesla", "files/Cases/Tesla/", "Te", 426672),
			new Case("syngovia", "files/Cases/syngovia/", "Svia", 47970),
			// Not enumerable: only the UniGen samples are checked.
			new Case("HockertyShirts", "files/Cases/HockertyShirts/", "HS", NOT_ENUMERABLE) };

	public static void main(String[] args) throws Exception {
		Set<String> only = new LinkedHashSet<String>();
		for (String arg : args) {
			only.add(arg);
		}
		for (Case c : CASES) {
			if (!only.isEmpty() && !only.contains(c.name)) {
				continue;
			}
			check(c);
		}
	}

	private static void check(Case c) throws Exception {
		LoadedSplModel model = SingleProductTestGenerationAPI.load(c.folder + "configs/model.xml",
				c.folder + c.prefix + "_ESGFx.mxe");
		FeatureModelCnf cnf = FeatureModelCnf.of(model);
		List<int[]> clauses = clausesOf(cnf);
		int variables = cnf.getVariableCount();

		int zeroLiteralClauses = 0;
		int maxVariable = 0;
		for (int[] clause : clauses) {
			for (int literal : clause) {
				if (literal == 0) {
					zeroLiteralClauses++;
					break;
				}
				maxVariable = Math.max(maxVariable, Math.abs(literal));
			}
		}

		if (c.thesisCount == NOT_ENUMERABLE) {
			System.out.println("== " + c.name);
			System.out.println("  feature variables:        " + variables + " (largest variable in a clause: "
					+ maxVariable + ")");
			System.out.println("  clauses:                  " + clauses.size()
					+ (zeroLiteralClauses > 0 ? " (" + zeroLiteralClauses + " contain literal 0)" : ""));
			checkSamples(cnf, clauses, new File(c.folder + "configs/" + c.prefix + "_400.samples"),
					new File(c.folder + "configs/" + c.prefix + "_dimacsmapping.txt"));
			return;
		}

		// (a) models of the clauses alone, every feature variable declared.
		long start = System.nanoTime();
		Set<String> clauseModels = enumerate(clauses, variables, cnf);
		long clauseMs = (System.nanoTime() - start) / 1000000;

		// (b) the enumeration of Chapter 6: same solver set-up, validator filter.
		start = System.nanoTime();
		Map<String, Boolean> rejectedByValidator = new LinkedHashMap<String, Boolean>();
		long enumerationCount = enumerateAsChapter6(model, rejectedByValidator);
		long enumerationMs = (System.nanoTime() - start) / 1000000;

		System.out.println("== " + c.name);
		System.out.println("  feature variables:        " + variables + " (largest variable in a clause: "
				+ maxVariable + ")");
		System.out.println("  clauses:                  " + clauses.size()
				+ (zeroLiteralClauses > 0 ? " (" + zeroLiteralClauses + " contain literal 0)" : ""));
		System.out.println("  clause-only count:        " + clauseModels.size() + " (" + clauseMs + " ms)");
		System.out.println("  enumeration + validator:  " + enumerationCount + " (" + enumerationMs + " ms)");
		System.out.println("  thesis count:             " + c.thesisCount);
		System.out.println("  rejected by validator:    " + rejectedByValidator.size());
		int shown = 0;
		for (String rejected : rejectedByValidator.keySet()) {
			if (shown++ == 10) {
				System.out.println("    ...");
				break;
			}
			System.out.println("    " + rejected);
		}

		File samples = new File(c.folder + "configs/" + c.prefix + "_400.samples");
		File mapping = new File(c.folder + "configs/" + c.prefix + "_dimacsmapping.txt");
		if (samples.exists() && mapping.exists()) {
			checkSamples(cnf, clauses, samples, mapping);
		}
	}

	private static List<int[]> clausesOf(FeatureModelCnf cnf) {
		List<int[]> clauses = new ArrayList<int[]>();
		for (String line : cnf.toDimacs().split("\n")) {
			String trimmed = line.trim();
			if (trimmed.isEmpty() || trimmed.startsWith("c") || trimmed.startsWith("p")) {
				continue;
			}
			String[] tokens = trimmed.split("\\s+");
			// The DIMACS line ends in the terminating 0; a 0 before it is a real literal 0.
			int[] clause = new int[tokens.length - 1];
			for (int i = 0; i < clause.length; i++) {
				clause[i] = Integer.parseInt(tokens[i]);
			}
			clauses.add(clause);
		}
		return clauses;
	}

	private static Set<String> enumerate(List<int[]> clauses, int variables, FeatureModelCnf cnf)
			throws ContradictionException {
		Set<String> configurations = new LinkedHashSet<String>();
		ISolver solver = new ModelIterator(SolverFactory.newDefault());
		solver.newVar(variables);
		try {
			for (int[] clause : clauses) {
				solver.addClause(new VecInt(clause));
			}
		} catch (ContradictionException e) {
			return configurations;
		}
		try {
			while (solver.isSatisfiable()) {
				configurations.add(describe(solver.model(), cnf));
			}
		} catch (org.sat4j.specs.TimeoutException e) {
			throw new IllegalStateException(e);
		}
		return configurations;
	}

	/**
	 * Replicates {@code AutomaticProductConfigurationGenerator}: truth values are
	 * read from the model by index, the model is blocked, then the validator
	 * decides whether the configuration counts.
	 */
	private static long enumerateAsChapter6(LoadedSplModel loaded, Map<String, Boolean> rejected)
			throws Exception {
		Map<String, FeatureExpression> map = loaded.getFeatureExpressionMap();
		List<FeatureExpression> list = new ArrayList<FeatureExpression>();
		for (Map.Entry<String, FeatureExpression> entry : map.entrySet()) {
			if (!entry.getKey().contains("!") && entry.getValue() != null
					&& entry.getValue().getFeature().getName() != null) {
				list.add(entry.getValue());
			}
		}
		ISolver solver = new ModelIterator(SolverFactory.newDefault());
		new tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel()
				.addSATClauses(solver, loaded.getFeatureModel(), map, list);
		ProductConfigurationValidator validator = new ProductConfigurationValidator();

		long count = 0;
		while (solver.isSatisfiable()) {
			int[] m = solver.model();
			if (m.length != list.size()) {
				System.out.println("  WARNING: model length " + m.length + " differs from feature list size "
						+ list.size());
			}
			TreeSet<String> selected = new TreeSet<String>();
			for (int i = 0; i < m.length; i++) {
				list.get(i).setTruthValue(m[i] > 0);
				if (m[i] > 0) {
					selected.add(list.get(i).getFeature().getName());
				}
			}
			VecInt blocking = new VecInt();
			for (int i = 0; i < m.length; i++) {
				blocking.push(-m[i]);
			}
			boolean exhausted = false;
			try {
				solver.addClause(blocking);
			} catch (ContradictionException e) {
				exhausted = true;
			}
			if (validator.validate(loaded.getFeatureModel(), map)) {
				count++;
			} else {
				rejected.put(selected.toString(), Boolean.TRUE);
			}
			if (exhausted) {
				break;
			}
		}
		return count;
	}

	private static String describe(int[] model, FeatureModelCnf cnf) {
		TreeSet<String> selected = new TreeSet<String>();
		for (int literal : model) {
			if (literal > 0 && literal <= cnf.getVariableCount()) {
				selected.add(cnf.featureOf(literal));
			}
		}
		return selected.toString();
	}

	/** Evaluates every UniGen sample against the clauses, all features fixed. */
	private static void checkSamples(FeatureModelCnf cnf, List<int[]> clauses, File samples, File mapping)
			throws Exception {
		Map<Integer, String> idToName = new HashMap<Integer, String>();
		try (BufferedReader reader = new BufferedReader(new FileReader(mapping))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(":", 2);
				if (parts.length == 2 && parts[0].trim().matches("\\d+")) {
					idToName.put(Integer.parseInt(parts[0].trim()), parts[1].trim());
				}
			}
		}
		Map<String, Integer> nameToVariable = new HashMap<String, Integer>();
		for (int v = 1; v <= cnf.getVariableCount(); v++) {
			nameToVariable.put(cnf.featureOf(v), v);
		}
		Set<String> missingFromMapping = new TreeSet<String>(nameToVariable.keySet());
		missingFromMapping.removeAll(idToName.values());

		ISolver solver = SolverFactory.newDefault();
		solver.newVar(cnf.getVariableCount());
		for (int[] clause : clauses) {
			solver.addClause(new VecInt(clause));
		}

		int total = 0;
		int accepted = 0;
		List<Integer> rejectedLines = new ArrayList<Integer>();
		try (BufferedReader reader = new BufferedReader(new FileReader(samples))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().isEmpty()) {
					continue;
				}
				total++;
				VecInt assumptions = new VecInt();
				Set<Integer> fixed = new LinkedHashSet<Integer>();
				for (String token : line.trim().split("\\s+")) {
					int literal = Integer.parseInt(token);
					if (literal == 0) {
						continue;
					}
					String name = idToName.get(Math.abs(literal));
					Integer variable = name == null ? null : nameToVariable.get(name);
					if (variable != null) {
						assumptions.push(literal > 0 ? variable : -variable);
						fixed.add(variable);
					}
				}
				if (fixed.size() != cnf.getVariableCount()) {
					System.out.println("  sample " + total + " fixes " + fixed.size() + " of "
							+ cnf.getVariableCount() + " features");
				}
				if (solver.isSatisfiable(assumptions)) {
					accepted++;
				} else {
					rejectedLines.add(total);
				}
			}
		}
		System.out.println("  UniGen samples accepted by Phi_d: " + accepted + " / " + total
				+ (missingFromMapping.isEmpty() ? "" : " (features absent from the DIMACS mapping: "
						+ missingFromMapping + ")")
				+ (rejectedLines.isEmpty() ? "" : " rejected lines: " + rejectedLines));
	}
}
