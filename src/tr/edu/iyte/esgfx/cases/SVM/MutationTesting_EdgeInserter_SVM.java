package tr.edu.iyte.esgfx.cases.SVM;

import tr.edu.iyte.esgfx.cases.MutantGeneratorEdgeInserter;

public class MutationTesting_EdgeInserter_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_SVM.initializeFilePaths();
		
		MutantGeneratorEdgeInserter edgeInserterMutantGenerator = new MutantGeneratorEdgeInserter();
		edgeInserterMutantGenerator.generateMutants();
	}
}
