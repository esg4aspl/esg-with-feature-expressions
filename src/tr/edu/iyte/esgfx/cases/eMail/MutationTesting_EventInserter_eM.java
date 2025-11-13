package tr.edu.iyte.esgfx.cases.eMail;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEventInserter;

public class MutationTesting_EventInserter_eM extends CaseStudyUtilities_eM {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_eM.initializeFilePaths();

		MutantGeneratorEventInserter mutantGeneratorEventInserter = new MutantGeneratorEventInserter();
		mutantGeneratorEventInserter.generateMutants();
	}
}
