package tr.edu.iyte.esgfx.cases.edgecoverage.Tesla;

import java.util.Set;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esgfx.cases.edgecoverage.MutantGeneratorFeaturenserter;
import tr.edu.iyte.esgfx.conversion.mxe.FeatureESGSetGenerator;

public class MutationTesting_FeatureInserter_Te extends CaseStudyUtilities_Tesla {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_Tesla.initializeFilePaths();
		
		Set<ESG> featureESGSet = FeatureESGSetGenerator.createFeatureESGSet(featureESGSetFolderPath_FeatureInsertion);

		MutantGeneratorFeaturenserter mutantGeneratorFeaturenserter = new MutantGeneratorFeaturenserter();
		mutantGeneratorFeaturenserter.generateMutants(featureESGSet);

	}
}
