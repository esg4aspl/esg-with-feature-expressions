package tr.edu.iyte.esgfx.cases.BankAccountv2;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEdgeOmitter;

public class MutationTesting_EdgeOmitter_BAv2 extends CaseStudyUtilities_BAv2 {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_BAv2.initializeFilePaths();
		
		MutantGeneratorEdgeOmitter edgeOmitterMutantGenerator = new MutantGeneratorEdgeOmitter();
		edgeOmitterMutantGenerator.generateMutants();
	}
}
