package tr.edu.iyte.esgfx.staticanalysis;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.Vertex;

/**
 * Two plain traversals on one projected model: forward from the pseudo-start
 * and backward from the pseudo-end. Vertices are identified by their position
 * in the vertex list of the projection.
 */
public final class ProjectionWellFormedness {

	private final ESG projection;
	private final List<Vertex> vertices;
	private final Map<Integer, Integer> index = new HashMap<Integer, Integer>();
	private final boolean[] forward;
	private final boolean[] backward;
	private final int start;
	private final int end;

	private ProjectionWellFormedness(ESG projection) {
		this.projection = projection;
		this.vertices = new ArrayList<Vertex>(projection.getVertexList());
		for (int i = 0; i < vertices.size(); i++) {
			index.put(vertices.get(i).getID(), i);
		}
		List<List<Integer>> successors = new ArrayList<List<Integer>>();
		List<List<Integer>> predecessors = new ArrayList<List<Integer>>();
		for (int i = 0; i < vertices.size(); i++) {
			successors.add(new ArrayList<Integer>());
			predecessors.add(new ArrayList<Integer>());
		}
		int startIndex = -1;
		int endIndex = -1;
		for (int i = 0; i < vertices.size(); i++) {
			if (vertices.get(i).isPseudoStartVertex()) {
				startIndex = i;
			}
			if (vertices.get(i).isPseudoEndVertex()) {
				endIndex = i;
			}
		}
		for (Edge edge : projection.getEdgeList()) {
			int x = index.get(edge.getSource().getID());
			int y = index.get(edge.getTarget().getID());
			successors.get(x).add(y);
			predecessors.get(y).add(x);
		}
		this.start = startIndex;
		this.end = endIndex;
		this.forward = traverse(startIndex, successors);
		this.backward = traverse(endIndex, predecessors);
	}

	public static ProjectionWellFormedness check(ESG projection) {
		return new ProjectionWellFormedness(projection);
	}

	private boolean[] traverse(int from, List<List<Integer>> next) {
		boolean[] seen = new boolean[vertices.size()];
		if (from < 0) {
			return seen;
		}
		Deque<Integer> pending = new ArrayDeque<Integer>();
		seen[from] = true;
		pending.add(from);
		while (!pending.isEmpty()) {
			int x = pending.poll();
			for (int y : next.get(x)) {
				if (!seen[y]) {
					seen[y] = true;
					pending.add(y);
				}
			}
		}
		return seen;
	}

	/** Every event reachable from the pseudo-start and the pseudo-end reachable from every event. */
	public boolean isWellFormed() {
		for (int i = 0; i < vertices.size(); i++) {
			if (!forward[i] || !backward[i]) {
				return false;
			}
		}
		return true;
	}

	public boolean isEndReachable() {
		return end >= 0 && forward[end];
	}

	/** Events of the projection not reachable from the pseudo-start. */
	public List<Vertex> getUnreachable() {
		return select(forward, false);
	}

	/** Events of the projection from which the pseudo-end is not reachable. */
	public List<Vertex> getNotCoreachable() {
		return select(backward, false);
	}

	public Set<Vertex> getForwardSet() {
		return new HashSet<Vertex>(select(forward, true));
	}

	public Set<Vertex> getBackwardSet() {
		return new HashSet<Vertex>(select(backward, true));
	}

	public boolean isReachable(Vertex vertex) {
		return forward[indexOf(vertex)];
	}

	public boolean isCoreachable(Vertex vertex) {
		return backward[indexOf(vertex)];
	}

	public List<Vertex> getVertices() {
		return Collections.unmodifiableList(vertices);
	}

	public ESG getProjection() {
		return projection;
	}

	private int indexOf(Vertex vertex) {
		Integer i = index.get(vertex.getID());
		if (i == null) {
			throw new IllegalArgumentException("Vertex " + vertex.getEvent().getName() + " is not in the projection");
		}
		return i;
	}

	private List<Vertex> select(boolean[] flags, boolean value) {
		List<Vertex> selected = new ArrayList<Vertex>();
		for (int i = 0; i < vertices.size(); i++) {
			if (i != start && i != end && flags[i] == value) {
				selected.add(vertices.get(i));
			}
		}
		return selected;
	}
}
