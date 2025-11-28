package tr.edu.iyte.esgfx.cases.Elevator;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEdgeOmitter;

public class MutationTesting_EdgeOmitter_El extends CaseStudyUtilities_El {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_El.initializeFilePaths();
		
		MutantGeneratorEdgeOmitter edgeOmitterMutantGenerator = new MutantGeneratorEdgeOmitter();
		edgeOmitterMutantGenerator.generateMutants();

	}
}
