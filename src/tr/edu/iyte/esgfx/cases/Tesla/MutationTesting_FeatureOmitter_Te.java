package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.MutantGeneratorFeatureOmitter;
import tr.edu.iyte.esgfx.conversion.mxe.FeatureESGSetGenerator;

public class MutationTesting_FeatureOmitter_Te extends CaseStudyUtilities_Tesla {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_Tesla.initializeFilePaths();
		
		featureESGSet = FeatureESGSetGenerator.createFeatureESGSet(featureESGSetFolderPath_FeatureOmission);
		
		MutantGeneratorFeatureOmitter mutantGeneratorFeatureOmitter = new MutantGeneratorFeatureOmitter();
		mutantGeneratorFeatureOmitter.generateMutants();

	}

}
