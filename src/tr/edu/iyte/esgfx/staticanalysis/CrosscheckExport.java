package tr.edu.iyte.esgfx.staticanalysis;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.api.LoadedSplModel;

/**
 * Writes the static analysis verdicts per element of the ESG-Fx model, for the
 * comparison with FTS4VMC (brief, Step 8). Vertices and edges are listed in the
 * order of the engine's model, which is the document order of the vertex and
 * edge cells of the .mxe file (the order {@code MXEFileToESGFxConverter} reads
 * them), so they can be joined with the FTS converter's state map by index.
 *
 * <p>Per model, into the output directory:
 * <ul>
 * <li>{@code <abbr>_esg_vertices.csv}: index, label, event, guard, core,
 * dead (cause), false optional (cause), hidden unreachable, hidden dead end,
 * the witness product and configuration of each hidden finding, and in the
 * witness product of a hidden dead end the number of successors of the event
 * and whether the pseudo-end is reachable;</li>
 * <li>{@code <abbr>_esg_edges.csv}: index, source and target index, label, dead
 * edge (cause).</li>
 * </ul>
 *
 * <p>Usage: {@code CrosscheckExport [--phid engine|dimacs] [--accents] outputDir [model ...]}.
 * {@code --accents} adds the unit clause {@code Accents} to Phi_d (Step 10).
 */
public final class CrosscheckExport {

	public static void main(String[] args) throws Exception {
		PhiDSource source = PhiDSource.DIMACS;
		boolean accents = false;
		List<String> rest = new ArrayList<String>();
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--phid")) {
				source = PhiDSource.parse(args[++i]);
			} else if (args[i].equals("--accents")) {
				accents = true;
			} else {
				rest.add(args[i]);
			}
		}
		File out = new File(rest.get(0));
		out.mkdirs();
		for (CaseStudy study : CaseStudy.select(rest.subList(1, rest.size()))) {
			LoadedSplModel model = study.load();
			PhiD phiD = source.load(study, model);
			if (accents) {
				phiD = withUnitClause(phiD, "Accents");
			}
			export(study, model, phiD, out);
		}
	}

	/** Phi_d with one feature forced true; the source label records the change. */
	public static PhiD withUnitClause(PhiD phiD, String feature) {
		Integer variable = phiD.variableOf(feature);
		if (variable == null) {
			throw new IllegalArgumentException(feature + " is not a variable of Phi_d");
		}
		List<int[]> clauses = new ArrayList<int[]>(phiD.getClauses());
		clauses.add(new int[] { variable });
		List<String> abstractFeatures = new ArrayList<String>();
		for (int v = 1; v <= phiD.getVariableCount(); v++) {
			if (!phiD.isConcrete(v)) {
				abstractFeatures.add(phiD.featureOf(v));
			}
		}
		return new PhiD(phiD.getSource() + "+" + feature, phiD.getFeatureNames(), clauses, abstractFeatures);
	}

	static void export(CaseStudy study, LoadedSplModel model, PhiD phiD, File out) throws Exception {
		StaticAnalysisEncoder encoder = new StaticAnalysisEncoder(model.getEsgFx(), phiD);
		AmbiguityAnalyzer analyzer = new AmbiguityAnalyzer(encoder);
		analyzer.analyze();
		new WitnessVerifier(study.name, model.getFeatureExpressionMap(), null).verify(analyzer);

		int n = encoder.vertexCount();
		String[][] vertexColumns = new String[n][];
		for (int v = 0; v < n; v++) {
			vertexColumns[v] = new String[] { "", "", "", "", "", "", "", "", "", "" };
		}
		Map<Integer, String> deadEdges = new HashMap<Integer, String>();
		for (AmbiguityAnalyzer.Finding finding : analyzer.getFindings()) {
			String[] c = finding.kind.equals(AmbiguityAnalyzer.DEAD_EDGE) ? null : vertexColumns[finding.index];
			switch (finding.kind) {
			case AmbiguityAnalyzer.CORE:
				c[0] = "true";
				break;
			case AmbiguityAnalyzer.DEAD_EVENT:
				c[1] = finding.cause;
				break;
			case AmbiguityAnalyzer.FALSE_OPTIONAL:
				c[2] = finding.cause;
				break;
			case AmbiguityAnalyzer.HIDDEN_UNREACHABLE:
				c[3] = "true";
				c[5] = String.join(";", finding.witness);
				c[6] = String.join(";", finding.configuration);
				break;
			case AmbiguityAnalyzer.HIDDEN_DEAD_END:
				c[4] = "true";
				c[7] = String.join(";", finding.witness);
				c[8] = String.join(";", finding.configuration);
				ProductProjection projection = ProductProjection.of(encoder.getEsg(), model.getFeatureExpressionMap(),
						phiD, finding.configuration);
				Vertex copy = projection.copyOf(finding.index);
				int successors = 0;
				for (Edge edge : projection.getProduct().getEdgeList()) {
					if (edge.getSource() == copy) {
						successors++;
					}
				}
				c[9] = Integer.toString(successors);
				break;
			case AmbiguityAnalyzer.DEAD_EDGE:
				deadEdges.put(finding.index, finding.cause);
				break;
			case AmbiguityAnalyzer.NO_COMPLETE_SEQUENCE:
				try (PrintWriter w = new PrintWriter(new File(out, study.prefix + "_no_complete_sequence.csv"),
						"UTF-8")) {
					w.println("phid_source,witness,configuration");
					w.println(String.join(",", phiD.getSource(), String.join(";", finding.witness),
							String.join(";", finding.configuration)));
				}
				break;
			default:
				break;
			}
		}

		try (PrintWriter w = new PrintWriter(new File(out, study.prefix + "_esg_vertices.csv"), "UTF-8")) {
			w.println("phid_source,index,label,event,guard,core,dead,false_optional,hidden_unreachable,"
					+ "hidden_dead_end,hu_witness,hu_configuration,hde_witness,hde_configuration,"
					+ "hde_successors_in_witness");
			for (int v = 0; v < n; v++) {
				Vertex vertex = encoder.vertex(v);
				List<String> row = new ArrayList<String>(Arrays.asList(phiD.getSource(), Integer.toString(v),
						StaticAnalysisRunner.csv(encoder.label(v)),
						StaticAnalysisRunner.csv(vertex.getEvent().getName()), guardText(encoder, v)));
				for (String value : vertexColumns[v]) {
					row.add(StaticAnalysisRunner.csv(value));
				}
				w.println(String.join(",", row));
			}
		}
		try (PrintWriter w = new PrintWriter(new File(out, study.prefix + "_esg_edges.csv"), "UTF-8")) {
			w.println("phid_source,index,source,target,label,dead_edge");
			for (int k = 0; k < encoder.edgeCount(); k++) {
				w.println(String.join(",", phiD.getSource(), Integer.toString(k),
						Integer.toString(encoder.edgeSource(k)), Integer.toString(encoder.edgeTarget(k)),
						StaticAnalysisRunner.csv(encoder.edgeLabel(k)),
						deadEdges.containsKey(k) ? deadEdges.get(k) : ""));
			}
		}
		System.out.println(study.prefix + " [" + phiD.getSource() + "]: " + n + " vertices, " + encoder.edgeCount()
				+ " edges, " + analyzer.getFindings().size() + " findings");
	}

	/** The guard as literals joined by '&', '!' for negation, empty for the pseudo vertices. */
	private static String guardText(StaticAnalysisEncoder encoder, int v) {
		List<String> literals = new ArrayList<String>();
		for (int literal : encoder.guard(v)) {
			literals.add((literal < 0 ? "!" : "") + encoder.getPhiD().featureOf(Math.abs(literal)));
		}
		return StaticAnalysisRunner.csv(String.join("&", literals));
	}
}
