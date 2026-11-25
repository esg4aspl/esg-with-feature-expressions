package tr.edu.iyte.esgfx.testgeneration.eventcoverage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

/**
 * Test generation for event coverage.
 *
 * The generator walks the product-level model from the pseudo-start vertex.
 * At every step it moves to the nearest event that is not covered yet: an
 * uncovered successor if there is one, otherwise along a shortest path to
 * the nearest uncovered event. The pseudo-end vertex is joined to the
 * pseudo-start vertex for this search, so a path that runs through the
 * pseudo-end vertex closes the current sequence and opens the next one.
 * When every event is covered, the walk is closed with a shortest path to
 * the pseudo-end vertex.
 *
 * Every step of every sequence is therefore an edge of the product-level
 * model, every sequence starts at a successor of the pseudo-start vertex
 * and ends at a predecessor of the pseudo-end vertex.
 *
 * The model is used as it is; it does not have to be strongly connected or
 * balanced. If a strongly connected, balanced copy is passed, its
 * duplicated edges and the edges that enter the pseudo-start vertex are
 * ignored, and the result is the same.
 *
 * The result is kept in the order expected by
 * EulerCycleToTestSequenceGenerator, which reads the vertex list from its
 * last element to its first.
 */
public class EulerCycleGeneratorForEventCoverage {

	private Set<Vertex> toCover;
	private List<Vertex> eulerCycle;
	private Map<Vertex, List<Vertex>> adjacencyMap;
	private Set<Vertex> unreachableEvents;

	public EulerCycleGeneratorForEventCoverage() {
		this.toCover = new LinkedHashSet<>();
		this.eulerCycle = new LinkedList<>();
		this.adjacencyMap = new HashMap<>();
		this.unreachableEvents = new LinkedHashSet<>();
	}

	public EulerCycleGeneratorForEventCoverage(Map<String, FeatureExpression> featureExpressionMap) {
		this();
	}

	public void reset() {
		this.toCover.clear();
		this.eulerCycle.clear();
		this.adjacencyMap.clear();
		this.unreachableEvents.clear();
	}

	private static boolean isEnabledEvent(Vertex vertex) {
		if (vertex.isPseudoStartVertex() || vertex.isPseudoEndVertex()) {
			return false;
		}
		if (vertex instanceof VertexRefinedByFeatureExpression) {
			FeatureExpression featureExpression = ((VertexRefinedByFeatureExpression) vertex).getFeatureExpression();
			return featureExpression == null || featureExpression.evaluate();
		}
		return true;
	}

	/**
	 * Successors of every vertex, restricted to the events whose feature
	 * expression is true and to the pseudo-end vertex, without duplicates.
	 */
	private void buildAdjacency(ESGFx graph, Vertex pseudoStart, Vertex pseudoEnd) {
		List<Vertex> sources = new ArrayList<>();
		sources.add(pseudoStart);
		for (Vertex vertex : graph.getVertexList()) {
			if (isEnabledEvent(vertex)) {
				sources.add(vertex);
				toCover.add(vertex);
			}
		}
		for (Vertex source : sources) {
			Set<Vertex> successors = new LinkedHashSet<>();
			for (Vertex target : graph.getAdjacencyList(source)) {
				if (target.isPseudoEndVertex() || isEnabledEvent(target)) {
					successors.add(target);
				}
			}
			adjacencyMap.put(source, new ArrayList<>(successors));
		}
		// Only for the search: leaving the pseudo-end vertex means starting a new sequence.
		adjacencyMap.put(pseudoEnd, Collections.singletonList(pseudoStart));
	}

	/**
	 * Breadth-first search from 'from'. Returns the vertices after 'from' on a
	 * shortest path to the nearest vertex accepted by the target test, or null
	 * if there is none. Successors are visited in the order of the model, so
	 * the result is deterministic.
	 */
	private List<Vertex> shortestPath(Vertex from, boolean toPseudoEnd) {
		Map<Vertex, Vertex> parent = new HashMap<>();
		Deque<Vertex> queue = new ArrayDeque<>();
		Set<Vertex> visited = new LinkedHashSet<>();
		queue.add(from);
		visited.add(from);
		while (!queue.isEmpty()) {
			Vertex vertex = queue.poll();
			List<Vertex> successors = adjacencyMap.get(vertex);
			if (successors == null) {
				continue;
			}
			for (Vertex next : successors) {
				if (!visited.add(next)) {
					continue;
				}
				parent.put(next, vertex);
				boolean isTarget = toPseudoEnd ? next.isPseudoEndVertex() : toCover.contains(next);
				if (isTarget) {
					LinkedList<Vertex> path = new LinkedList<>();
					for (Vertex v = next; v != from; v = parent.get(v)) {
						path.addFirst(v);
					}
					return path;
				}
				if (toPseudoEnd && next.isPseudoStartVertex()) {
					continue;
				}
				queue.add(next);
			}
		}
		return null;
	}

	public void generateEulerCycle(ESG productESGFx) {
		reset();
		ESGFx graph = (productESGFx instanceof ESGFx) ? (ESGFx) productESGFx : new ESGFx(productESGFx);
		Vertex pseudoStart = graph.getPseudoStartVertex();
		Vertex pseudoEnd = graph.getPseudoEndVertex();
		buildAdjacency(graph, pseudoStart, pseudoEnd);

		List<Vertex> walk = new ArrayList<>();
		walk.add(pseudoStart);
		Vertex current = pseudoStart;

		while (!toCover.isEmpty()) {
			List<Vertex> path = shortestPath(current, false);
			if (path == null) {
				// The remaining events cannot be reached; the model is not well-formed.
				unreachableEvents.addAll(toCover);
				toCover.clear();
				break;
			}
			walk.addAll(path);
			toCover.removeAll(path);
			current = path.get(path.size() - 1);
		}

		if (!current.isPseudoStartVertex()) {
			List<Vertex> closing = shortestPath(current, true);
			if (closing == null) {
				throw new IllegalStateException("Event coverage: the pseudo-end vertex cannot be reached from "
						+ current.getEvent().getName());
			}
			walk.addAll(closing);
		} else {
			walk.remove(walk.size() - 1);
		}

		Collections.reverse(walk);
		eulerCycle.addAll(walk);
	}

	/** Events that could not be reached from the pseudo-start vertex in the last run. */
	public Set<Vertex> getUnreachableEvents() {
		return unreachableEvents;
	}

	public Set<Vertex> getToCover() {
		return toCover;
	}

	public List<Vertex> getEulerCycle() {
		return eulerCycle;
	}

	public Map<Vertex, List<Vertex>> getAdjacencyMap() {
		return adjacencyMap;
	}

}
