package tr.edu.iyte.esgfx.cases.SVM;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEventInserter;

public class MutationTesting_EventInserter_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {
		
		coverageLength = 2;
		
		CaseStudyUtilities_SVM.initializeFilePaths();
		
		MutantGeneratorEventInserter mutantGeneratorEventInserter = new MutantGeneratorEventInserter();
		mutantGeneratorEventInserter.generateMutants();
	}
}
