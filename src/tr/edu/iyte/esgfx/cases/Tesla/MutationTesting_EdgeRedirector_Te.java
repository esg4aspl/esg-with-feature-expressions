package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEdgeRedirector;

public class MutationTesting_EdgeRedirector_Te extends CaseStudyUtilities_Te {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_Te.initializeFilePaths();
		
		MutantGeneratorEdgeRedirector mutantGeneratorEdgeRedirector = new MutantGeneratorEdgeRedirector();
		mutantGeneratorEdgeRedirector.generateMutants();
	}
}
