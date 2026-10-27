package tr.edu.iyte.esgfx.staticanalysis;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import tr.edu.iyte.esgfx.api.LoadedSplModel;

/**
 * Runs the static analysis over the product lines of the study (brief,
 * Section 7) for one or both sources of Phi_d, and writes, per source, into
 * {@code <out>/phid-<source>/}:
 * <ul>
 * <li>{@code <model>_findings.csv}: one row per finding, with the witness (the
 * selected concrete features) and, for the enumerable product lines, the
 * number of products in which the finding appears;</li>
 * <li>{@code <model>_questions.csv}: one row per solver call of the last
 * repetition;</li>
 * <li>{@code summary.csv}: counts per kind and times (medians over the
 * repetitions).</li>
 * </ul>
 * Every row records the source of Phi_d.
 *
 * <p>Usage: {@code StaticAnalysisRunner [--phid engine|dimacs|both] [--reps N]
 * [--out DIR] [--no-product-counts] [--unit FEATURE] [model ...]}; all eight
 * models when none is named. {@code --unit} adds the unit clause FEATURE to
 * Phi_d in memory (a candidate repair; no model file is changed) and writes to
 * {@code phid-<source>-<feature>/}.
 */
public final class StaticAnalysisRunner {

	private static final String[] REPORTED_KINDS = { AmbiguityAnalyzer.DEAD_EVENT, AmbiguityAnalyzer.DEAD_EDGE,
			AmbiguityAnalyzer.FALSE_OPTIONAL, AmbiguityAnalyzer.HIDDEN_UNREACHABLE, AmbiguityAnalyzer.HIDDEN_DEAD_END,
			AmbiguityAnalyzer.NO_COMPLETE_SEQUENCE, AmbiguityAnalyzer.CONTAINED, AmbiguityAnalyzer.CORE };

	public static void main(String[] args) throws Exception {
		List<PhiDSource> sources = Arrays.asList(PhiDSource.values());
		int repetitions = 11;
		String out = "results";
		boolean productCounts = true;
		String unit = null;
		List<String> models = new ArrayList<String>();
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "--phid":
				sources = args[++i].equals("both") ? Arrays.asList(PhiDSource.values())
						: Collections.singletonList(PhiDSource.parse(args[i]));
				break;
			case "--reps":
				repetitions = Integer.parseInt(args[++i]);
				break;
			case "--out":
				out = args[++i];
				break;
			case "--no-product-counts":
				productCounts = false;
				break;
			case "--unit":
				unit = args[++i];
				break;
			default:
				models.add(args[i]);
			}
		}
		for (PhiDSource source : sources) {
			File folder = new File(out, "phid-" + source.label() + (unit == null ? "" : "-" + unit.toLowerCase()));
			folder.mkdirs();
			File summary = new File(folder, "summary.csv");
			try (PrintWriter writer = new PrintWriter(summary, "UTF-8")) {
				writer.println("phid_source,model,vertices,edges,phid_variables,concrete_variables,phid_clauses,"
						+ "solver_variables,solver_clauses,cardinality_constraints,configurations,products,"
						+ "dead_events,dead_edges,false_optional,false_optional_core,hidden_unreachable,"
						+ "hidden_dead_end,no_complete_sequence,contained,core,witnesses_verified,questions,"
						+ "load_ms,build_ms,analysis_ms,repetitions");
				for (CaseStudy study : CaseStudy.select(models)) {
					writer.println(run(study, source, unit, repetitions, productCounts, folder));
					writer.flush();
				}
			}
			System.out.println("Wrote " + summary);
		}
	}

	private static String run(CaseStudy study, PhiDSource source, String unit, int repetitions,
			boolean productCounts, File folder) throws Exception {
		long started = System.nanoTime();
		LoadedSplModel model = study.load();
		PhiD phiD = source.load(study, model);
		if (unit != null) {
			phiD = CrosscheckExport.withUnitClause(phiD, unit);
			// The counts of Step 0 hold for the unchanged Phi_d only.
			productCounts = false;
		}
		long loadNanos = System.nanoTime() - started;
		GuardFeatureCheck.check(study.name, model.getEsgFx(), phiD);

		long[] build = new long[repetitions];
		long[] analysis = new long[repetitions];
		AmbiguityAnalyzer analyzer = null;
		for (int r = 0; r < repetitions; r++) {
			StaticAnalysisEncoder encoder = new StaticAnalysisEncoder(model.getEsgFx(), phiD);
			analyzer = new AmbiguityAnalyzer(encoder);
			analyzer.analyze();
			build[r] = encoder.getBuildNanos();
			analysis[r] = analyzer.getAnalysisNanos();
		}
		StaticAnalysisEncoder encoder = analyzer.getEncoder();
		int verified = new WitnessVerifier(study.name, model.getFeatureExpressionMap(), null).verify(analyzer);

		long configurations = -1;
		long products = -1;
		if (study.isEnumerable() && productCounts) {
			if (source == PhiDSource.DIMACS) {
				// Established by the Step 0 check (PhiDSourceCheck); not recounted here.
				configurations = study.configurations;
				products = study.products;
			} else {
				configurations = study.chapter6;
				products = ProductEnumerator.products(phiD, null, ProductEnumerator.NO_LIMIT);
			}
			for (AmbiguityAnalyzer.Finding finding : analyzer.getFindings()) {
				if (finding.witness != null) {
					StaticAnalysisEncoder counter = new StaticAnalysisEncoder(model.getEsgFx(), phiD);
					finding.setProducts(counter.countProductsAndConsume(
							AmbiguityAnalyzer.assumptionsOf(counter, finding), ProductEnumerator.NO_LIMIT));
				}
			}
		}

		writeFindings(new File(folder, study.name + "_findings.csv"), study, phiD, analyzer);
		writeQuestions(new File(folder, study.name + "_questions.csv"), study, phiD, analyzer);

		int[] counts = new int[REPORTED_KINDS.length];
		int falseOptionalCore = 0;
		for (AmbiguityAnalyzer.Finding finding : analyzer.getFindings()) {
			counts[Arrays.asList(REPORTED_KINDS).indexOf(finding.kind)]++;
			if (finding.kind.equals(AmbiguityAnalyzer.FALSE_OPTIONAL)
					&& AmbiguityAnalyzer.CAUSE_CORE.equals(finding.cause)) {
				falseOptionalCore++;
			}
		}
		double buildMs = median(build) / 1e6;
		double analysisMs = median(analysis) / 1e6;
		System.out.println(String.format(Locale.ROOT,
				"[%s] %-18s |V|=%d |E|=%d products=%s dead events %d, dead edges %d, false optional %d (core %d), "
						+ "hidden unreachable %d, hidden dead end %d, no complete sequence %d; contained %d, core %d; "
						+ "%d witnesses verified; %d questions; build %.1f ms, analysis %.1f ms (median of %d)",
				phiD.getSource(), study.name, encoder.vertexCount(), encoder.edgeCount(),
				products < 0 ? "n/a" : Long.toString(products), counts[0], counts[1], counts[2], falseOptionalCore,
				counts[3], counts[4], counts[5], counts[6], counts[7], verified, analyzer.getQuestions().size(),
				buildMs, analysisMs, repetitions));
		return String.format(Locale.ROOT, "%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.1f,%.3f,%.3f,%d",
				phiD.getSource(), study.name, encoder.vertexCount(), encoder.edgeCount(), phiD.getVariableCount(),
				phiD.getConcreteVariables().length, phiD.getClauses().size(), encoder.getVariableCount(),
				encoder.getClauseCount(), encoder.getCardinalityConstraintCount(),
				configurations < 0 ? "" : Long.toString(configurations), products < 0 ? "" : Long.toString(products),
				counts[0], counts[1], counts[2], falseOptionalCore, counts[3], counts[4], counts[5], counts[6],
				counts[7], verified, analyzer.getQuestions().size(), loadNanos / 1e6, buildMs, analysisMs,
				repetitions);
	}

	private static void writeFindings(File file, CaseStudy study, PhiD phiD, AmbiguityAnalyzer analyzer)
			throws IOException {
		try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
			writer.println("phid_source,model,kind,element,cause,witness,products,verified");
			for (AmbiguityAnalyzer.Finding finding : analyzer.getFindings()) {
				writer.println(String.join(",", phiD.getSource(), study.name, finding.kind, csv(finding.element),
						finding.cause == null ? "" : finding.cause,
						finding.witness == null ? "" : csv(String.join(";", finding.witness)),
						finding.getProducts() < 0 ? "" : Long.toString(finding.getProducts()),
						finding.getVerified() == null ? "" : finding.getVerified().toString()));
			}
		}
	}

	private static void writeQuestions(File file, CaseStudy study, PhiD phiD, AmbiguityAnalyzer analyzer)
			throws IOException {
		try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
			writer.println("phid_source,model,kind,element,answer,nanos");
			for (AmbiguityAnalyzer.Question question : analyzer.getQuestions()) {
				writer.println(String.join(",", phiD.getSource(), study.name, question.kind, csv(question.element),
						question.satisfiable ? "SAT" : "UNSAT", Long.toString(question.nanos)));
			}
		}
	}

	static String csv(String value) {
		if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}

	static double median(long[] values) {
		long[] sorted = values.clone();
		Arrays.sort(sorted);
		int n = sorted.length;
		return n % 2 == 1 ? sorted[n / 2] : (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;
	}
}
