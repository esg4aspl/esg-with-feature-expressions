package tr.edu.iyte.esgfx.staticanalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.sat4j.core.VecInt;
import org.sat4j.specs.ISolver;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esgfx.api.LoadedSplModel;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EdgeCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EulerCycleGeneratorForEdgeCoverage;
import tr.edu.iyte.esgfx.testgeneration.eventcoverage.EulerCycleGeneratorForEventCoverage;
import tr.edu.iyte.esgfx.testgeneration.eventcoverage.EventCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.eventtriplecoverage.TransformedESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;

/**
 * RQ2, natural part (brief, Step 9), Hockerty Shirts. Every product is
 * projected and checked by {@link ProjectionWellFormedness} (the guard of
 * Section 9a). A product that is not well-formed is recorded with its failing
 * elements and is not passed to the balancing step. A well-formed product runs
 * the pipeline of Chapter 6 at L = 1 (balancing, Euler cycle for event
 * coverage, CES) and L = 2 (transformation, balancing, Euler cycle for edge
 * coverage, CES), with the generation code unchanged; generation time is the
 * median over the repetitions, the other measures are taken from the first.
 *
 * <p>Usage: {@code NaturalRq2Runner affected|unaffected <products file> <out.csv> [--reps N]
 * [--witnesses findings.csv]} where the products file holds samples as DIMACS
 * literals of HS.dimacs (concrete variables at least), or
 * {@code NaturalRq2Runner demo <products file> <line>}: one product through the
 * pipeline at L = 1 without the guard.
 */
public final class NaturalRq2Runner {

	public static void main(String[] args) throws Exception {
		CaseStudy study = CaseStudy.byName("HockertyShirts");
		LoadedSplModel model = study.load();
		PhiD phiD = PhiDSource.DIMACS.load(study, model);
		if (args[0].equals("demo")) {
			List<int[]> products = readProducts(new File(args[1]), phiD);
			demo(model, phiD, products.get(Integer.parseInt(args[2]) - 1));
			return;
		}
		String group = args[0];
		int reps = 11;
		File witnesses = null;
		for (int i = 3; i < args.length; i++) {
			if (args[i].equals("--reps")) {
				reps = Integer.parseInt(args[++i]);
			} else if (args[i].equals("--witnesses")) {
				witnesses = new File(args[++i]);
			}
		}
		List<int[]> products = readProducts(new File(args[1]), phiD);
		List<String> origins = new ArrayList<String>();
		for (int i = 0; i < products.size(); i++) {
			origins.add("sample " + (i + 1));
		}
		if (witnesses != null) {
			Set<List<String>> seen = new HashSet<List<String>>();
			for (int[] product : products) {
				seen.add(phiD.concreteSelection(product));
			}
			for (int[] witness : readWitnesses(witnesses, phiD)) {
				if (seen.add(phiD.concreteSelection(witness))) {
					products.add(witness);
					origins.add("witness");
				}
			}
		}
		run(group, model, phiD, products, origins, reps, new File(args[2]));
	}

	private static void run(String group, LoadedSplModel model, PhiD phiD, List<int[]> products,
			List<String> origins, int reps, File out) throws Exception {
		Map<String, FeatureExpression> features = model.getFeatureExpressionMap();
		int accents = phiD.variableOf("Accents");
		int wellFormedCount = 0;
		try (PrintWriter w = new PrintWriter(out, "UTF-8")) {
			w.println("phid_source,group,product,origin,accents,events,edges,check_us,well_formed,"
					+ "complete_sequence,unreachable_events,events_not_reaching_end,not_useful_events,"
					+ "not_useful_edges,L1_completed,L1_coverage,L1_balancing_edges,L1_test_cases,L1_test_events,"
					+ "L1_generation_ms,L2_completed,L2_coverage,L2_balancing_edges,L2_test_cases,L2_test_events,"
					+ "L2_generation_ms,repetitions,unreachable,not_reaching_end,product_features");
			for (int p = 0; p < products.size(); p++) {
				int[] model0 = products.get(p);
				Set<String> selected = new HashSet<String>(phiD.selection(model0));
				long start = System.nanoTime();
				ESG product = ProductProjection.project(model.getEsgFx(), features, phiD, selected);
				ProjectionWellFormedness check = ProjectionWellFormedness.check(product);
				boolean wellFormed = check.isWellFormed() && check.isEndReachable();
				long checkNanos = System.nanoTime() - start;

				List<String> row = new ArrayList<String>(Arrays.asList(phiD.getSource(), group,
						Integer.toString(p + 1), origins.get(p), Boolean.toString(model0[accents - 1] > 0),
						Integer.toString(product.getVertexList().size() - 2),
						Integer.toString(product.getEdgeList().size()),
						String.format(Locale.ROOT, "%.1f", checkNanos / 1e3), Boolean.toString(wellFormed),
						Boolean.toString(check.isEndReachable())));
				Set<Vertex> forward = check.getForwardSet();
				Set<Vertex> backward = check.getBackwardSet();
				int notUsefulEdges = 0;
				for (Edge edge : product.getEdgeList()) {
					if (!(isOrIn(edge.getSource(), forward) && isOrIn(edge.getTarget(), backward))) {
						notUsefulEdges++;
					}
				}
				Set<Vertex> notUseful = new LinkedHashSet<Vertex>(check.getUnreachable());
				notUseful.addAll(check.getNotCoreachable());
				row.add(Integer.toString(check.getUnreachable().size()));
				row.add(Integer.toString(check.getNotCoreachable().size()));
				row.add(Integer.toString(notUseful.size()));
				row.add(Integer.toString(notUsefulEdges));
				if (wellFormed) {
					wellFormedCount++;
					java.util.function.Supplier<ESG> fresh = () -> ProductProjection.project(model.getEsgFx(), features,
							phiD, selected);
					row.addAll(pipeline(1, fresh, features, reps));
					row.addAll(pipeline(2, fresh, features, reps));
					row.add(Integer.toString(reps));
				} else {
					row.addAll(Arrays.asList("", "", "", "", "", "", "", "", "", "", "", "", ""));
				}
				row.add(StaticAnalysisRunner.csv(names(check.getUnreachable())));
				row.add(StaticAnalysisRunner.csv(names(check.getNotCoreachable())));
				row.add(StaticAnalysisRunner.csv(String.join(";", phiD.concreteSelection(model0))));
				w.println(String.join(",", row));
				w.flush();
			}
		}
		System.out.println(group + ": " + products.size() + " products, " + wellFormedCount + " well-formed; " + out);
	}

	/** A vertex of the projection: the pseudo vertices count as reachable and co-reachable as the sets say. */
	private static boolean isOrIn(Vertex vertex, Set<Vertex> set) {
		return set.contains(vertex) || vertex.isPseudoStartVertex() || vertex.isPseudoEndVertex();
	}

	/**
	 * completed, coverage, balancing edges, test cases, test events, generation ms (median). Every
	 * repetition starts from a freshly projected product: the generators change the state of the
	 * vertices they are given, and Chapter 6 ran each product once.
	 */
	private static List<String> pipeline(int level, java.util.function.Supplier<ESG> fresh,
			Map<String, FeatureExpression> features, int reps) {
		long[] nanos = new long[reps];
		List<String> first = null;
		for (int r = 0; r < reps; r++) {
			try {
				ESG product = fresh.get();
				long start = System.nanoTime();
				ESG input = level == 1 ? product
						: new TransformedESGFxGenerator().generateTransformedESGFx(level, product);
				ESG balanced = StronglyConnectedBalancedESGFxGeneration.getStronglyConnectedBalancedESGFxGeneration(input);
				List<Vertex> cycle;
				if (level == 1) {
					EulerCycleGeneratorForEventCoverage generator = new EulerCycleGeneratorForEventCoverage(features);
					generator.generateEulerCycle(balanced);
					cycle = generator.getEulerCycle();
				} else {
					EulerCycleGeneratorForEdgeCoverage generator = new EulerCycleGeneratorForEdgeCoverage();
					generator.generateEulerCycle(balanced);
					cycle = generator.getEulerCycle();
				}
				Set<EventSequence> sequences = new EulerCycleToTestSequenceGenerator().CESgenerator(cycle);
				nanos[r] = System.nanoTime() - start;
				if (first == null) {
					double coverage = level == 1
							? new EventCoverageAnalyser().analyseEventCoverage(product, sequences, features)
							: new EdgeCoverageAnalyser().analyseEdgeCoverage(product, sequences, features);
					long events = 0;
					for (EventSequence sequence : sequences) {
						events += sequence.length();
					}
					first = Arrays.asList("true", String.format(Locale.ROOT, "%.2f", coverage),
							Integer.toString(balanced.getEdgeList().size() - input.getEdgeList().size()),
							Integer.toString(sequences.size()), Long.toString(events));
				}
			} catch (Throwable t) {
				return Arrays.asList("false: " + t.getClass().getSimpleName(), "", "", "", "", "");
			}
		}
		List<String> result = new ArrayList<String>(first);
		result.add(String.format(Locale.ROOT, "%.3f", StaticAnalysisRunner.median(nanos) / 1e6));
		return result;
	}

	/** One product through the L = 1 pipeline without the guard; prints the stage it reaches. */
	private static void demo(LoadedSplModel model, PhiD phiD, int[] configuration) {
		ESG product = ProductProjection.project(model.getEsgFx(), model.getFeatureExpressionMap(), phiD,
				new HashSet<String>(phiD.selection(configuration)));
		System.out.println("demo: product " + phiD.concreteSelection(configuration) + ", "
				+ (product.getVertexList().size() - 2) + " events, " + product.getEdgeList().size() + " edges");
		System.out.println("demo: balancing started at " + System.currentTimeMillis());
		ESG balanced = StronglyConnectedBalancedESGFxGeneration.getStronglyConnectedBalancedESGFxGeneration(product);
		System.out.println("demo: balancing returned, " + balanced.getEdgeList().size() + " edges");
		EulerCycleGeneratorForEventCoverage generator = new EulerCycleGeneratorForEventCoverage(
				model.getFeatureExpressionMap());
		generator.generateEulerCycle(balanced);
		System.out.println("demo: Euler cycle of " + generator.getEulerCycle().size() + " vertices");
	}

	private static String names(List<Vertex> vertices) {
		List<String> names = new ArrayList<String>();
		for (Vertex vertex : vertices) {
			names.add(vertex.getEvent().getName() + "/" + vertex.getID());
		}
		return String.join(";", names);
	}

	/**
	 * Samples as DIMACS literals of HS.dimacs (same variable ids as the DIMACS
	 * Phi_d), extended to full models of Phi_d by one SAT call each; a sample
	 * that Phi_d rejects fails the run.
	 */
	static List<int[]> readProducts(File file, PhiD phiD) throws Exception {
		List<int[]> products = new ArrayList<int[]>();
		ISolver solver = ProductEnumerator.solverFor(phiD);
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().isEmpty() || line.startsWith("c")) {
					continue;
				}
				VecInt assumptions = new VecInt();
				for (String token : line.trim().split("\\s+")) {
					int literal = Integer.parseInt(token);
					if (literal != 0) {
						assumptions.push(literal);
					}
				}
				products.add(extend(solver, assumptions, phiD, file + " line " + (products.size() + 1)));
			}
		}
		return products;
	}

	/** The witness column (selected concrete features) of a findings CSV of the runner. */
	static List<int[]> readWitnesses(File findings, PhiD phiD) throws Exception {
		List<int[]> witnesses = new ArrayList<int[]>();
		ISolver solver = ProductEnumerator.solverFor(phiD);
		try (BufferedReader reader = new BufferedReader(new FileReader(findings))) {
			reader.readLine();
			String line;
			while ((line = reader.readLine()) != null) {
				List<String> fields = parseCsvLine(line, reader);
				if (fields.size() < 6 || fields.get(5).isEmpty()) {
					continue;
				}
				Set<String> selected = new HashSet<String>(Arrays.asList(fields.get(5).split(";")));
				VecInt assumptions = new VecInt();
				for (int v : phiD.getConcreteVariables()) {
					assumptions.push(selected.contains(phiD.featureOf(v)) ? v : -v);
				}
				witnesses.add(extend(solver, assumptions, phiD, "witness of " + fields.get(2) + " " + fields.get(3)));
			}
		}
		return witnesses;
	}

	private static int[] extend(ISolver solver, VecInt assumptions, PhiD phiD, String what) throws Exception {
		if (solver == null || !solver.isSatisfiable(assumptions)) {
			throw new IllegalStateException(what + " does not satisfy Phi_d");
		}
		int[] full = new int[phiD.getVariableCount()];
		for (int variable = 1; variable <= full.length; variable++) {
			full[variable - 1] = -variable;
		}
		for (int literal : solver.model()) {
			if (Math.abs(literal) <= full.length) {
				full[Math.abs(literal) - 1] = literal;
			}
		}
		return full;
	}

	/** Minimal CSV line reader: quoted fields may contain commas, quotes and line breaks. */
	private static List<String> parseCsvLine(String first, BufferedReader reader) throws Exception {
		List<String> fields = new ArrayList<String>();
		StringBuilder field = new StringBuilder();
		boolean quoted = false;
		String line = first;
		while (true) {
			for (int i = 0; i < line.length(); i++) {
				char c = line.charAt(i);
				if (quoted) {
					if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
						field.append('"');
						i++;
					} else if (c == '"') {
						quoted = false;
					} else {
						field.append(c);
					}
				} else if (c == '"') {
					quoted = true;
				} else if (c == ',') {
					fields.add(field.toString());
					field.setLength(0);
				} else {
					field.append(c);
				}
			}
			if (!quoted) {
				break;
			}
			field.append('\n');
			line = reader.readLine();
		}
		fields.add(field.toString());
		return fields;
	}
}
