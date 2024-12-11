package tr.edu.iyte.esgfx.cases.edgecoverage.HockertyShirts;

import tr.edu.iyte.esgfx.cases.CaseStudyUtilities;

public class CaseStudyUtilities_HS extends CaseStudyUtilities {
	
	public static void initializeFilePaths() {
		ESGFxFilePath = "files/Cases/HockertyShirts/HS_ESGFx.mxe";
		featureModelFilePath = "files/Cases/HockertyShirts/configs/model.xml";
		detailedFaultDetectionResults = "files/Cases/HockertyShirts/testsequences/edgecoverage/faultdetection/HockertyShirts_detailedFaultDetectionResults";
		faultDetectionResultsForSPL = "files/Cases/HockertyShirts/testsequences/edgecoverage/faultdetection/HockertyShirts_faultDetectionResultsForSPL.csv";
		
		testsequencesFolderPath = "files/Cases/HockertyShirts/testsequences/";

		timemeasurementFolderPath = "files/Cases/HockertyShirts/timemeasurement/";

		testsequencesFolderPath += "edgecoverage/";
		timemeasurementFolderPath += "edgecoverage/";

		testSuiteFilePath_edgeCoverage = testsequencesFolderPath + "HS_EdgeCoverage.txt";
		
		featureESGSetFolderPath_FeatureInsertion = "files/Cases/HockertyShirts/featureESGModels";
		featureESGSetFolderPath_FeatureOmission = "files/Cases/HockertyShirtsv2/featureESGModels";
		
		productConfigurationFilePath = "files/Cases/HockertyShirts/productConfigurations_HS.txt";
	}

}
