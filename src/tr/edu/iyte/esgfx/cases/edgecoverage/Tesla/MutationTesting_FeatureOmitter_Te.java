package tr.edu.iyte.esgfx.cases.edgecoverage.Tesla;

import java.util.Set;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esgfx.cases.edgecoverage.MutantGeneratorFeatureOmitter;
import tr.edu.iyte.esgfx.conversion.mxe.FeatureESGSetGenerator;

public class MutationTesting_FeatureOmitter_Te extends CaseStudyUtilities_Tesla {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_Tesla.initializeFilePaths();
		
		Set<ESG> featureESGSet = FeatureESGSetGenerator.createFeatureESGSet(featureESGSetFolderPath_FeatureOmission);
		
		MutantGeneratorFeatureOmitter mutantGeneratorFeatureOmitter = new MutantGeneratorFeatureOmitter();
		mutantGeneratorFeatureOmitter.generateMutants(featureESGSet);

	}

}
