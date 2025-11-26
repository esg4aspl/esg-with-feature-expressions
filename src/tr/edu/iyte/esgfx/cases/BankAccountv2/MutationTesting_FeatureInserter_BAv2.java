package tr.edu.iyte.esgfx.cases.BankAccountv2;

import tr.edu.iyte.esgfx.cases.MutantGeneratorFeaturenserter;
import tr.edu.iyte.esgfx.conversion.mxe.FeatureESGSetGenerator;

public class MutationTesting_FeatureInserter_BAv2 extends CaseStudyUtilities_BAv2 {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_BAv2.coverageLength = 2;
		CaseStudyUtilities_BAv2.initializeFilePaths();

		CaseStudyUtilities_BAv2.featureESGSet = FeatureESGSetGenerator.createFeatureESGSet(CaseStudyUtilities_BAv2.featureESGSetFolderPath_FeatureInsertion);

		MutantGeneratorFeaturenserter mutantGeneratorFeaturenserter = new MutantGeneratorFeaturenserter();
		mutantGeneratorFeaturenserter.generateMutants();
	}
}
