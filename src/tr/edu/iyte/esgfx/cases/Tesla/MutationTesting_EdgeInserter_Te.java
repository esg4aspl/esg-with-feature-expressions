package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEdgeInserter;

public class MutationTesting_EdgeInserter_Te extends CaseStudyUtilities_Tesla {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_Tesla.initializeFilePaths();
		
		MutantGeneratorEdgeInserter edgeInserterMutantGenerator = new MutantGeneratorEdgeInserter();
		edgeInserterMutantGenerator.generateMutants();
	}
}
