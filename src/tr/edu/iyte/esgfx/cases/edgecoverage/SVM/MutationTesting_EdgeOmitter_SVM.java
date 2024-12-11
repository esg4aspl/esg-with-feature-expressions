package tr.edu.iyte.esgfx.cases.edgecoverage.SVM;

import tr.edu.iyte.esgfx.cases.edgecoverage.MutantGeneratorEdgeOmitter;

public class MutationTesting_EdgeOmitter_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_SVM.initializeFilePaths();
		
		MutantGeneratorEdgeOmitter edgeOmitterMutantGenerator = new MutantGeneratorEdgeOmitter();
		edgeOmitterMutantGenerator.generateMutants();

	}
}
