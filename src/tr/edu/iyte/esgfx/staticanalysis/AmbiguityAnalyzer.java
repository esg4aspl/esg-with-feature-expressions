package tr.edu.iyte.esgfx.staticanalysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Asks the questions of the static analysis on one encoded model, one
 * {@code isSatisfiable(assumptions)} call per question, and collects the
 * findings with their witnesses.
 */
public final class AmbiguityAnalyzer {

	public static final String DEAD_EVENT = "dead-event";
	public static final String DEAD_EDGE = "dead-edge";
	public static final String CONTAINED = "contained";
	public static final String FALSE_OPTIONAL = "false-optional";
	public static final String CORE = "core";
	public static final String HIDDEN_UNREACHABLE = "hidden-unreachable";
	public static final String HIDDEN_DEAD_END = "hidden-dead-end";
	public static final String NO_COMPLETE_SEQUENCE = "no-complete-sequence";

	public static final String CAUSE_UNREACHABLE = "unreachable";
	public static final String CAUSE_NO_COMPLETION = "no-completion";
	/** False optional because the feature model alone implies the guard. */
	public static final String CAUSE_CORE = "core";
	/** False optional because the guards on every path to the event imply it, the feature model alone does not. */
	public static final String CAUSE_PREDECESSORS = "predecessors";

	/** One solver call. */
	public static final class Question {
		public final String kind;
		public final String element;
		public final boolean satisfiable;
		public final long nanos;

		Question(String kind, String element, boolean satisfiable, long nanos) {
			this.kind = kind;
			this.element = element;
			this.satisfiable = satisfiable;
			this.nanos = nanos;
		}
	}

	/**
	 * One finding. For the SAT kinds (contained, hidden unreachable, hidden dead
	 * end, no complete sequence) {@code witness} is the product, the selected
	 * concrete features, and {@code configuration} the full solver model it came
	 * from, abstract features included; both are null for the UNSAT kinds.
	 */
	public static final class Finding {
		public final String kind;
		public final String element;
		public final String cause;
		public final List<String> witness;
		public final List<String> configuration;
		/** Vertex index for event findings, edge index for edge findings. */
		public final int index;
		private Boolean verified;
		private long products = -1;

		Finding(String kind, String element, String cause, List<String> witness, List<String> configuration,
				int index) {
			this.kind = kind;
			this.element = element;
			this.cause = cause;
			this.witness = witness == null ? null : Collections.unmodifiableList(witness);
			this.configuration = configuration == null ? null : Collections.unmodifiableList(configuration);
			this.index = index;
		}

		public Boolean getVerified() {
			return verified;
		}

		void setVerified(boolean verified) {
			this.verified = verified;
		}

		/** Number of products in which the finding appears, or -1 when not counted. */
		public long getProducts() {
			return products;
		}

		public void setProducts(long products) {
			this.products = products;
		}

		@Override
		public String toString() {
			return kind + " " + element + (cause == null ? "" : " (" + cause + ")")
					+ (witness == null ? "" : " " + witness);
		}
	}

	private final StaticAnalysisEncoder encoder;
	private final List<Question> questions = new ArrayList<Question>();
	private final List<Finding> findings = new ArrayList<Finding>();
	private Boolean[] targetSatisfiable;
	private long analysisNanos;

	public AmbiguityAnalyzer(StaticAnalysisEncoder encoder) {
		this.encoder = encoder;
	}

	public List<Finding> analyze() {
		long started = System.nanoTime();
		questions.clear();
		findings.clear();
		int n = encoder.vertexCount();
		targetSatisfiable = new Boolean[n];

		boolean[] deadEvent = new boolean[n];
		for (int v = 0; v < n; v++) {
			if (!encoder.isPseudo(v)) {
				deadEvent[v] = askDeadEvent(v);
			}
		}
		for (int k = 0; k < encoder.edgeCount(); k++) {
			askDeadEdge(k);
		}
		for (int v = 0; v < n; v++) {
			if (deadEvent[v]) {
				askContained(v);
			}
		}
		for (int v = 0; v < n; v++) {
			if (!encoder.isPseudo(v) && encoder.negatedGuard(v) != null) {
				boolean core = askCore(v);
				askFalseOptional(v, core);
			}
		}
		// Definitions 7.7 and 7.8: the hidden kinds are asked of events that are not dead.
		for (int v = 0; v < n; v++) {
			if (!encoder.isPseudo(v) && !deadEvent[v]) {
				askHiddenUnreachable(v);
				askHiddenDeadEnd(v);
			}
		}
		askNoCompleteSequence();
		analysisNanos = System.nanoTime() - started;
		return findings;
	}

	private boolean askDeadEvent(int v) {
		String element = encoder.label(v);
		boolean sat = ask(DEAD_EVENT, element, StaticAnalysisEncoder.union(encoder.target(v), encoder.source(v)));
		if (sat) {
			return false;
		}
		String cause = isTargetSatisfiable(v) ? CAUSE_NO_COMPLETION : CAUSE_UNREACHABLE;
		findings.add(new Finding(DEAD_EVENT, element, cause, null, null, v));
		return true;
	}

	private void askDeadEdge(int k) {
		int u = encoder.edgeSource(k);
		int v = encoder.edgeTarget(k);
		String element = encoder.edgeLabel(k);
		if (ask(DEAD_EDGE, element, StaticAnalysisEncoder.union(encoder.target(u), encoder.source(v)))) {
			return;
		}
		boolean reachable = ask(DEAD_EDGE + "-cause", element,
				StaticAnalysisEncoder.union(encoder.target(u), encoder.guard(v)));
		findings.add(new Finding(DEAD_EDGE, element, reachable ? CAUSE_NO_COMPLETION : CAUSE_UNREACHABLE, null,
				null, k));
	}

	private void askContained(int v) {
		String element = encoder.label(v);
		if (ask(CONTAINED, element, encoder.guard(v))) {
			findings.add(new Finding(CONTAINED, element, null, encoder.witness(), encoder.configuration(), v));
		}
	}

	/** Auxiliary: the feature model alone implies the guard of v. */
	private boolean askCore(int v) {
		String element = encoder.label(v);
		if (!ask(CORE, element, encoder.negatedGuard(v))) {
			findings.add(new Finding(CORE, element, null, null, null, v));
			return true;
		}
		return false;
	}

	/**
	 * Definition 7.6, asked per predecessor: mu(v) is not true, (a) some
	 * predecessor u has target(u) SAT, and (b) for every predecessor u, target(u)
	 * with the negated guard of v is UNSAT. The cause records the source of the
	 * redundancy: the feature model alone (core) or the guards on the paths.
	 */
	private void askFalseOptional(int v, boolean core) {
		String element = encoder.label(v);
		Set<Integer> predecessors = new LinkedHashSet<Integer>();
		for (int k : encoder.incoming(v)) {
			predecessors.add(encoder.edgeSource(k));
		}
		boolean someReachable = false;
		for (int u : predecessors) {
			if (isTargetSatisfiable(u)) {
				someReachable = true;
				break;
			}
		}
		if (!someReachable) {
			return;
		}
		int[] negated = encoder.negatedGuard(v);
		for (int u : predecessors) {
			if (ask(FALSE_OPTIONAL, element + "|" + encoder.label(u),
					StaticAnalysisEncoder.union(encoder.target(u), negated))) {
				return;
			}
		}
		findings.add(new Finding(FALSE_OPTIONAL, element, core ? CAUSE_CORE : CAUSE_PREDECESSORS, null, null, v));
	}

	private void askHiddenUnreachable(int v) {
		String element = encoder.label(v);
		int[] assumptions = StaticAnalysisEncoder.union(encoder.guard(v), new int[] { encoder.notReachable(v) });
		if (ask(HIDDEN_UNREACHABLE, element, assumptions)) {
			findings.add(new Finding(HIDDEN_UNREACHABLE, element, null, encoder.witness(), encoder.configuration(), v));
		}
	}

	private void askHiddenDeadEnd(int v) {
		String element = encoder.label(v);
		int[] assumptions = StaticAnalysisEncoder.union(encoder.target(v), new int[] { encoder.notCoreachable(v) });
		if (ask(HIDDEN_DEAD_END, element, assumptions)) {
			findings.add(new Finding(HIDDEN_DEAD_END, element, null, encoder.witness(), encoder.configuration(), v));
		}
	}

	private void askNoCompleteSequence() {
		int start = encoder.startIndex();
		String element = encoder.label(start);
		if (ask(NO_COMPLETE_SEQUENCE, element, new int[] { encoder.notCoreachable(start) })) {
			findings.add(new Finding(NO_COMPLETE_SEQUENCE, element, null, encoder.witness(), encoder.configuration(),
					start));
		}
	}

	/**
	 * The assumptions of the question that reports a SAT finding, so that the
	 * products in which it appears can be counted on a fresh encoder.
	 */
	public static int[] assumptionsOf(StaticAnalysisEncoder encoder, Finding finding) {
		int v = finding.index;
		if (finding.kind.equals(HIDDEN_UNREACHABLE)) {
			return StaticAnalysisEncoder.union(encoder.guard(v), new int[] { encoder.notReachable(v) });
		}
		if (finding.kind.equals(HIDDEN_DEAD_END)) {
			return StaticAnalysisEncoder.union(encoder.target(v), new int[] { encoder.notCoreachable(v) });
		}
		if (finding.kind.equals(NO_COMPLETE_SEQUENCE)) {
			return new int[] { encoder.notCoreachable(encoder.startIndex()) };
		}
		if (finding.kind.equals(CONTAINED)) {
			return encoder.guard(v);
		}
		throw new IllegalArgumentException("No product count for kind " + finding.kind);
	}

	/** target(v) alone; asked once per vertex and reused by the cause and false optional questions. */
	private boolean isTargetSatisfiable(int v) {
		if (targetSatisfiable[v] == null) {
			targetSatisfiable[v] = ask("reachable", encoder.label(v), encoder.target(v));
		}
		return targetSatisfiable[v];
	}

	private boolean ask(String kind, String element, int[] assumptions) {
		long started = System.nanoTime();
		boolean sat = encoder.isSatisfiable(assumptions);
		questions.add(new Question(kind, element, sat, System.nanoTime() - started));
		return sat;
	}

	public List<Question> getQuestions() {
		return questions;
	}

	public List<Finding> getFindings() {
		return findings;
	}

	public long getAnalysisNanos() {
		return analysisNanos;
	}

	public StaticAnalysisEncoder getEncoder() {
		return encoder;
	}
}
