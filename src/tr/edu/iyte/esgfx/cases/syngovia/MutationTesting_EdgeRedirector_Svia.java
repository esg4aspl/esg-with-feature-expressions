package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEdgeRedirector;

public class MutationTesting_EdgeRedirector_Svia extends CaseStudyUtilities_Svia {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_Svia.initializeFilePaths();
		
		MutantGeneratorEdgeRedirector mutantGeneratorEdgeRedirector = new MutantGeneratorEdgeRedirector();
		mutantGeneratorEdgeRedirector.generateMutants();
	}
}
