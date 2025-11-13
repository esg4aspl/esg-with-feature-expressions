package tr.edu.iyte.esgfx.cases.HockertyShirts;


import tr.edu.iyte.esgfx.cases.MutantGeneratorFeaturenserter;
import tr.edu.iyte.esgfx.conversion.mxe.FeatureESGSetGenerator;

public class MutationTesting_FeatureInserter_HS extends CaseStudyUtilities_HS {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_HS.initializeFilePaths();
		
		featureESGSet = FeatureESGSetGenerator.createFeatureESGSet(featureESGSetFolderPath_FeatureInsertion);

		MutantGeneratorFeaturenserter mutantGeneratorFeaturenserter = new MutantGeneratorFeaturenserter();
		mutantGeneratorFeaturenserter.generateMutants();

	}
}
