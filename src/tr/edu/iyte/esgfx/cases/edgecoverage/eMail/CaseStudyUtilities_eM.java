package tr.edu.iyte.esgfx.cases.edgecoverage.eMail;

import tr.edu.iyte.esgfx.cases.CaseStudyUtilities;

public class CaseStudyUtilities_eM extends CaseStudyUtilities {
	
	public static void initializeFilePaths() {
		ESGFxFilePath = "files/Cases/eMail/eM_ESGFx.mxe";
		featureModelFilePath = "files/Cases/eMail/configs/model.xml";
		detailedFaultDetectionResults = "files/Cases/eMail/testsequences/edgecoverage/faultdetection/eM_detailedFaultDetectionResults";
		faultDetectionResultsForSPL = "files/Cases/eMail/testsequences/edgecoverage/faultdetection/eM_faultDetectionResultsForSPL.csv";
		
		testsequencesFolderPath = "files/Cases/eMail/testsequences/";

		timemeasurementFolderPath = "files/Cases/eMail/timemeasurement/";

		testsequencesFolderPath += "edgecoverage/";
		timemeasurementFolderPath += "edgecoverage/";

		testSuiteFilePath_edgeCoverage = testsequencesFolderPath + "eM_EdgeCoverage.txt";
		
		featureESGSetFolderPath_FeatureInsertion = "files/Cases/eMailv2/featureESGModels";
		featureESGSetFolderPath_FeatureOmission = "files/Cases/eMail/featureESGModels";
	}
}
