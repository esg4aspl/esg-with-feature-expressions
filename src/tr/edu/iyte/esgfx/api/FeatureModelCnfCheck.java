package tr.edu.iyte.esgfx.api;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;
import org.sat4j.tools.ModelIterator;

/**
 * Checks that {@link FeatureModelCnf} means the same thing as the feature model.
 *
 * <p>Comparing the CNF text against a recorded file would only catch a change in
 * formatting. What matters is the set of configurations it admits, so this
 * enumerates the CNF's own models and requires that set to equal the one
 * {@link AllProductsTestGenerationAPI} generates for — configuration by
 * configuration, not merely by count.
 */
public class FeatureModelCnfCheck {

	private static final SplCase[] SPLS = new SplCase[] {
			new SplCase("SVM", "files/Cases/SodaVendingMachine/", "SVM_ESGFx.mxe"),
			new SplCase("eM", "files/Cases/eMail/", "eM_ESGFx.mxe"),
			new SplCase("El", "files/Cases/Elevator/", "El_ESGFx.mxe")
	};

	public static void main(String[] args) throws Exception {
		int pass = 0;
		int fail = 0;

		for (SplCase spl : SPLS) {
			LoadedSplModel model = SingleProductTestGenerationAPI.load(spl.caseFolder + "configs/model.xml",
					spl.caseFolder + spl.esgFxFileName);

			FeatureModelCnf cnf = FeatureModelCnf.of(model);
			Set<String> fromCnf = enumerateCnf(cnf);

			Set<String> fromEngine = new LinkedHashSet<String>();
			for (SingleProductTestResult result : AllProductsTestGenerationAPI
					.generateForAllProducts(model, 1)) {
				fromEngine.add(describe(result.getSelection()));
			}

			boolean same = fromCnf.equals(fromEngine);
			System.out.println(spl.name + ": variables " + cnf.getVariableCount()
					+ " | CNF configurations " + fromCnf.size()
					+ " | engine configurations " + fromEngine.size()
					+ " | identical sets: " + same);

			if (same) {
				pass++;
			} else {
				fail++;
				Set<String> onlyCnf = new LinkedHashSet<String>(fromCnf);
				onlyCnf.removeAll(fromEngine);
				Set<String> onlyEngine = new LinkedHashSet<String>(fromEngine);
				onlyEngine.removeAll(fromCnf);
				System.out.println("  only in CNF:    " + onlyCnf);
				System.out.println("  only in engine: " + onlyEngine);
			}
		}

		System.out.println("=== Grand total: " + pass + " PASS, " + fail + " FAIL");
	}

	/** Every satisfying assignment of the CNF, as a canonical description. */
	private static Set<String> enumerateCnf(FeatureModelCnf cnf) throws Exception {
		ISolver solver = new ModelIterator(SolverFactory.newDefault());
		solver.newVar(cnf.getVariableCount());

		String dimacs = cnf.toDimacs();
		for (String line : dimacs.split("\n")) {
			String trimmed = line.trim();
			if (trimmed.isEmpty() || trimmed.startsWith("c") || trimmed.startsWith("p")) {
				continue;
			}
			VecInt clause = new VecInt();
			for (String token : trimmed.split("\\s+")) {
				int literal = Integer.parseInt(token);
				if (literal != 0) {
					clause.push(literal);
				}
			}
			solver.addClause(clause);
		}

		Set<String> configurations = new LinkedHashSet<String>();
		while (solver.isSatisfiable()) {
			int[] assignment = solver.model();
			configurations.add(describe(cnf.selectionOf(assignment)));
			if (SingleProductTestGenerationAPI.blockCurrentModel(solver, assignment)) {
				break;
			}
		}
		return configurations;
	}

	/** Selected feature names, sorted, so two selections compare by content alone. */
	private static String describe(Map<String, Boolean> selection) {
		Map<String, Boolean> sorted = new TreeMap<String, Boolean>(selection);
		List<String> enabled = new ArrayList<String>();
		for (Map.Entry<String, Boolean> entry : sorted.entrySet()) {
			if (Boolean.TRUE.equals(entry.getValue())) {
				enabled.add(entry.getKey());
			}
		}
		return enabled.toString();
	}

	private static class SplCase {
		final String name;
		final String caseFolder;
		final String esgFxFileName;

		SplCase(String name, String caseFolder, String esgFxFileName) {
			this.name = name;
			this.caseFolder = caseFolder;
			this.esgFxFileName = esgFxFileName;
		}
	}
}
