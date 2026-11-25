package tr.edu.iyte.esgfx.staticanalysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static tr.edu.iyte.esgfx.staticanalysis.AmbiguityAnalyzer.CONTAINED;
import static tr.edu.iyte.esgfx.staticanalysis.AmbiguityAnalyzer.CORE;
import static tr.edu.iyte.esgfx.staticanalysis.AmbiguityAnalyzer.DEAD_EDGE;
import static tr.edu.iyte.esgfx.staticanalysis.AmbiguityAnalyzer.DEAD_EVENT;
import static tr.edu.iyte.esgfx.staticanalysis.AmbiguityAnalyzer.FALSE_OPTIONAL;
import static tr.edu.iyte.esgfx.staticanalysis.AmbiguityAnalyzer.HIDDEN_DEAD_END;
import static tr.edu.iyte.esgfx.staticanalysis.AmbiguityAnalyzer.HIDDEN_UNREACHABLE;
import static tr.edu.iyte.esgfx.staticanalysis.AmbiguityAnalyzer.NO_COMPLETE_SEQUENCE;
import static tr.edu.iyte.esgfx.staticanalysis.TestModels.clauses;
import static tr.edu.iyte.esgfx.staticanalysis.TestModels.describe;
import static tr.edu.iyte.esgfx.staticanalysis.TestModels.guards;
import static tr.edu.iyte.esgfx.staticanalysis.TestModels.set;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.api.LoadedSplModel;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.Negation;

/**
 * Hand-checked expectations for the encoding (brief Section 10). SVM feature
 * names: s = Soda, t = Tea, f = FreeDrinks, c = CancelPurchase.
 */
public class StaticAnalysisEncoderTest {

	private static final String[] FIVE_KINDS_AND_CONTAINED = { DEAD_EVENT, DEAD_EDGE, HIDDEN_UNREACHABLE,
			HIDDEN_DEAD_END, NO_COMPLETE_SEQUENCE, CONTAINED };

	@Test
	public void svmUnmodifiedHasNoAmbiguity() throws Exception {
		LoadedSplModel svm = TestModels.svm();
		AmbiguityAnalyzer analyzer = TestModels.analyze(svm.getEsgFx(), PhiD.of(svm));

		assertEquals(15, analyzer.getEncoder().vertexCount());
		assertEquals(21, analyzer.getEncoder().edgeCount());
		assertEquals(Collections.emptySet(), describe(analyzer.getFindings(), FIVE_KINDS_AND_CONTAINED));
		assertEquals(Collections.emptySet(), describe(analyzer.getFindings(), CORE));
		// Each of these events has only predecessors whose guard already requires its own guard.
		assertEquals(set("false-optional serveSoda predecessors", "false-optional serveTea predecessors",
				"false-optional return predecessors", "false-optional change predecessors",
				"false-optional take/!f predecessors", "false-optional close predecessors"),
				describe(analyzer.getFindings(), FALSE_OPTIONAL));
	}

	/** Chapter 7 example, panel e of svm_ambiguities.svg: the guard of return changed to !c. */
	@Test
	public void svmReturnGuardNegated() throws Exception {
		LoadedSplModel svm = TestModels.svm();
		ESG changed = withGuard(svm, "return", new Negation(svm.getFeatureExpressionMap().get("c")));
		AmbiguityAnalyzer analyzer = TestModels.analyze(changed, PhiD.of(svm));

		assertEquals(set(
				"dead-event cancel no-completion",
				"dead-event return unreachable",
				"dead-edge change->cancel no-completion",
				"dead-edge free->cancel no-completion",
				"dead-edge cancel->return unreachable",
				"dead-edge return->] unreachable",
				"contained cancel",
				"contained return"),
				describe(analyzer.getFindings(), FIVE_KINDS_AND_CONTAINED));

		for (AmbiguityAnalyzer.Finding finding : analyzer.getFindings()) {
			if (finding.element.equals("cancel") && finding.witness != null) {
				assertTrue(finding + " needs c", finding.witness.contains("c"));
			}
			if (finding.element.equals("return") && finding.witness != null) {
				assertFalse(finding + " must not have c", finding.witness.contains("c"));
			}
		}
	}

	/** [ -> a, a -> v, v -> a, a -> ]: the walk [ a v a ] contains v, although no simple path does. */
	@Test
	public void walksNotSimplePaths() throws Exception {
		TestModels.Small model = new TestModels.Small(Arrays.asList("x"), clauses(new int[] { 1 }),
				guards("a", "x", "v", "x"), "[->a", "a->v", "v->a", "a->]");
		AmbiguityAnalyzer analyzer = TestModels.analyze(model.esg, model.phiD);
		assertEquals(Collections.emptySet(), describe(analyzer.getFindings(), FIVE_KINDS_AND_CONTAINED));
	}

	/** mu(u) = f, mu(v) = !f on the edge (u, v); v has no other predecessor. */
	@Test
	public void contradictoryJoinGivesDeadEdgeAndDeadEvent() throws Exception {
		TestModels.Small model = new TestModels.Small(Arrays.asList("f"), clauses(),
				guards("u", "f", "v", "!f", "w", "!f"), "[->u", "u->v", "v->]", "[->w", "w->]", "u->]");
		AmbiguityAnalyzer analyzer = TestModels.analyze(model.esg, model.phiD);

		assertEquals(set(
				"dead-edge u->v unreachable",
				"dead-edge v->] unreachable",
				"dead-event v unreachable",
				"contained v"),
				describe(analyzer.getFindings(), FIVE_KINDS_AND_CONTAINED));
	}

	/** v (guard x & y) is reachable with x only and leavable with !x only: dead, cause no-completion. */
	@Test
	public void deadWithoutCompletion() throws Exception {
		TestModels.Small model = new TestModels.Small(Arrays.asList("x", "y"), clauses(),
				guards("a", "x", "v", "x & y", "b", "!x", "c", "!x"),
				"[->a", "a->v", "v->b", "b->]", "a->]", "[->c", "c->]");
		AmbiguityAnalyzer analyzer = TestModels.analyze(model.esg, model.phiD);

		assertEquals(set(
				"dead-event v no-completion",
				"dead-event b unreachable",
				"dead-edge a->v no-completion",
				"dead-edge v->b unreachable",
				"dead-edge b->] unreachable",
				"contained v",
				"contained b"),
				describe(analyzer.getFindings(), FIVE_KINDS_AND_CONTAINED));
	}

	/** mu(v) = f where every predecessor already requires f. */
	@Test
	public void falseOptional() throws Exception {
		TestModels.Small model = new TestModels.Small(Arrays.asList("f", "g"), clauses(new int[] { 1, 2 }),
				guards("a", "f", "b", "f & g", "v", "f", "w", "g"),
				"[->a", "[->b", "a->v", "b->v", "v->]", "b->w", "w->]", "a->]", "[->w");
		AmbiguityAnalyzer analyzer = TestModels.analyze(model.esg, model.phiD);

		assertEquals(set("false-optional v predecessors"), describe(analyzer.getFindings(), FALSE_OPTIONAL));
		assertEquals(Collections.emptySet(), describe(analyzer.getFindings(), FIVE_KINDS_AND_CONTAINED));
	}

	/** A guard the feature model forces is core. */
	@Test
	public void coreGuard() throws Exception {
		TestModels.Small model = new TestModels.Small(Arrays.asList("f"), clauses(new int[] { 1 }),
				guards("a", "f"), "[->a", "a->]");
		AmbiguityAnalyzer analyzer = TestModels.analyze(model.esg, model.phiD);
		assertEquals(set("core a"), describe(analyzer.getFindings(), CORE));
		assertEquals(set("false-optional a core"), describe(analyzer.getFindings(), FALSE_OPTIONAL));
	}

	/** A product without x reaches no exit: no complete event sequence, witness without x. */
	@Test
	public void noCompleteSequence() throws Exception {
		TestModels.Small model = new TestModels.Small(Arrays.asList("x"), clauses(),
				guards("a", "x"), "[->a", "a->]");
		AmbiguityAnalyzer analyzer = TestModels.analyze(model.esg, model.phiD);

		assertEquals(set("no-complete-sequence ["), describe(analyzer.getFindings(), NO_COMPLETE_SEQUENCE));
		for (AmbiguityAnalyzer.Finding finding : analyzer.getFindings()) {
			if (finding.kind.equals(NO_COMPLETE_SEQUENCE)) {
				assertEquals(Collections.emptyList(), finding.witness);
			}
		}
	}

	@Test(expected = UnsupportedOperationException.class)
	public void disjunctionIsRejected() throws Exception {
		TestModels.Small model = new TestModels.Small(Arrays.asList("f", "g"), clauses(),
				guards("a", "f"), "[->a", "a->]");
		tr.edu.iyte.esgfx.model.featureexpression.Disjunction disjunction =
				new tr.edu.iyte.esgfx.model.featureexpression.Disjunction();
		disjunction.addOperand(model.features.get("f"));
		disjunction.addOperand(model.features.get("g"));
		Vertex a = model.esg.getVertexByEventName("a/f");
		ESG changed = GuardRewriter.withGuards(model.esg,
				Collections.<Integer, FeatureExpression>singletonMap(a.getID(), disjunction));
		new StaticAnalysisEncoder(changed, model.phiD);
	}

	private static ESG withGuard(LoadedSplModel model, String eventName, FeatureExpression guard) {
		List<Vertex> matches = new java.util.ArrayList<Vertex>();
		for (Vertex vertex : model.getEsgFx().getVertexList()) {
			if (vertex.getEvent().getName().equals(eventName)) {
				matches.add(vertex);
			}
		}
		assertEquals("one vertex named " + eventName, 1, matches.size());
		Map<Integer, FeatureExpression> guards = Collections.singletonMap(matches.get(0).getID(), guard);
		return GuardRewriter.withGuards(model.getEsgFx(), guards);
	}
}
