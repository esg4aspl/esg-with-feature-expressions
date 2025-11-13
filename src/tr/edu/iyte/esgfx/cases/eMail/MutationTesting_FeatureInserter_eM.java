package tr.edu.iyte.esgfx.cases.eMail;

import tr.edu.iyte.esgfx.cases.MutantGeneratorFeaturenserter;
import tr.edu.iyte.esgfx.conversion.mxe.FeatureESGSetGenerator;

public class MutationTesting_FeatureInserter_eM extends CaseStudyUtilities_eM {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_eM.initializeFilePaths();

		featureESGSet = FeatureESGSetGenerator.createFeatureESGSet(featureESGSetFolderPath_FeatureInsertion);

		MutantGeneratorFeaturenserter mutantGeneratorFeaturenserter = new MutantGeneratorFeaturenserter();
		mutantGeneratorFeaturenserter.generateMutants();
	}
}
