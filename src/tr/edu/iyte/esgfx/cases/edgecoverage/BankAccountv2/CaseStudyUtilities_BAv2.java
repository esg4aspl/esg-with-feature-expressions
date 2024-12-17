package tr.edu.iyte.esgfx.cases.edgecoverage.BankAccountv2;

import tr.edu.iyte.esgfx.cases.CaseStudyUtilities;

public class CaseStudyUtilities_BAv2 extends CaseStudyUtilities {

	public static void initializeFilePaths() {
		ESGFxFilePath = "files/Cases/BankAccountv2/BAv2_ESGFx.mxe";
		featureModelFilePath = "files/Cases/BankAccountv2/configs/model.xml";
		detailedFaultDetectionResults = "files/Cases/BankAccountv2/testsequences/edgecoverage/faultdetection/BAv2_detailedFaultDetectionResults";
		faultDetectionResultsForSPL = "files/Cases/BankAccountv2/testsequences/edgecoverage/faultdetection/BAv2_faultDetectionResultsForSPL.csv";
		testsequencesFolderPath = "files/Cases/BankAccountv2/testsequences/";

		timemeasurementFolderPath = "files/Cases/BankAccountv2/timemeasurement/";

		testsequencesFolderPath += "edgecoverage/";
		timemeasurementFolderPath += "edgecoverage/";

		testSuiteFilePath_edgeCoverage = testsequencesFolderPath + "BA_EdgeCoverage.txt";
		
		featureESGSetFolderPath_FeatureInsertion = "files/Cases/BankAccountv3/featureESGModels";
		featureESGSetFolderPath_FeatureOmission = "files/Cases/BankAccountv2/featureESGModels";
		
		productConfigurationFilePath = "files/Cases/BankAccountv2/productConfigurations_BAv2.txt";
	}

}
