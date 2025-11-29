package tr.edu.iyte.esgfx.cases.StudentAttendanceSystem;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEdgeRedirector;

public class MutationTesting_EdgeRedirector_SAS extends CaseStudyUtilities_SAS {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_SAS.initializeFilePaths();
		
		MutantGeneratorEdgeRedirector mutantGeneratorEdgeRedirector = new MutantGeneratorEdgeRedirector();
		mutantGeneratorEdgeRedirector.generateMutants();
	}
}
