package tr.edu.iyte.esgfx.staticanalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esgfx.api.LoadedSplModel;

/**
 * The RQ3 baseline (brief, Section 9): every product is projected with the
 * generation code path and validated by the two traversals, forward from the
 * pseudo-start and backward from the pseudo-end. For the seven enumerable
 * product lines the products are enumerated with the blocking loop over the
 * concrete variables, and the enumeration time is recorded separately; for
 * Hockerty Shirts the products are the UniGen samples of Chapter 6.
 *
 * <p>Writes {@code <out>/phid-<source>/baseline_summary.csv} and, for Hockerty
 * Shirts, {@code baseline_HockertyShirts_products.csv}.
 *
 * <p>Usage: {@code ReachabilityBaseline [--phid engine|dimacs] [--reps N]
 * [--out DIR] [model ...]}; by default the DIMACS source, 3 repetitions for
 * Tesla and syngo.via and 11 for the others.
 */
public final class ReachabilityBaseline {

	public static void main(String[] args) throws Exception {
		PhiDSource source = PhiDSource.DIMACS;
		Integer repetitions = null;
		String out = "results";
		List<String> models = new ArrayList<String>();
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "--phid":
				source = PhiDSource.parse(args[++i]);
				break;
			case "--reps":
				repetitions = Integer.parseInt(args[++i]);
				break;
			case "--out":
				out = args[++i];
				break;
			default:
				models.add(args[i]);
			}
		}
		File folder = new File(out, "phid-" + source.label());
		folder.mkdirs();
		File summary = new File(folder, "baseline_summary.csv");
		try (PrintWriter writer = new PrintWriter(summary, "UTF-8")) {
			writer.println("phid_source,model,products,product_source,load_ms,enumeration_ms,check_ms,"
					+ "per_product_median_us,per_product_mean_us,not_well_formed,repetitions");
			for (CaseStudy study : CaseStudy.select(models)) {
				int reps = repetitions != null ? repetitions
						: study.name.equals("Tesla") || study.name.equals("syngovia") ? 3 : 11;
				writer.println(run(study, source, reps, folder));
				writer.flush();
			}
		}
		System.out.println("Wrote " + summary);
	}

	private static String run(CaseStudy study, PhiDSource source, int repetitions, File folder) throws Exception {
		long started = System.nanoTime();
		LoadedSplModel model = study.load();
		PhiD phiD = source.load(study, model);
		long loadNanos = System.nanoTime() - started;
		GuardFeatureCheck.check(study.name, model.getEsgFx(), phiD);

		long[] enumeration = new long[repetitions];
		long[] check = new long[repetitions];
		long[] perProductMedian = new long[repetitions];
		List<int[]> products = null;
		long[] lastPerProduct = null;
		boolean[] wellFormed = null;
		int[][] failing = null;
		for (int r = 0; r < repetitions; r++) {
			if (study.isEnumerable()) {
				long t = System.nanoTime();
				final List<int[]> enumerated = new ArrayList<int[]>();
				ProductEnumerator.products(phiD, enumerated::add, ProductEnumerator.NO_LIMIT);
				enumeration[r] = System.nanoTime() - t;
				products = enumerated;
			} else if (products == null) {
				products = readSamples(study, phiD);
			}
			long[] perProduct = new long[products.size()];
			wellFormed = new boolean[products.size()];
			failing = new int[products.size()][];
			long total = 0;
			for (int p = 0; p < products.size(); p++) {
				long s = System.nanoTime();
				Set<String> selected = new HashSet<String>(phiD.selection(products.get(p)));
				ESG product = ProductProjection.project(model.getEsgFx(), model.getFeatureExpressionMap(), phiD,
						selected);
				ProjectionWellFormedness traversals = ProjectionWellFormedness.check(product);
				wellFormed[p] = traversals.isWellFormed() && traversals.isEndReachable();
				perProduct[p] = System.nanoTime() - s;
				total += perProduct[p];
				failing[p] = new int[] { traversals.getUnreachable().size(), traversals.getNotCoreachable().size(),
						traversals.isEndReachable() ? 1 : 0, product.getVertexList().size() };
			}
			// The accumulated time of projecting and validating, without the diagnostics above.
			check[r] = total;
			perProductMedian[r] = (long) StaticAnalysisRunner.median(perProduct);
			lastPerProduct = perProduct;
		}
		int notWellFormed = 0;
		for (boolean ok : wellFormed) {
			notWellFormed += ok ? 0 : 1;
		}
		if (!study.isEnumerable()) {
			writePerProduct(new File(folder, "baseline_" + study.name + "_products.csv"), study, phiD, products,
					lastPerProduct, wellFormed, failing);
		}
		double checkMs = StaticAnalysisRunner.median(check) / 1e6;
		double enumerationMs = study.isEnumerable() ? StaticAnalysisRunner.median(enumeration) / 1e6 : 0;
		double medianUs = StaticAnalysisRunner.median(perProductMedian) / 1e3;
		double meanUs = checkMs * 1e3 / products.size();
		String productSource = study.isEnumerable() ? "enumeration" : "unigen-samples";
		System.out.println(String.format(Locale.ROOT,
				"[%s] %-18s products %d (%s), not well-formed %d; enumeration %.1f ms, check %.1f ms, "
						+ "per product median %.1f us, mean %.1f us (median of %d)",
				source.label(), study.name, products.size(), productSource, notWellFormed, enumerationMs, checkMs,
				medianUs, meanUs, repetitions));
		return String.format(Locale.ROOT, "%s,%s,%d,%s,%.1f,%s,%.3f,%.2f,%.2f,%d,%d", source.label(), study.name,
				products.size(), productSource, loadNanos / 1e6,
				study.isEnumerable() ? String.format(Locale.ROOT, "%.3f", enumerationMs) : "", checkMs, medianUs,
				meanUs, notWellFormed, repetitions);
	}

	/** The UniGen samples as full models over the variables of Phi_d (DIMACS ids mapped by name). */
	static List<int[]> readSamples(CaseStudy study, PhiD phiD) throws Exception {
		TreeMap<Integer, String> mapping = DimacsPhiD.readMapping(study.dimacsMapping());
		List<int[]> samples = new ArrayList<int[]>();
		try (BufferedReader reader = new BufferedReader(new FileReader(study.samples()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().isEmpty()) {
					continue;
				}
				int[] model = new int[phiD.getVariableCount()];
				for (String token : line.trim().split("\\s+")) {
					int literal = Integer.parseInt(token);
					if (literal == 0) {
						continue;
					}
					Integer variable = phiD.variableOf(mapping.get(Math.abs(literal)));
					if (variable != null) {
						model[variable - 1] = literal > 0 ? variable : -variable;
					}
				}
				for (int i = 0; i < model.length; i++) {
					if (model[i] == 0) {
						throw new IllegalStateException(study.name + ": sample " + (samples.size() + 1)
								+ " does not fix " + phiD.featureOf(i + 1));
					}
				}
				samples.add(model);
			}
		}
		return samples;
	}

	private static void writePerProduct(File file, CaseStudy study, PhiD phiD, List<int[]> products, long[] nanos,
			boolean[] wellFormed, int[][] failing) throws Exception {
		Set<List<String>> distinct = new LinkedHashSet<List<String>>();
		try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
			writer.println("phid_source,model,sample,product_vertices,nanos,well_formed,unreachable_events,"
					+ "events_not_reaching_end,end_reachable,product");
			for (int p = 0; p < products.size(); p++) {
				List<String> product = phiD.concreteSelection(products.get(p));
				distinct.add(product);
				writer.println(String.join(",", phiD.getSource(), study.name, Integer.toString(p + 1),
						Integer.toString(failing[p][3]), Long.toString(nanos[p]), Boolean.toString(wellFormed[p]),
						Integer.toString(failing[p][0]), Integer.toString(failing[p][1]),
						Boolean.toString(failing[p][2] == 1),
						StaticAnalysisRunner.csv(String.join(";", product))));
			}
		}
		System.out.println("  " + study.name + ": " + products.size() + " samples, " + distinct.size()
				+ " distinct products; per-sample rows in " + file);
	}
}
