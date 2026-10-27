package tr.edu.iyte.esgfx.staticanalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sat4j.core.VecInt;

import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

/**
 * Checks every witness of an analysis independently of the encoding: the
 * configuration satisfies Phi_d, its product (the selected concrete features)
 * is one of the enumerated products when the enumeration is given, and the
 * projection, traversed forward from the pseudo-start and backward from the
 * pseudo-end, has the claimed property.
 * A mismatch is a bug in the encoding and fails the run.
 */
public final class WitnessVerifier {

	private final String modelName;
	private final Map<String, FeatureExpression> featureMap;
	private final Set<Set<String>> enumerated;

	/**
	 * @param enumerated the enumerated products (sets of selected concrete
	 *                   features), or null when no enumeration is available
	 */
	public WitnessVerifier(String modelName, Map<String, FeatureExpression> featureMap,
			Set<Set<String>> enumerated) {
		this.modelName = modelName;
		this.featureMap = featureMap;
		this.enumerated = enumerated;
	}

	/** @return the number of witnesses verified */
	public int verify(AmbiguityAnalyzer analyzer) {
		StaticAnalysisEncoder encoder = analyzer.getEncoder();
		int verified = 0;
		for (AmbiguityAnalyzer.Finding finding : analyzer.getFindings()) {
			if (finding.witness == null) {
				continue;
			}
			checkConfiguration(encoder, finding);
			ProductProjection projection = ProductProjection.of(encoder.getEsg(), featureMap, encoder.getPhiD(),
					finding.configuration);
			ProjectionWellFormedness traversals = ProjectionWellFormedness.check(projection.getProduct());
			if (!holds(finding, projection, traversals)) {
				fail(finding, "the projection does not have the claimed property");
			}
			finding.setVerified(true);
			verified++;
		}
		return verified;
	}

	private void checkConfiguration(StaticAnalysisEncoder encoder, AmbiguityAnalyzer.Finding finding) {
		PhiD phiD = encoder.getPhiD();
		VecInt configuration = new VecInt();
		for (int variable = 1; variable <= phiD.getVariableCount(); variable++) {
			configuration.push(finding.configuration.contains(phiD.featureOf(variable)) ? variable : -variable);
		}
		if (!encoder.satisfiesPhiD(configuration)) {
			fail(finding, "the witness does not satisfy Phi_d");
		}
		if (!phiD.concreteSelection(toModel(phiD, finding.configuration)).equals(finding.witness)) {
			fail(finding, "the witness is not the concrete restriction of its configuration");
		}
		if (enumerated != null && !enumerated.contains(new HashSet<String>(finding.witness))) {
			fail(finding, "the witness is not an enumerated product");
		}
	}

	private static int[] toModel(PhiD phiD, java.util.List<String> trueFeatures) {
		int[] model = new int[phiD.getVariableCount()];
		for (int variable = 1; variable <= model.length; variable++) {
			model[variable - 1] = trueFeatures.contains(phiD.featureOf(variable)) ? variable : -variable;
		}
		return model;
	}

	private static boolean holds(AmbiguityAnalyzer.Finding finding, ProductProjection projection,
			ProjectionWellFormedness traversals) {
		if (finding.kind.equals(AmbiguityAnalyzer.NO_COMPLETE_SEQUENCE)) {
			return !traversals.isEndReachable();
		}
		Vertex copy = projection.copyOf(finding.index);
		if (copy == null) {
			return false;
		}
		boolean reachable = traversals.isReachable(copy);
		boolean coreachable = traversals.isCoreachable(copy);
		if (finding.kind.equals(AmbiguityAnalyzer.HIDDEN_UNREACHABLE)) {
			return !reachable;
		}
		if (finding.kind.equals(AmbiguityAnalyzer.HIDDEN_DEAD_END)) {
			return reachable && !coreachable;
		}
		if (finding.kind.equals(AmbiguityAnalyzer.CONTAINED)) {
			// A dead event in the product: never on a complete event sequence.
			return !(reachable && coreachable);
		}
		throw new IllegalArgumentException("No witness check for kind " + finding.kind);
	}

	private void fail(AmbiguityAnalyzer.Finding finding, String reason) {
		throw new IllegalStateException("Witness verification failed: model " + modelName + ", question "
				+ finding.kind + " " + finding.element + ", configuration " + finding.witness + ": " + reason);
	}

	/**
	 * Reads a product configuration file of the Chapter 6 enumeration ("P0001:
	 * <a, b>:2 features" per line) and restricts every line to the concrete
	 * features of Phi_d. Returns null when the file is missing or lists fewer
	 * configurations than expected, that is, when it holds a sample and not the
	 * enumeration.
	 */
	public static Set<Set<String>> readEnumeration(File file, long expectedConfigurations, PhiD phiD)
			throws IOException {
		if (!file.exists()) {
			return null;
		}
		Set<Set<String>> products = new HashSet<Set<String>>();
		long lines = 0;
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = reader.readLine()) != null) {
				int open = line.indexOf('<');
				int close = line.indexOf('>');
				if (!line.startsWith("P") || open < 0 || close < open) {
					continue;
				}
				Set<String> features = new HashSet<String>();
				for (String name : line.substring(open + 1, close).split(",")) {
					Integer variable = phiD.variableOf(name.trim());
					if (variable != null && phiD.isConcrete(variable)) {
						features.add(name.trim());
					}
				}
				products.add(features);
				lines++;
			}
		}
		return lines == expectedConfigurations ? products : null;
	}
}
