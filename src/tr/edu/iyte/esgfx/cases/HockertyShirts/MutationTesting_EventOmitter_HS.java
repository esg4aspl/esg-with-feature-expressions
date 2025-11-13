package tr.edu.iyte.esgfx.cases.HockertyShirts;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEventOmitter;

public class MutationTesting_EventOmitter_HS extends CaseStudyUtilities_HS {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_HS.initializeFilePaths();
		
		MutantGeneratorEventOmitter mutantGeneratorEventOmitter = new MutantGeneratorEventOmitter();
		mutantGeneratorEventOmitter.generateMutants();
	}
}
