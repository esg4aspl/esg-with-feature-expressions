package tr.edu.iyte.esgfx.cases.edgecoverage.HockertyShirts;

import java.util.Set;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esgfx.cases.edgecoverage.MutantGeneratorFeaturenserter;
import tr.edu.iyte.esgfx.conversion.mxe.FeatureESGSetGenerator;

public class MutationTesting_FeatureInserter_HS extends CaseStudyUtilities_HS {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_HS.initializeFilePaths();
		
		Set<ESG> featureESGSet = FeatureESGSetGenerator.createFeatureESGSet(featureESGSetFolderPath_FeatureInsertion);

		MutantGeneratorFeaturenserter mutantGeneratorFeaturenserter = new MutantGeneratorFeaturenserter();
		mutantGeneratorFeaturenserter.generateMutants(featureESGSet);

	}
}
