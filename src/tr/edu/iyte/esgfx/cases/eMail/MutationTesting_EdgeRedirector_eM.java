package tr.edu.iyte.esgfx.cases.eMail;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEdgeRedirector;

public class MutationTesting_EdgeRedirector_eM extends CaseStudyUtilities_eM {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_eM.initializeFilePaths();
		
		MutantGeneratorEdgeRedirector mutantGeneratorEdgeRedirector = new MutantGeneratorEdgeRedirector();
		mutantGeneratorEdgeRedirector.generateMutants();
	}
}
