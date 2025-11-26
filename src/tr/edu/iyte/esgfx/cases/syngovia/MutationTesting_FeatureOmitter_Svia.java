package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.MutantGeneratorFeatureOmitter;
import tr.edu.iyte.esgfx.conversion.mxe.FeatureESGSetGenerator;

public class MutationTesting_FeatureOmitter_Svia extends CaseStudyUtilities_Svia {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_Svia.initializeFilePaths();

		CaseStudyUtilities_Svia.featureESGSet = FeatureESGSetGenerator
				.createFeatureESGSet(CaseStudyUtilities_Svia.featureESGSetFolderPath_FeatureOmission);

		MutantGeneratorFeatureOmitter mutantGeneratorFeatureOmitter = new MutantGeneratorFeatureOmitter();
		mutantGeneratorFeatureOmitter.generateMutants();

	}

}
