package tr.edu.iyte.esgfx.cases.StudentAttendanceSystem;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEventOmitter;

public class MutationTesting_EventOmitter_SAS extends CaseStudyUtilities_SAS {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_SAS.initializeFilePaths();
		
		MutantGeneratorEventOmitter mutantGeneratorEventOmitter = new MutantGeneratorEventOmitter();
		mutantGeneratorEventOmitter.generateMutants();

	}
}
