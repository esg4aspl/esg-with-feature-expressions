package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEventInserter;

public class MutationTesting_EventInserter_Svia extends CaseStudyUtilities_Svia {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_Svia.initializeFilePaths();

		MutantGeneratorEventInserter mutantGeneratorEventInserter = new MutantGeneratorEventInserter();
		mutantGeneratorEventInserter.generateMutants();
	}
}
