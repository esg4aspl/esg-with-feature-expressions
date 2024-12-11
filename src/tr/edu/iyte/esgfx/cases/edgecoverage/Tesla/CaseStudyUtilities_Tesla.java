package tr.edu.iyte.esgfx.cases.edgecoverage.Tesla;

import tr.edu.iyte.esgfx.cases.CaseStudyUtilities;

public class CaseStudyUtilities_Tesla extends CaseStudyUtilities {
	
	public static void initializeFilePaths() {
		ESGFxFilePath = "files/Cases/Tesla/Tesla_ESGFx.mxe";
		featureModelFilePath = "files/Cases/Tesla/configs/model.xml";
		
		detailedFaultDetectionResults = "files/Cases/Tesla/testsequences/edgecoverage/faultdetection/Tesla_detailedFaultDetectionResults";
		faultDetectionResultsForSPL = "files/Cases/Tesla/testsequences/edgecoverage/faultdetection/Tesla_faultDetectionResultsForSPL.csv";
		
		testsequencesFolderPath = "files/Cases/Tesla/testsequences/";
		timemeasurementFolderPath = "files/Cases/Tesla/timemeasurement/";

		testsequencesFolderPath += "edgecoverage/";
		timemeasurementFolderPath += "edgecoverage/";

		testSuiteFilePath_edgeCoverage = testsequencesFolderPath + "Te_EdgeCoverage.txt";
		
		featureESGSetFolderPath_FeatureInsertion = "files/Cases/Teslav2/featureESGModels";
		featureESGSetFolderPath_FeatureOmission = "files/Cases/Tesla/featureESGModels";
	}

}
