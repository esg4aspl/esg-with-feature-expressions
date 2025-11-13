package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.MutantGeneratorFeaturenserter;
import tr.edu.iyte.esgfx.conversion.mxe.FeatureESGSetGenerator;

public class MutationTesting_FeatureInserter_Te extends CaseStudyUtilities_Tesla {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_Tesla.initializeFilePaths();
		
		featureESGSet = FeatureESGSetGenerator.createFeatureESGSet(featureESGSetFolderPath_FeatureInsertion);

		MutantGeneratorFeaturenserter mutantGeneratorFeaturenserter = new MutantGeneratorFeaturenserter();
		mutantGeneratorFeaturenserter.generateMutants();

	}
}
