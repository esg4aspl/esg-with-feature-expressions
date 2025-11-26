package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.MutantGeneratorFeaturenserter;
import tr.edu.iyte.esgfx.conversion.mxe.FeatureESGSetGenerator;

public class MutationTesting_FeatureInserter_Svia extends CaseStudyUtilities_Svia {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_Svia.initializeFilePaths();
		
		CaseStudyUtilities_Svia.featureESGSet = FeatureESGSetGenerator.createFeatureESGSet(CaseStudyUtilities_Svia.featureESGSetFolderPath_FeatureInsertion);

		MutantGeneratorFeaturenserter mutantGeneratorFeaturenserter = new MutantGeneratorFeaturenserter();
		mutantGeneratorFeaturenserter.generateMutants();

	}
}
