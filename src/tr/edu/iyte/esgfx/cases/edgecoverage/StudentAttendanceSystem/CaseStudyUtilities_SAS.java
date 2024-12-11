package tr.edu.iyte.esgfx.cases.edgecoverage.StudentAttendanceSystem;

import tr.edu.iyte.esgfx.cases.CaseStudyUtilities;

public class CaseStudyUtilities_SAS extends CaseStudyUtilities {

	public static void initializeFilePaths() {
		ESGFxFilePath = "files/Cases/StudentAttendanceSystem/SAS_ESGFx.mxe";
		featureModelFilePath = "files/Cases/StudentAttendanceSystem/configs/model.xml";

		detailedFaultDetectionResults = testsequencesFolderPath
				+ "/edgecoverage/faultdetection/StudentAttendanceSystem_detailedFaultDetectionResults";
		faultDetectionResultsForSPL = testsequencesFolderPath
				+ "/edgecoverage/faultdetection/StudentAttendanceSystem_faultDetectionResultsForSPL.csv";

		testsequencesFolderPath = "files/Cases/StudentAttendanceSystem/testsequences/";

		timemeasurementFolderPath = "files/Cases/StudentAttendanceSystem/timemeasurement/";

		testsequencesFolderPath += "edgecoverage/";
		timemeasurementFolderPath += "edgecoverage/";

		testSuiteFilePath_edgeCoverage = testsequencesFolderPath + "SAS_EdgeCoverage.txt";

		featureESGSetFolderPath_FeatureInsertion = "files/Cases/StudentAttendanceSystemv2/featureESGModels";
		featureESGSetFolderPath_FeatureOmission = "files/Cases/StudentAttendanceSystem/featureESGModels";
	}
}
