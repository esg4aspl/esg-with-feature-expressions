package tr.edu.iyte.esgfx.cases.BankAccountv2;

import tr.edu.iyte.esgfx.cases.MutantGeneratorFeatureOmitter;
import tr.edu.iyte.esgfx.conversion.mxe.FeatureESGSetGenerator;

public class MutationTesting_FeatureOmitter_BAv2 extends CaseStudyUtilities_BAv2 {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_BAv2.initializeFilePaths();
		featureESGSet = FeatureESGSetGenerator.createFeatureESGSet(featureESGSetFolderPath_FeatureOmission);
		
		MutantGeneratorFeatureOmitter mutantGeneratorFeatureOmitter = new MutantGeneratorFeatureOmitter();
		mutantGeneratorFeatureOmitter.generateMutants();
	}
}