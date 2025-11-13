package tr.edu.iyte.esgfx.cases.BankAccountv2;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEventOmitter;

public class MutationTesting_EventOmitter_BAv2 extends CaseStudyUtilities_BAv2 {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_BAv2.initializeFilePaths();
		
		MutantGeneratorEventOmitter mutantGeneratorEventOmitter = new MutantGeneratorEventOmitter();
		mutantGeneratorEventOmitter.generateMutants();

	}
}
