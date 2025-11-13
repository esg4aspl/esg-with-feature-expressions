package tr.edu.iyte.esgfx.cases.StudentAttendanceSystem;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEdgeOmitter;

public class MutationTesting_EdgeOmitter_SAS extends CaseStudyUtilities_SAS {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_SAS.initializeFilePaths();
		
		MutantGeneratorEdgeOmitter edgeOmitterMutantGenerator = new MutantGeneratorEdgeOmitter();
		edgeOmitterMutantGenerator.generateMutants();

	}
}
