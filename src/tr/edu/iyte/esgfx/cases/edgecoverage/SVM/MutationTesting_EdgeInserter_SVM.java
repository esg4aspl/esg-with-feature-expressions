package tr.edu.iyte.esgfx.cases.edgecoverage.SVM;

import tr.edu.iyte.esgfx.cases.edgecoverage.MutantGeneratorEdgeInserter;

public class MutationTesting_EdgeInserter_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_SVM.initializeFilePaths();
		
		MutantGeneratorEdgeInserter edgeInserterMutantGenerator = new MutantGeneratorEdgeInserter();
		edgeInserterMutantGenerator.generateMutants();
	}
}
