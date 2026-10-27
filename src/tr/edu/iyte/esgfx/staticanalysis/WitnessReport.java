package tr.edu.iyte.esgfx.staticanalysis;

import java.util.ArrayList;
import java.util.List;

import tr.edu.iyte.esgfx.api.LoadedSplModel;

/**
 * Prints the witnesses of one product line under one source of Phi_d as named
 * feature sets: the question of the pseudo-start vertex first (a product
 * without a complete event sequence), then every other finding with a witness.
 * Each line gives the selected concrete features (the product) and the
 * abstract features its configuration selects.
 *
 * <p>Usage: {@code WitnessReport model engine|dimacs}.
 */
public final class WitnessReport {

	public static void main(String[] args) throws Exception {
		CaseStudy study = CaseStudy.byName(args[0]);
		PhiDSource source = PhiDSource.parse(args[1]);
		LoadedSplModel model = study.load();
		PhiD phiD = source.load(study, model);
		AmbiguityAnalyzer analyzer = new AmbiguityAnalyzer(new StaticAnalysisEncoder(model.getEsgFx(), phiD));
		analyzer.analyze();
		int verified = new WitnessVerifier(study.name, model.getFeatureExpressionMap(), null).verify(analyzer);

		List<AmbiguityAnalyzer.Finding> ordered = new ArrayList<AmbiguityAnalyzer.Finding>();
		for (AmbiguityAnalyzer.Finding finding : analyzer.getFindings()) {
			if (finding.kind.equals(AmbiguityAnalyzer.NO_COMPLETE_SEQUENCE)) {
				ordered.add(finding);
			}
		}
		if (ordered.isEmpty()) {
			System.out.println("[" + source.label() + "] " + study.name
					+ " no-complete-sequence: UNSAT, every product has a complete event sequence");
		}
		for (AmbiguityAnalyzer.Finding finding : analyzer.getFindings()) {
			if (finding.witness != null && !finding.kind.equals(AmbiguityAnalyzer.NO_COMPLETE_SEQUENCE)) {
				ordered.add(finding);
			}
		}
		for (AmbiguityAnalyzer.Finding finding : ordered) {
			List<String> abstractSelected = new ArrayList<String>(finding.configuration);
			abstractSelected.removeAll(finding.witness);
			System.out.println("[" + source.label() + "] " + finding.kind + " " + finding.element + " | product "
					+ finding.witness + " | abstract selected " + abstractSelected);
		}
		System.out.println("[" + source.label() + "] " + study.name + ": " + ordered.size() + " witnesses, "
				+ verified + " verified");
	}
}
