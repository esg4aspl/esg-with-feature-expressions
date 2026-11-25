package tr.edu.iyte.esgfx.testgeneration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.sequenceesgfx.VertexRefinedBySequence;

/**
 * Coverage and replay of a test suite on a product-level model, by vertex
 * identity.
 *
 * A suite is a set of event sequences. A sequence generated on a
 * transformed graph (event-triple and event-quadruple coverage) is a
 * sequence of vertices that each stand for several events; unfold() turns
 * it into the sequence of events it describes. All measurements here are
 * made on unfolded sequences and on the product-level model, never on the
 * transformed graph.
 *
 * For a length k, the sequences to cover are the walks of k events of the
 * product-level model: k = 1 gives event coverage, k = 2 event-couple
 * coverage, k = 3 event-triple coverage and k = 4 event-quadruple
 * coverage. Their number is counted without listing them.
 */
public class SequenceCoverageAnalyser {

	/** Result of one coverage measurement. */
	public static class Coverage {
		public final int length;
		public final long required;
		public final long covered;

		Coverage(int length, long required, long covered) {
			this.length = length;
			this.required = required;
			this.covered = covered;
		}

		public double percentage() {
			return required == 0 ? 100.0 : 100.0 * covered / required;
		}
	}

	/** Result of replaying a suite on a product-level model. */
	public static class Replay {
		public int sequences;
		public int events;
		public int badStart;
		public int badEnd;
		public int badSteps;
		public int unknownVertices;
		public int infeasibleSequences;

		public int violations() {
			return badStart + badEnd + badSteps + unknownVertices;
		}
	}

	private final ESGFx product;
	private final Map<Integer, Vertex> eventById = new HashMap<>();
	private final Map<Integer, Set<Integer>> successors = new HashMap<>();
	private final Set<Integer> entryEvents = new HashSet<>();
	private final Set<Integer> exitEvents = new HashSet<>();

	public SequenceCoverageAnalyser(ESG productESGFx) {
		this.product = (productESGFx instanceof ESGFx) ? (ESGFx) productESGFx : new ESGFx(productESGFx);
		for (Vertex vertex : product.getVertexList()) {
			if (isEnabledEvent(vertex)) {
				eventById.put(vertex.getID(), vertex);
			}
		}
		for (Vertex vertex : eventById.values()) {
			Set<Integer> next = new LinkedHashSet<>();
			for (Vertex target : product.getAdjacencyList(vertex)) {
				if (target.isPseudoEndVertex()) {
					exitEvents.add(vertex.getID());
				} else if (eventById.containsKey(target.getID())) {
					next.add(target.getID());
				}
			}
			successors.put(vertex.getID(), next);
		}
		Vertex pseudoStart = product.getPseudoStartVertex();
		if (pseudoStart != null) {
			for (Vertex target : product.getAdjacencyList(pseudoStart)) {
				if (eventById.containsKey(target.getID())) {
					entryEvents.add(target.getID());
				}
			}
		}
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
	 * Turns every sequence of a suite into the sequence of events it describes.
	 * The first vertex of a sequence gives all its events and every later
	 * vertex gives its last event, because consecutive vertices of a
	 * transformed graph overlap in all but one event. A sequence of ordinary
	 * vertices is returned unchanged.
	 */
	public static Set<EventSequence> unfold(Set<EventSequence> suite) {
		Set<EventSequence> unfolded = new LinkedHashSet<>();
		if (suite == null) {
			return unfolded;
		}
		for (EventSequence sequence : suite) {
			if (sequence == null || sequence.getEventSequence() == null || sequence.getEventSequence().isEmpty()) {
				continue;
			}
			List<Vertex> events = new ArrayList<>();
			boolean first = true;
			for (Vertex vertex : sequence.getEventSequence()) {
				if (vertex instanceof VertexRefinedBySequence
						&& ((VertexRefinedBySequence) vertex).getSequence().getSize() > 0) {
					VertexRefinedBySequence sequenceVertex = (VertexRefinedBySequence) vertex;
					int size = sequenceVertex.getSequence().getSize();
					if (first) {
						for (int i = 0; i < size; i++) {
							events.add(sequenceVertex.getSequence().getElement(i));
						}
					} else {
						events.add(sequenceVertex.getSequence().getElement(size - 1));
					}
				} else {
					events.add(vertex);
				}
				first = false;
			}
			EventSequence eventSequence = new EventSequence();
			eventSequence.setEventSequence(events);
			unfolded.add(eventSequence);
		}
		return unfolded;
	}

	/** Number of events in a suite of unfolded sequences. */
	public static int numberOfEvents(Set<EventSequence> unfoldedSuite) {
		int events = 0;
		for (EventSequence sequence : unfoldedSuite) {
			events += sequence.length();
		}
		return events;
	}

	/** Number of walks of k events in the product-level model. */
	public long numberOfWalks(int k) {
		Map<Integer, Long> endingAt = new HashMap<>();
		for (Integer id : eventById.keySet()) {
			endingAt.put(id, 1L);
		}
		for (int length = 2; length <= k; length++) {
			Map<Integer, Long> next = new HashMap<>();
			for (Map.Entry<Integer, Long> entry : endingAt.entrySet()) {
				for (Integer target : successors.get(entry.getKey())) {
					next.merge(target, entry.getValue(), Long::sum);
				}
			}
			endingAt = next;
		}
		long total = 0;
		for (Long count : endingAt.values()) {
			total += count;
		}
		return total;
	}

	/**
	 * Coverage of the walks of k events by a suite of unfolded sequences. A
	 * window of k consecutive events of a sequence counts only if it is a walk
	 * of the product-level model.
	 */
	public Coverage coverage(Set<EventSequence> unfoldedSuite, int k) {
		Set<String> covered = new HashSet<>();
		for (EventSequence sequence : unfoldedSuite) {
			List<Vertex> events = sequence.getEventSequence();
			for (int i = 0; i + k <= events.size(); i++) {
				StringBuilder key = new StringBuilder();
				boolean walk = eventById.containsKey(events.get(i).getID());
				for (int j = 0; walk && j < k; j++) {
					int id = events.get(i + j).getID();
					if (j > 0) {
						walk = successors.get(events.get(i + j - 1).getID()).contains(id);
						key.append(',');
					}
					key.append(id);
				}
				if (walk) {
					covered.add(key.toString());
				}
			}
		}
		return new Coverage(k, numberOfWalks(k), covered.size());
	}

	/** Result of the sequence-omission analysis of one suite for one length. */
	public static class Omission {
		public final int length;
		/** One mutant for every walk of 'length' events of the product-level model. */
		public final long mutants;
		/**
		 * For every killed mutant, the number of events of the suite that have been
		 * run when the mutant is killed, in increasing order.
		 */
		public final int[] eventsToKill;
		public final int eventsInSuite;

		Omission(int length, long mutants, int[] eventsToKill, int eventsInSuite) {
			this.length = length;
			this.mutants = mutants;
			this.eventsToKill = eventsToKill;
			this.eventsInSuite = eventsInSuite;
		}

		public int killed() {
			return eventsToKill.length;
		}
	}

	/**
	 * Sequence omission: a mutant forbids one walk of k events, that is, it omits
	 * one edge of the graph whose vertices are the walks of k - 1 events. A test
	 * sequence fails on the mutant at the first place where it contains the
	 * forbidden walk, so a suite kills the mutant when the last event of the
	 * first occurrence of the walk is run. With k = 2 this is edge omission.
	 */
	public Omission sequenceOmission(Set<EventSequence> unfoldedSuite, int k) {
		Map<String, Integer> firstKill = new HashMap<>();
		int position = 0;
		int eventsInSuite = 0;
		for (EventSequence sequence : unfoldedSuite) {
			List<Vertex> events = sequence.getEventSequence();
			eventsInSuite += events.size();
			for (int end = 0; end < events.size(); end++) {
				position++;
				// A sequence is run up to its first step that the product-level model
				// does not allow; the event of that step is counted, the rest is not run.
				boolean allowed = eventById.containsKey(events.get(end).getID()) && (end == 0
						|| successors.get(events.get(end - 1).getID()).contains(events.get(end).getID()));
				if (!allowed) {
					break;
				}
				int start = end - k + 1;
				if (start < 0) {
					continue;
				}
				StringBuilder key = new StringBuilder();
				for (int j = start; j <= end; j++) {
					if (j > start) {
						key.append(',');
					}
					key.append(events.get(j).getID());
				}
				firstKill.putIfAbsent(key.toString(), position);
			}
		}
		int[] eventsToKill = new int[firstKill.size()];
		int i = 0;
		for (Integer value : firstKill.values()) {
			eventsToKill[i++] = value;
		}
		java.util.Arrays.sort(eventsToKill);
		return new Omission(k, numberOfWalks(k), eventsToKill, eventsInSuite);
	}

	/**
	 * Replays a suite of unfolded sequences on the product-level model: the
	 * first event must follow the pseudo-start vertex, the last event must be
	 * followed by the pseudo-end vertex, and every two consecutive events must
	 * be joined by an edge.
	 */
	public Replay replay(Set<EventSequence> unfoldedSuite) {
		Replay result = new Replay();
		for (EventSequence sequence : unfoldedSuite) {
			List<Vertex> events = sequence.getEventSequence();
			result.sequences++;
			result.events += events.size();
			int before = result.violations();
			if (events.isEmpty()) {
				result.badStart++;
				result.infeasibleSequences++;
				continue;
			}
			for (Vertex event : events) {
				if (!eventById.containsKey(event.getID())) {
					result.unknownVertices++;
				}
			}
			if (!entryEvents.contains(events.get(0).getID())) {
				result.badStart++;
			}
			if (!exitEvents.contains(events.get(events.size() - 1).getID())) {
				result.badEnd++;
			}
			for (int i = 1; i < events.size(); i++) {
				Set<Integer> next = successors.get(events.get(i - 1).getID());
				if (next == null || !next.contains(events.get(i).getID())) {
					result.badSteps++;
				}
			}
			if (result.violations() > before) {
				result.infeasibleSequences++;
			}
		}
		return result;
	}
}
