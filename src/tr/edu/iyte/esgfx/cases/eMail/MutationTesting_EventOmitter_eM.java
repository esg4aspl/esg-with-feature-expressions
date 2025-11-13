package tr.edu.iyte.esgfx.cases.eMail;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEventOmitter;

public class MutationTesting_EventOmitter_eM extends CaseStudyUtilities_eM {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_eM.initializeFilePaths();
		
		MutantGeneratorEventOmitter mutantGeneratorEventOmitter = new MutantGeneratorEventOmitter();
		mutantGeneratorEventOmitter.generateMutants();
	}
}
