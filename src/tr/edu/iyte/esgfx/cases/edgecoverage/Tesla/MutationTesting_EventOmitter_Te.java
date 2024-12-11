package tr.edu.iyte.esgfx.cases.edgecoverage.Tesla;

import tr.edu.iyte.esgfx.cases.edgecoverage.MutantGeneratorEventOmitter;

public class MutationTesting_EventOmitter_Te extends CaseStudyUtilities_Tesla {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_Tesla.initializeFilePaths();
		
		MutantGeneratorEventOmitter mutantGeneratorEventOmitter = new MutantGeneratorEventOmitter();
		mutantGeneratorEventOmitter.generateMutants();
	}
}
