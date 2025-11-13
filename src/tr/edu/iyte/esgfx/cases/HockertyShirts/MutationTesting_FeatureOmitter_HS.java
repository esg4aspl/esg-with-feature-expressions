package tr.edu.iyte.esgfx.cases.HockertyShirts;

import tr.edu.iyte.esgfx.cases.MutantGeneratorFeatureOmitter;
import tr.edu.iyte.esgfx.conversion.mxe.FeatureESGSetGenerator;

public class MutationTesting_FeatureOmitter_HS extends CaseStudyUtilities_HS {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_HS.initializeFilePaths();
		
		featureESGSet = FeatureESGSetGenerator.createFeatureESGSet(featureESGSetFolderPath_FeatureOmission);
		
		MutantGeneratorFeatureOmitter mutantGeneratorFeatureOmitter = new MutantGeneratorFeatureOmitter();
		mutantGeneratorFeatureOmitter.generateMutants();

	}

}
