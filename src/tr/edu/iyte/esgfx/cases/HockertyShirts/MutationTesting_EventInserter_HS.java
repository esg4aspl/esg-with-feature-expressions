package tr.edu.iyte.esgfx.cases.HockertyShirts;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEventInserter;

public class MutationTesting_EventInserter_HS extends CaseStudyUtilities_HS {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_HS.initializeFilePaths();
		
		MutantGeneratorEventInserter mutantGeneratorEventInserter = new MutantGeneratorEventInserter();
		mutantGeneratorEventInserter.generateMutants();
	}
}
