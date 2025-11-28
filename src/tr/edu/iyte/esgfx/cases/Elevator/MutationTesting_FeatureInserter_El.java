package tr.edu.iyte.esgfx.cases.Elevator;

import tr.edu.iyte.esgfx.cases.MutantGeneratorFeaturenserter;
import tr.edu.iyte.esgfx.conversion.mxe.FeatureESGSetGenerator;

public class MutationTesting_FeatureInserter_El extends CaseStudyUtilities_El {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_El.initializeFilePaths();

		CaseStudyUtilities_El.featureESGSet = FeatureESGSetGenerator.createFeatureESGSet(CaseStudyUtilities_El.featureESGSetFolderPath_FeatureInsertion);

		MutantGeneratorFeaturenserter mutantGeneratorFeaturenserter = new MutantGeneratorFeaturenserter();
		mutantGeneratorFeaturenserter.generateMutants();
	}
}
