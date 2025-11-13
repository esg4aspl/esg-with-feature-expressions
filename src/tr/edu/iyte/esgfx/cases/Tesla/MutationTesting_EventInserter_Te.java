package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEventInserter;

public class MutationTesting_EventInserter_Te extends CaseStudyUtilities_Tesla {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_Tesla.initializeFilePaths();
		
		MutantGeneratorEventInserter mutantGeneratorEventInserter = new MutantGeneratorEventInserter();
		mutantGeneratorEventInserter.generateMutants();
	}
}
