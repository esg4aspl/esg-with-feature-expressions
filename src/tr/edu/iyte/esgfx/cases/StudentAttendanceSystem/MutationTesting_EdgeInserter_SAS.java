package tr.edu.iyte.esgfx.cases.StudentAttendanceSystem;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEdgeInserter;

public class MutationTesting_EdgeInserter_SAS extends CaseStudyUtilities_SAS {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_SAS.initializeFilePaths();
		
		MutantGeneratorEdgeInserter edgeInserterMutantGenerator = new MutantGeneratorEdgeInserter();
		edgeInserterMutantGenerator.generateMutants();
	}
}
