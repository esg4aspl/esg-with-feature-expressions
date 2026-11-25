package tr.edu.iyte.esgfx.staticanalysis;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

/**
 * The reference for the agreement tests: enumerate every product of Phi_d
 * (solve, block the concrete literals, repeat), project the family model onto
 * it with the generation code path, traverse forward and backward, and derive
 * every kind of finding from those facts alone. The encoder is used only for
 * vertex labels, guards and the edge list.
 */
final class BruteForce {

	final int products;
	final Set<String> findings = new TreeSet<String>();
	/** The enumerated products, each as its set of selected concrete features. */
	final Set<Set<String>> productSet = new java.util.HashSet<Set<String>>();

	BruteForce(StaticAnalysisEncoder encoder, Map<String, FeatureExpression> featureMap) throws Exception {
		int n = encoder.vertexCount();
		int m = encoder.edgeCount();
		boolean[] onSequence = new boolean[n];
		boolean[] reachedSomewhere = new boolean[n];
		boolean[] presentSomewhere = new boolean[n];
		boolean[] absentSomewhere = new boolean[n];
		boolean[] hiddenUnreachable = new boolean[n];
		boolean[] hiddenDeadEnd = new boolean[n];
		boolean[] edgeLive = new boolean[m];
		boolean[] edgeReachable = new boolean[m];
		// guardFailsWhenReached[u][v]: some product reaches u and does not contain v
		Set<Long> guardFailsAfter = new java.util.HashSet<Long>();
		boolean noCompleteSequence = false;

		PhiD phiD = encoder.getPhiD();
		int count = 0;
		for (int[] model : enumerate(phiD)) {
			count++;
			productSet.add(new java.util.HashSet<String>(phiD.concreteSelection(model)));
			List<String> configuration = phiD.selection(model);
			ProductProjection projection = ProductProjection.of(encoder.getEsg(), featureMap, encoder.getPhiD(),
					configuration);
			ProjectionWellFormedness traversals = ProjectionWellFormedness.check(projection.getProduct());
			boolean[] present = new boolean[n];
			boolean[] reach = new boolean[n];
			boolean[] coreach = new boolean[n];
			for (int v = 0; v < n; v++) {
				Vertex copy = projection.copyOf(v);
				if (copy != null) {
					present[v] = true;
					reach[v] = traversals.isReachable(copy);
					coreach[v] = traversals.isCoreachable(copy);
				}
			}
			if (!reach[encoder.endIndex()]) {
				noCompleteSequence = true;
			}
			for (int v = 0; v < n; v++) {
				onSequence[v] |= reach[v] && coreach[v];
				reachedSomewhere[v] |= reach[v];
				presentSomewhere[v] |= present[v];
				absentSomewhere[v] |= !present[v];
				hiddenUnreachable[v] |= present[v] && !reach[v];
				hiddenDeadEnd[v] |= reach[v] && !coreach[v];
			}
			for (int k = 0; k < m; k++) {
				int u = encoder.edgeSource(k);
				int v = encoder.edgeTarget(k);
				edgeLive[k] |= reach[u] && coreach[v];
				edgeReachable[k] |= reach[u] && present[v];
				if (reach[u] && !present[v]) {
					guardFailsAfter.add((long) u * n + v);
				}
			}
		}
		products = count;

		for (int v = 0; v < n; v++) {
			if (encoder.isPseudo(v)) {
				continue;
			}
			String label = encoder.label(v);
			boolean dead = !onSequence[v];
			if (dead) {
				findings.add("dead-event " + label + " " + (reachedSomewhere[v] ? "no-completion" : "unreachable"));
				if (presentSomewhere[v]) {
					findings.add("contained " + label);
				}
			}
			// Definitions 7.7 and 7.8: the hidden kinds apply to events that are not dead.
			if (hiddenUnreachable[v] && !dead) {
				findings.add("hidden-unreachable " + label);
			}
			if (hiddenDeadEnd[v] && !dead) {
				findings.add("hidden-dead-end " + label);
			}
			if (encoder.negatedGuard(v) != null) {
				boolean core = !absentSomewhere[v];
				if (core) {
					findings.add("core " + label);
				}
				Set<Integer> predecessors = new LinkedHashSet<Integer>();
				for (int k : encoder.incoming(v)) {
					predecessors.add(encoder.edgeSource(k));
				}
				boolean someReached = false;
				boolean alwaysImplied = true;
				for (int u : predecessors) {
					someReached |= reachedSomewhere[u];
					alwaysImplied &= !guardFailsAfter.contains((long) u * n + v);
				}
				if (someReached && alwaysImplied) {
					findings.add("false-optional " + label + " " + (core ? "core" : "predecessors"));
				}
			}
		}
		for (int k = 0; k < m; k++) {
			if (!edgeLive[k]) {
				findings.add("dead-edge " + encoder.edgeLabel(k) + " "
						+ (edgeReachable[k] ? "no-completion" : "unreachable"));
			}
		}
		if (noCompleteSequence) {
			findings.add("no-complete-sequence " + encoder.label(encoder.startIndex()));
		}
	}

	/** The full solver model of every product, by the blocking loop over the concrete variables. */
	static List<int[]> enumerate(PhiD phiD) throws Exception {
		final List<int[]> models = new ArrayList<int[]>();
		ProductEnumerator.products(phiD, models::add, ProductEnumerator.NO_LIMIT);
		return models;
	}
}
