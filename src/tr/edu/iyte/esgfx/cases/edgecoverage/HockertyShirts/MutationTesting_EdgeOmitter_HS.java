package tr.edu.iyte.esgfx.cases.edgecoverage.HockertyShirts;

import tr.edu.iyte.esgfx.cases.edgecoverage.MutantGeneratorEdgeOmitter;

public class MutationTesting_EdgeOmitter_HS extends CaseStudyUtilities_HS {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_HS.initializeFilePaths();
		
		MutantGeneratorEdgeOmitter edgeOmitterMutantGenerator = new MutantGeneratorEdgeOmitter();
		edgeOmitterMutantGenerator.generateMutants();

	}
}
