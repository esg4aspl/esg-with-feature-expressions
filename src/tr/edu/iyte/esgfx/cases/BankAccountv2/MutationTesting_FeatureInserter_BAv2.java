package tr.edu.iyte.esgfx.cases.BankAccountv2;

import java.util.Set;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esgfx.cases.MutantGeneratorFeaturenserter;
import tr.edu.iyte.esgfx.conversion.mxe.FeatureESGSetGenerator;

public class MutationTesting_FeatureInserter_BAv2 extends CaseStudyUtilities_BAv2 {

	public static void main(String[] args) throws Exception {

		coverageLength = 2;
		CaseStudyUtilities_BAv2.initializeFilePaths();

		featureESGSet = FeatureESGSetGenerator.createFeatureESGSet(featureESGSetFolderPath_FeatureInsertion);

		MutantGeneratorFeaturenserter mutantGeneratorFeaturenserter = new MutantGeneratorFeaturenserter();
		mutantGeneratorFeaturenserter.generateMutants();
	}
}
