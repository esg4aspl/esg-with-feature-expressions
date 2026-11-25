package tr.edu.iyte.esgfx.staticanalysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.EdgeSimple;
import tr.edu.iyte.esg.model.Event;
import tr.edu.iyte.esg.model.EventSimple;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.api.LoadedSplModel;
import tr.edu.iyte.esgfx.api.SingleProductTestGenerationAPI;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.Conjunction;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.Negation;
import tr.edu.iyte.esgfx.model.featuremodel.Feature;

/** Helpers shared by the static analysis tests. */
final class TestModels {

	private TestModels() {
	}

	static LoadedSplModel load(String folder, String prefix) throws Exception {
		return SingleProductTestGenerationAPI.load("files/Cases/" + folder + "/configs/model.xml",
				"files/Cases/" + folder + "/" + prefix + "_ESGFx.mxe");
	}

	static LoadedSplModel svm() throws Exception {
		return load("SodaVendingMachine", "SVM");
	}

	/**
	 * A small model built in memory. Guards are written as in the .mxe files:
	 * "f", "!f", or "f & !g" for a conjunction; vertices without a guard entry
	 * get "true" only if they are the pseudo vertices, so every event needs one.
	 */
	static final class Small {
		final ESG esg;
		final Map<String, FeatureExpression> features = new LinkedHashMap<String, FeatureExpression>();
		final PhiD phiD;

		Small(List<String> featureNames, List<int[]> clauses, Map<String, String> guards, String... edges) {
			for (String name : featureNames) {
				features.put(name, new FeatureExpression(new Feature(name)));
			}
			phiD = new PhiD(featureNames, clauses);

			ESGFx model = new ESGFx(1, "small");
			Map<String, Vertex> byName = new LinkedHashMap<String, Vertex>();
			for (String name : vertexNames(guards, edges)) {
				Event event = new EventSimple(model.getNextEventID(), name);
				model.addEvent(event);
				FeatureExpression guard = name.equals("[") || name.equals("]")
						? new FeatureExpression(new Feature(""), true)
						: parse(guards.get(name));
				Vertex vertex = new VertexRefinedByFeatureExpression(model.getNextVertexID(), event, guard);
				model.addVertex(vertex);
				byName.put(name, vertex);
			}
			for (String edge : edges) {
				String[] ends = edge.split("->");
				model.addEdge(new EdgeSimple(model.getNextEdgeID(), byName.get(ends[0].trim()),
						byName.get(ends[1].trim())));
			}
			esg = model;
		}

		private static List<String> vertexNames(Map<String, String> guards, String[] edges) {
			List<String> names = new ArrayList<String>(Arrays.asList("[", "]"));
			for (String name : guards.keySet()) {
				if (!names.contains(name)) {
					names.add(name);
				}
			}
			for (String edge : edges) {
				for (String end : edge.split("->")) {
					if (!names.contains(end.trim())) {
						names.add(end.trim());
					}
				}
			}
			return names;
		}

		FeatureExpression parse(String guard) {
			if (guard == null) {
				throw new IllegalArgumentException("Every event needs a guard");
			}
			String[] parts = guard.split("&");
			if (parts.length == 1) {
				return literal(parts[0].trim());
			}
			Conjunction conjunction = new Conjunction();
			for (String part : parts) {
				conjunction.addOperand(literal(part.trim()));
			}
			return conjunction;
		}

		private FeatureExpression literal(String text) {
			return text.startsWith("!") ? new Negation(features.get(text.substring(1))) : features.get(text);
		}
	}

	static Map<String, String> guards(String... pairs) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (int i = 0; i < pairs.length; i += 2) {
			map.put(pairs[i], pairs[i + 1]);
		}
		return map;
	}

	static List<int[]> clauses(int[]... clauses) {
		return Arrays.asList(clauses);
	}

	static AmbiguityAnalyzer analyze(ESG esg, PhiD phiD) throws Exception {
		AmbiguityAnalyzer analyzer = new AmbiguityAnalyzer(new StaticAnalysisEncoder(esg, phiD));
		analyzer.analyze();
		return analyzer;
	}

	/** "kind element [cause]" for every finding of the given kinds. */
	static Set<String> describe(List<AmbiguityAnalyzer.Finding> findings, String... kinds) {
		List<String> wanted = Arrays.asList(kinds);
		Set<String> described = new TreeSet<String>();
		for (AmbiguityAnalyzer.Finding finding : findings) {
			if (wanted.contains(finding.kind)) {
				described.add(finding.kind + " " + finding.element + (finding.cause == null ? "" : " " + finding.cause));
			}
		}
		return described;
	}

	static Set<String> set(String... values) {
		return new TreeSet<String>(Arrays.asList(values));
	}
}
