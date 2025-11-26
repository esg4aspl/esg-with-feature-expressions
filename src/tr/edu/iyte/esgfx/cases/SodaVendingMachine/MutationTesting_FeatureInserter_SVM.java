package tr.edu.iyte.esgfx.cases.SodaVendingMachine;

import tr.edu.iyte.esgfx.cases.MutantGeneratorFeaturenserter;

import tr.edu.iyte.esgfx.conversion.mxe.FeatureESGSetGenerator;

public class MutationTesting_FeatureInserter_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_SVM.initializeFilePaths();
		
		CaseStudyUtilities_SVM.featureESGSet = FeatureESGSetGenerator.createFeatureESGSet(CaseStudyUtilities_SVM.featureESGSetFolderPath_FeatureInsertion);

		MutantGeneratorFeaturenserter mutantGeneratorFeaturenserter = new MutantGeneratorFeaturenserter();
		mutantGeneratorFeaturenserter.generateMutants();

	}
}
