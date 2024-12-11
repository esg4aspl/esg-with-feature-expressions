package tr.edu.iyte.esgfx.cases.edgecoverage.SVM;

import tr.edu.iyte.esgfx.cases.edgecoverage.MutantGeneratorEventInserter;

public class MutationTesting_EventInserter_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_SVM.initializeFilePaths();
		
		MutantGeneratorEventInserter mutantGeneratorEventInserter = new MutantGeneratorEventInserter();
		mutantGeneratorEventInserter.generateMutants("", "");
	}
}
