package tr.edu.iyte.esgfx.cases.StudentAttendanceSystem;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEventInserter;

public class MutationTesting_EventInserter_SAS extends CaseStudyUtilities_SAS {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_SAS.initializeFilePaths();
		
		MutantGeneratorEventInserter mutantGeneratorEventInserter = new MutantGeneratorEventInserter();
		mutantGeneratorEventInserter.generateMutants();
	}
}
