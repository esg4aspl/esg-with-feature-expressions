package tr.edu.iyte.esgfx.cases.Elevator;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEdgeRedirector;

public class MutationTesting_EdgeRedirector_El extends CaseStudyUtilities_El {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_El.initializeFilePaths();
		
		MutantGeneratorEdgeRedirector mutantGeneratorEdgeRedirector = new MutantGeneratorEdgeRedirector();
		mutantGeneratorEdgeRedirector.generateMutants();
	}
}
