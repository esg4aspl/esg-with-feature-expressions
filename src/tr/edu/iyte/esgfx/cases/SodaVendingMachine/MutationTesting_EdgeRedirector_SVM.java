package tr.edu.iyte.esgfx.cases.SodaVendingMachine;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEdgeRedirector;

public class MutationTesting_EdgeRedirector_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {

		
		CaseStudyUtilities_SVM.initializeFilePaths();
		
		MutantGeneratorEdgeRedirector edgeRedirectorMutantGenerator = new MutantGeneratorEdgeRedirector();
		edgeRedirectorMutantGenerator.generateMutants();

	}
}
