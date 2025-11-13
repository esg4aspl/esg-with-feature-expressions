package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEdgeOmitter;

public class MutationTesting_EdgeOmitter_Te extends CaseStudyUtilities_Tesla {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_Tesla.initializeFilePaths();
		
		MutantGeneratorEdgeOmitter edgeOmitterMutantGenerator = new MutantGeneratorEdgeOmitter();
		edgeOmitterMutantGenerator.generateMutants();

	}
}
