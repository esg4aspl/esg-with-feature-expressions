package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEdgeOmitter;

public class MutationTesting_EdgeOmitter_Svia extends CaseStudyUtilities_Svia {

	public static void main(String[] args) throws Exception {
		
		
		CaseStudyUtilities_Svia.initializeFilePaths();
		
		MutantGeneratorEdgeOmitter edgeOmitterMutantGenerator = new MutantGeneratorEdgeOmitter();
		edgeOmitterMutantGenerator.generateMutants();

	}
}
