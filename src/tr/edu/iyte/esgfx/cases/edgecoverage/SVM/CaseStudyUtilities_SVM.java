package tr.edu.iyte.esgfx.cases.edgecoverage.SVM;

import tr.edu.iyte.esgfx.cases.CaseStudyUtilities;

public class CaseStudyUtilities_SVM extends CaseStudyUtilities {
	
	public static void initializeFilePaths() {
		ESGFxFilePath = "files/Cases/SodaVendingMachine/SVM_ESGFx.mxe";
		featureModelFilePath = "files/Cases/SodaVendingMachine/configs/model.xml";
		detailedFaultDetectionResults = "files/Cases/SodaVendingMachine/testsequences/edgecoverage/faultdetection/SVM_detailedFaultDetectionResults";
		faultDetectionResultsForSPL = "files/Cases/SodaVendingMachine/testsequences/edgecoverage/faultdetection/SVM_faultDetectionResultsForSPL.csv";
		
		testsequencesFolderPath = "files/Cases/SodaVendingMachine/testsequences/";

		timemeasurementFolderPath = "files/Cases/SodaVendingMachine/timemeasurement/";

		testsequencesFolderPath += "edgecoverage/";
		timemeasurementFolderPath += "edgecoverage/";

		testSuiteFilePath_edgeCoverage = testsequencesFolderPath + "SVM_EdgeCoverage.txt";
		
		featureESGSetFolderPath_FeatureInsertion = "files/Cases/SodaVendingMachinev2/featureESGModels";
		featureESGSetFolderPath_FeatureOmission = "files/Cases/SodaVendingMachine/featureESGModels";
		
		productConfigurationFilePath = "files/Cases/SodaVendingMachine/productConfigurations_SVM.txt";
	}

}
