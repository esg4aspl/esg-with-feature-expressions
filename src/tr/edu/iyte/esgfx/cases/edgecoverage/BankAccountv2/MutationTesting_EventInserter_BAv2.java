package tr.edu.iyte.esgfx.cases.edgecoverage.BankAccountv2;

import tr.edu.iyte.esgfx.cases.edgecoverage.MutantGeneratorEventInserter;

public class MutationTesting_EventInserter_BAv2 extends CaseStudyUtilities_BAv2 {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_BAv2.initializeFilePaths();
		
		MutantGeneratorEventInserter mutantGeneratorEventInserter = new MutantGeneratorEventInserter();
		mutantGeneratorEventInserter.generateMutants("update credentials", "b");
	}
}
