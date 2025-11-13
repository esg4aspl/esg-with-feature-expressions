package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEventOmitter;

public class MutationTesting_EventOmitter_Svia extends CaseStudyUtilities_Svia {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_Svia.initializeFilePaths();
		
		MutantGeneratorEventOmitter mutantGeneratorEventOmitter = new MutantGeneratorEventOmitter();
		mutantGeneratorEventOmitter.generateMutants();
	}
}
