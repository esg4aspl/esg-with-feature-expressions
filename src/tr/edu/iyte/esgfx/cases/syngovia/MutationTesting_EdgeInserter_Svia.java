package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEdgeInserter;

public class MutationTesting_EdgeInserter_Svia extends CaseStudyUtilities_Svia {

	public static void main(String[] args) throws Exception {
		

		CaseStudyUtilities_Svia.initializeFilePaths();
		
		MutantGeneratorEdgeInserter edgeInserterMutantGenerator = new MutantGeneratorEdgeInserter();
		edgeInserterMutantGenerator.generateMutants();
	}
}
