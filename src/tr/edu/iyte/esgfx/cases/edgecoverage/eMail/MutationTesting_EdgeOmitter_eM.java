package tr.edu.iyte.esgfx.cases.edgecoverage.eMail;

import tr.edu.iyte.esgfx.cases.edgecoverage.MutantGeneratorEdgeOmitter;

public class MutationTesting_EdgeOmitter_eM extends CaseStudyUtilities_eM {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_eM.initializeFilePaths();
		
		MutantGeneratorEdgeOmitter edgeOmitterMutantGenerator = new MutantGeneratorEdgeOmitter();
		edgeOmitterMutantGenerator.generateMutants();

	}
}
