package tr.edu.iyte.esgfx.staticanalysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static tr.edu.iyte.esgfx.staticanalysis.TestModels.clauses;
import static tr.edu.iyte.esgfx.staticanalysis.TestModels.guards;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.api.LoadedSplModel;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.Negation;

/**
 * The analysis and brute-force enumeration (enumerate, project, traverse
 * forward and backward) must report the same findings, kind by kind and
 * element by element, and every witness must pass the verifier.
 */
public class BruteForceAgreementTest {

	private static final String[] ALL_KINDS = { AmbiguityAnalyzer.DEAD_EVENT, AmbiguityAnalyzer.DEAD_EDGE,
			AmbiguityAnalyzer.CONTAINED, AmbiguityAnalyzer.FALSE_OPTIONAL, AmbiguityAnalyzer.CORE,
			AmbiguityAnalyzer.HIDDEN_UNREACHABLE, AmbiguityAnalyzer.HIDDEN_DEAD_END,
			AmbiguityAnalyzer.NO_COMPLETE_SEQUENCE };

	// Products per source: the DIMACS (feature model semantics) and the engine's clauses.

	@Test
	public void svm() throws Exception {
		agreeOnCaseStudy("SVM", PhiDSource.DIMACS, 12);
		agreeOnCaseStudy("SVM", PhiDSource.ENGINE, 12);
	}

	@Test
	public void eMail() throws Exception {
		agreeOnCaseStudy("eMail", PhiDSource.DIMACS, 24);
		agreeOnCaseStudy("eMail", PhiDSource.ENGINE, 23);
	}

	@Test
	public void elevator() throws Exception {
		agreeOnCaseStudy("Elevator", PhiDSource.DIMACS, 42);
		agreeOnCaseStudy("Elevator", PhiDSource.ENGINE, 42);
	}

	@Test
	public void bankAccount() throws Exception {
		agreeOnCaseStudy("BankAccount", PhiDSource.DIMACS, 504);
		agreeOnCaseStudy("BankAccount", PhiDSource.ENGINE, 498);
	}

	@Test
	public void studentAttendance() throws Exception {
		agreeOnCaseStudy("StudentAttendance", PhiDSource.DIMACS, 2664);
		agreeOnCaseStudy("StudentAttendance", PhiDSource.ENGINE, 2664);
	}

	@Test
	public void syngovia() throws Exception {
		agreeOnCaseStudy("syngovia", PhiDSource.DIMACS, 219);
		agreeOnCaseStudy("syngovia", PhiDSource.ENGINE, 219);
	}

	@Test
	public void tesla() throws Exception {
		agreeOnCaseStudy("Tesla", PhiDSource.DIMACS, 428544);
		agreeOnCaseStudy("Tesla", PhiDSource.ENGINE, 426672);
	}

	@Test
	public void svmReturnGuardNegated() throws Exception {
		LoadedSplModel svm = TestModels.svm();
		Vertex ret = null;
		for (Vertex vertex : svm.getEsgFx().getVertexList()) {
			if (vertex.getEvent().getName().equals("return")) {
				ret = vertex;
			}
		}
		assertNotNull(ret);
		ESG changed = GuardRewriter.withGuards(svm.getEsgFx(), Collections.<Integer, FeatureExpression>singletonMap(
				ret.getID(), new Negation(svm.getFeatureExpressionMap().get("c"))));
		for (PhiDSource source : PhiDSource.values()) {
			agree("SVM return=!c [" + source.label() + "]", changed, svm.getFeatureExpressionMap(),
					source.load(CaseStudy.byName("SVM"), svm), 12, null);
		}
	}

	@Test
	public void smallModels() throws Exception {
		TestModels.Small walk = new TestModels.Small(Arrays.asList("x"), clauses(),
				guards("a", "x", "v", "x"), "[->a", "a->v", "v->a", "a->]");
		agree("walk", walk.esg, walk.features, walk.phiD, 2, null);

		TestModels.Small join = new TestModels.Small(Arrays.asList("f"), clauses(),
				guards("u", "f", "v", "!f", "w", "!f"), "[->u", "u->v", "v->]", "[->w", "w->]", "u->]");
		agree("join", join.esg, join.features, join.phiD, 2, null);

		TestModels.Small completion = new TestModels.Small(Arrays.asList("x", "y"), clauses(),
				guards("a", "x", "v", "x & y", "b", "!x", "c", "!x"),
				"[->a", "a->v", "v->b", "b->]", "a->]", "[->c", "c->]");
		agree("completion", completion.esg, completion.features, completion.phiD, 4, null);

		TestModels.Small loops = new TestModels.Small(Arrays.asList("x", "y", "z"), clauses(new int[] { 1, 2 }),
				guards("a", "x", "b", "y & !z", "c", "z", "d", "!x"),
				"[->a", "[->b", "a->a", "a->b", "b->c", "c->b", "c->c", "b->]", "c->d", "d->a", "d->d", "a->]");
		agree("loops", loops.esg, loops.features, loops.phiD, 6, null);
	}

	private static void agreeOnCaseStudy(String name, PhiDSource source, int products) throws Exception {
		CaseStudy study = CaseStudy.byName(name);
		LoadedSplModel model = study.load();
		PhiD phiD = source.load(study, model);
		GuardFeatureCheck.check(study.name, model.getEsgFx(), phiD);
		// Witness membership: the Chapter 6 enumeration file under the engine's clauses
		// (restricted to concrete features), the enumerated products under the DIMACS.
		Set<Set<String>> enumeration = null;
		if (source == PhiDSource.ENGINE) {
			enumeration = WitnessVerifier.readEnumeration(study.enumerationFile(), study.chapter6, phiD);
			// The syngo.via and Tesla files hold the 400 Chapter 6 samples, not the enumeration.
			if (!study.name.equals("syngovia") && !study.name.equals("Tesla")) {
				assertNotNull("enumeration file of " + study.prefix, enumeration);
			}
			if (enumeration != null) {
				assertEquals(study.prefix + ": products in the enumeration file", products, enumeration.size());
			}
		}
		agree(study.prefix + " [" + source.label() + "]", model.getEsgFx(), model.getFeatureExpressionMap(), phiD,
				products, enumeration);
	}

	private static void agree(String name, ESG esg, Map<String, FeatureExpression> featureMap, PhiD phiD,
			int products, Set<Set<String>> enumeration) throws Exception {
		AmbiguityAnalyzer analyzer = TestModels.analyze(esg, phiD);
		BruteForce reference = new BruteForce(analyzer.getEncoder(), featureMap);

		assertEquals(name + ": products", products, reference.products);
		assertEquals(name + ": findings", reference.findings, TestModels.describe(analyzer.getFindings(), ALL_KINDS));

		Set<Set<String>> members = enumeration != null ? enumeration : reference.productSet;
		int witnesses = new WitnessVerifier(name, featureMap, members).verify(analyzer);
		for (AmbiguityAnalyzer.Finding finding : analyzer.getFindings()) {
			if (finding.witness != null) {
				assertTrue(name + ": " + finding, Boolean.TRUE.equals(finding.getVerified()));
			}
		}
		System.out.println(name + ": " + reference.products + " products, " + reference.findings.size()
				+ " findings agree, " + witnesses + " witnesses verified");
	}
}
