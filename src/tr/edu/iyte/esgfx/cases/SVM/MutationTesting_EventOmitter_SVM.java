package tr.edu.iyte.esgfx.cases.SVM;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEventOmitter;

public class MutationTesting_EventOmitter_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_SVM.initializeFilePaths();		
		MutantGeneratorEventOmitter mutantGeneratorEventOmitter = new MutantGeneratorEventOmitter();
		mutantGeneratorEventOmitter.generateMutants();
	}
}
