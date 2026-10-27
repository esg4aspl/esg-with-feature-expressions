package tr.edu.iyte.esgfx.staticanalysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.Conjunction;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.Negation;

/**
 * One solver instance per ESG-Fx model: the feature variables with the clauses
 * of Phi_d, and the structural clauses of the static analysis.
 *
 * <p>Variables, after the feature variables 1..n:
 * <ul>
 * <li>{@code s_x}, {@code e_xy}, {@code t_x}: a path from the pseudo-start to
 * the target inside the product (first path copy);</li>
 * <li>{@code s'_x}, {@code e'_xy}, {@code b'_x}: a path from a start vertex to
 * the pseudo-end inside the product (second path copy);</li>
 * <li>{@code r_x}: forward closure, x reachable from the pseudo-start;</li>
 * <li>{@code q_x}: backward closure, the pseudo-end reachable from x;</li>
 * <li>one activation literal per vertex whose guard is a conjunction, used to
 * assume the negation of the guard.</li>
 * </ul>
 *
 * <p>Nothing forces a path to exist; the questions switch the paths on through
 * assumptions, so the same clauses serve every question and the solver is
 * never reset.
 */
public final class StaticAnalysisEncoder {

	private final ESG esg;
	private final PhiD phiD;
	private final ISolver solver;

	private final List<Vertex> vertices;
	private final List<Edge> edges;
	private final Map<Integer, Integer> indexByVertexId = new HashMap<Integer, Integer>();
	private final List<String> labels = new ArrayList<String>();
	private final List<int[]> guardLiterals = new ArrayList<int[]>();
	private final List<List<Integer>> incomingEdges = new ArrayList<List<Integer>>();
	private final List<List<Integer>> outgoingEdges = new ArrayList<List<Integer>>();

	private final int start;
	private final int end;

	private final int[] s;
	private final int[] t;
	private final int[] r;
	private final int[] q;
	private final int[] s2;
	private final int[] b2;
	private final int[] e;
	private final int[] e2;
	private final int[] activation;

	private int clauseCount;
	private int cardinalityCount;
	private final long buildNanos;

	public StaticAnalysisEncoder(ESG esg, PhiD phiD) throws ContradictionException {
		long buildStart = System.nanoTime();
		this.esg = esg;
		this.phiD = phiD;
		this.vertices = new ArrayList<Vertex>(esg.getVertexList());
		this.edges = new ArrayList<Edge>(esg.getEdgeList());

		for (int i = 0; i < vertices.size(); i++) {
			indexByVertexId.put(vertices.get(i).getID(), i);
			incomingEdges.add(new ArrayList<Integer>());
			outgoingEdges.add(new ArrayList<Integer>());
		}
		for (int k = 0; k < edges.size(); k++) {
			outgoingEdges.get(indexOf(edges.get(k).getSource())).add(k);
			incomingEdges.get(indexOf(edges.get(k).getTarget())).add(k);
		}
		this.start = indexOf(requireVertex(esg.getPseudoStartVertex(), "pseudo-start"));
		this.end = indexOf(requireVertex(esg.getPseudoEndVertex(), "pseudo-end"));

		for (Vertex vertex : vertices) {
			guardLiterals.add(literalsOf(vertex));
		}
		labels.addAll(uniqueLabels(vertices));

		int n = vertices.size();
		int next = phiD.getVariableCount();
		s = new int[n];
		t = new int[n];
		r = new int[n];
		q = new int[n];
		s2 = new int[n];
		b2 = new int[n];
		activation = new int[n];
		for (int i = 0; i < n; i++) {
			s[i] = ++next;
			t[i] = ++next;
			r[i] = ++next;
			q[i] = ++next;
			s2[i] = ++next;
			b2[i] = ++next;
		}
		e = new int[edges.size()];
		e2 = new int[edges.size()];
		for (int k = 0; k < edges.size(); k++) {
			e[k] = ++next;
			e2[k] = ++next;
		}
		for (int i = 0; i < n; i++) {
			if (guardLiterals.get(i).length > 1) {
				activation[i] = ++next;
			}
		}

		solver = SolverFactory.newDefault();
		solver.newVar(next);

		for (int[] clause : phiD.getClauses()) {
			clause(clause);
		}
		addFirstPathCopy();
		addSecondPathCopy();
		addClosures();
		addGuardNegations();

		buildNanos = System.nanoTime() - buildStart;
	}

	// ---------------------------------------------------------------- clauses

	/** Clauses 1-4 of the brief: a path from the pseudo-start to the target. */
	private void addFirstPathCopy() throws ContradictionException {
		for (int x = 0; x < vertices.size(); x++) {
			for (int literal : guardLiterals.get(x)) {
				clause(-s[x], literal);
			}
		}
		for (int k = 0; k < edges.size(); k++) {
			clause(-e[k], s[edgeSource(k)]);
			clause(-e[k], s[edgeTarget(k)]);
		}
		for (int x = 0; x < vertices.size(); x++) {
			int[] in = edgeVariables(e, incomingEdges.get(x));
			int[] out = edgeVariables(e, outgoingEdges.get(x));
			if (x != start) {
				clause(prepend(in, -s[x]));
				atMostOne(in);
			}
			clause(prepend(out, -s[x], t[x]));
			atMostOne(out);
		}
	}

	/** Clauses 1'-4' of the brief: a path from the start vertex to the pseudo-end. */
	private void addSecondPathCopy() throws ContradictionException {
		for (int x = 0; x < vertices.size(); x++) {
			for (int literal : guardLiterals.get(x)) {
				clause(-s2[x], literal);
			}
		}
		for (int k = 0; k < edges.size(); k++) {
			clause(-e2[k], s2[edgeSource(k)]);
			clause(-e2[k], s2[edgeTarget(k)]);
		}
		for (int x = 0; x < vertices.size(); x++) {
			int[] in = edgeVariables(e2, incomingEdges.get(x));
			int[] out = edgeVariables(e2, outgoingEdges.get(x));
			clause(prepend(in, -s2[x], b2[x]));
			atMostOne(in);
			for (int edge : in) {
				clause(-b2[x], -edge);
			}
			if (x != end) {
				clause(prepend(out, -s2[x]));
				atMostOne(out);
			}
		}
	}

	/** Clauses 6-9 of the brief: forward and backward closure inside the product. */
	private void addClosures() throws ContradictionException {
		clause(r[start]);
		clause(q[end]);
		for (int k = 0; k < edges.size(); k++) {
			int x = edgeSource(k);
			int y = edgeTarget(k);
			// (r_x and mu(y)) -> r_y
			clause(closureClause(-r[x], guardLiterals.get(y), r[y]));
			// (q_y and mu(x)) -> q_x
			clause(closureClause(-q[y], guardLiterals.get(x), q[x]));
		}
	}

	/** For a conjunctive guard l_1..l_k: (a or not l_1 or ... or not l_k); assuming not a negates the guard. */
	private void addGuardNegations() throws ContradictionException {
		for (int x = 0; x < vertices.size(); x++) {
			if (activation[x] != 0) {
				int[] guard = guardLiterals.get(x);
				int[] literals = new int[guard.length + 1];
				literals[0] = activation[x];
				for (int i = 0; i < guard.length; i++) {
					literals[i + 1] = -guard[i];
				}
				clause(literals);
			}
		}
	}

	private static int[] closureClause(int premise, int[] guard, int conclusion) {
		int[] literals = new int[guard.length + 2];
		literals[0] = premise;
		for (int i = 0; i < guard.length; i++) {
			literals[i + 1] = -guard[i];
		}
		literals[guard.length + 1] = conclusion;
		return literals;
	}

	private void clause(int... literals) throws ContradictionException {
		solver.addClause(new VecInt(literals));
		clauseCount++;
	}

	private void atMostOne(int[] literals) throws ContradictionException {
		if (literals.length > 1) {
			solver.addAtMost(new VecInt(literals), 1);
			cardinalityCount++;
		}
	}

	private static int[] prepend(int[] rest, int... first) {
		int[] literals = new int[first.length + rest.length];
		System.arraycopy(first, 0, literals, 0, first.length);
		System.arraycopy(rest, 0, literals, first.length, rest.length);
		return literals;
	}

	private static int[] edgeVariables(int[] variables, List<Integer> edgeIndices) {
		int[] literals = new int[edgeIndices.size()];
		for (int i = 0; i < literals.length; i++) {
			literals[i] = variables[edgeIndices.get(i)];
		}
		return literals;
	}

	// ------------------------------------------------------------- guards

	/**
	 * The literals of a vertex guard: empty for the pseudo vertices (mu = true),
	 * one literal for a feature or its negation, one per conjunct for a
	 * conjunction of literals.
	 */
	private int[] literalsOf(Vertex vertex) {
		if (vertex.isPseudoStartVertex() || vertex.isPseudoEndVertex()) {
			return new int[0];
		}
		if (!(vertex instanceof VertexRefinedByFeatureExpression)) {
			throw new UnsupportedOperationException(
					"Vertex of event '" + vertex.getEvent().getName() + "' carries no feature expression");
		}
		FeatureExpression guard = ((VertexRefinedByFeatureExpression) vertex).getFeatureExpression();
		List<Integer> literals = new ArrayList<Integer>();
		collectLiterals(guard, vertex, literals);
		int[] result = new int[literals.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = literals.get(i);
		}
		return result;
	}

	private void collectLiterals(FeatureExpression expression, Vertex vertex, List<Integer> literals) {
		if (expression instanceof Conjunction) {
			for (FeatureExpression operand : ((Conjunction) expression).getOperands()) {
				if (operand instanceof Conjunction) {
					collectLiterals(operand, vertex, literals);
				} else {
					literals.add(literalOf(operand, vertex));
				}
			}
			return;
		}
		literals.add(literalOf(expression, vertex));
	}

	private int literalOf(FeatureExpression expression, Vertex vertex) {
		if (expression instanceof Negation) {
			FeatureExpression inner = ((Negation) expression).getFeatureExpression();
			if (inner.getClass() != FeatureExpression.class) {
				throw unsupported(vertex, expression);
			}
			return -variableOf(inner, vertex);
		}
		if (expression.getClass() != FeatureExpression.class) {
			throw unsupported(vertex, expression);
		}
		return variableOf(expression, vertex);
	}

	private int variableOf(FeatureExpression expression, Vertex vertex) {
		String name = expression.getFeature() == null ? null : expression.getFeature().getName();
		Integer variable = name == null ? null : phiD.variableOf(name);
		if (variable == null) {
			throw new IllegalStateException("Guard of event '" + vertex.getEvent().getName() + "' names feature '"
					+ name + "', which is not a feature variable");
		}
		return variable;
	}

	private static UnsupportedOperationException unsupported(Vertex vertex, FeatureExpression expression) {
		// The expression's own toString is not safe for every expression class, so it is not used here.
		return new UnsupportedOperationException("Guard of event '" + vertex.getEvent().getName() + "' is a "
				+ expression.getClass().getSimpleName()
				+ "; the static analysis supports literals and conjunctions of literals only");
	}

	// ---------------------------------------------------------- assumptions

	/** {s_[, s_v, t_v} and not t_x for every other x. */
	public int[] target(int v) {
		int[] literals = new int[vertices.size() + 2];
		int k = 0;
		literals[k++] = s[start];
		literals[k++] = s[v];
		for (int x = 0; x < vertices.size(); x++) {
			literals[k++] = x == v ? t[x] : -t[x];
		}
		return literals;
	}

	/** {s'_v, b'_v} and not b'_x for every other x. */
	public int[] source(int v) {
		int[] literals = new int[vertices.size() + 1];
		int k = 0;
		literals[k++] = s2[v];
		for (int x = 0; x < vertices.size(); x++) {
			literals[k++] = x == v ? b2[x] : -b2[x];
		}
		return literals;
	}

	/** The guard of v as assumptions, one literal per conjunct. */
	public int[] guard(int v) {
		return guardLiterals.get(v).clone();
	}

	/** The negated guard of v as assumptions; null when the guard is true. */
	public int[] negatedGuard(int v) {
		int[] guard = guardLiterals.get(v);
		if (guard.length == 0) {
			return null;
		}
		if (guard.length == 1) {
			return new int[] { -guard[0] };
		}
		return new int[] { -activation[v] };
	}

	public int notReachable(int v) {
		return -r[v];
	}

	public int notCoreachable(int v) {
		return -q[v];
	}

	public static int[] union(int[]... parts) {
		int size = 0;
		for (int[] part : parts) {
			size += part.length;
		}
		int[] literals = new int[size];
		int k = 0;
		for (int[] part : parts) {
			System.arraycopy(part, 0, literals, k, part.length);
			k += part.length;
		}
		return literals;
	}

	// ------------------------------------------------------------- solving

	public boolean isSatisfiable(int[] assumptions) {
		try {
			return solver.isSatisfiable(new VecInt(assumptions));
		} catch (TimeoutException ex) {
			throw new IllegalStateException("Solver timed out on " + esg.getName(), ex);
		}
	}

	/** Features true in the last model, abstract ones included, in variable order. */
	public List<String> configuration() {
		return phiD.selection(solver.model());
	}

	/** Concrete features true in the last model, in variable order: the product. */
	public List<String> witness() {
		return phiD.concreteSelection(solver.model());
	}

	/**
	 * Counts the distinct products (assignments to the concrete variables) of the
	 * models of Phi_d, the structural clauses and the assumptions, with blocking
	 * clauses over the concrete variables. The blocking clauses stay in the
	 * solver, so this encoder must not be used for anything else afterwards.
	 */
	public long countProductsAndConsume(int[] assumptions, long limit) throws Exception {
		return ProductEnumerator.enumerate(solver, assumptions, phiD.getConcreteVariables(), null, limit);
	}

	/** Whether a fully fixed configuration satisfies Phi_d (all features assumed). */
	public boolean satisfiesPhiD(IVecInt configuration) {
		try {
			return solver.isSatisfiable(configuration);
		} catch (TimeoutException ex) {
			throw new IllegalStateException(ex);
		}
	}

	// ------------------------------------------------------------ structure

	public ESG getEsg() {
		return esg;
	}

	public PhiD getPhiD() {
		return phiD;
	}

	public int vertexCount() {
		return vertices.size();
	}

	public int edgeCount() {
		return edges.size();
	}

	public Vertex vertex(int index) {
		return vertices.get(index);
	}

	public Edge edge(int index) {
		return edges.get(index);
	}

	public int edgeSource(int edge) {
		return indexOf(edges.get(edge).getSource());
	}

	public int edgeTarget(int edge) {
		return indexOf(edges.get(edge).getTarget());
	}

	public List<Integer> incoming(int v) {
		return Collections.unmodifiableList(incomingEdges.get(v));
	}

	public int startIndex() {
		return start;
	}

	public int endIndex() {
		return end;
	}

	public boolean isPseudo(int v) {
		return v == start || v == end;
	}

	public int indexOf(Vertex vertex) {
		Integer index = indexByVertexId.get(vertex.getID());
		if (index == null) {
			throw new IllegalArgumentException("Vertex " + vertex + " is not in " + esg.getName());
		}
		return index;
	}

	/** Event name, or event name and guard when several vertices share the event name. */
	public String label(int v) {
		return labels.get(v);
	}

	public String edgeLabel(int edge) {
		return label(edgeSource(edge)) + "->" + label(edgeTarget(edge));
	}

	public int getClauseCount() {
		return clauseCount;
	}

	public int getCardinalityConstraintCount() {
		return cardinalityCount;
	}

	public int getVariableCount() {
		return solver.nVars();
	}

	public long getBuildNanos() {
		return buildNanos;
	}

	private static Vertex requireVertex(Vertex vertex, String what) {
		if (vertex == null) {
			throw new IllegalArgumentException("Model has no " + what + " vertex");
		}
		return vertex;
	}

	private static List<String> uniqueLabels(List<Vertex> vertices) {
		Map<String, Integer> occurrences = new LinkedHashMap<String, Integer>();
		for (Vertex vertex : vertices) {
			occurrences.merge(vertex.getEvent().getName(), 1, Integer::sum);
		}
		List<String> labels = new ArrayList<String>();
		for (Vertex vertex : vertices) {
			String name = vertex.getEvent().getName();
			labels.add(occurrences.get(name) > 1 ? vertex.toString() : name);
		}
		return labels;
	}
}
