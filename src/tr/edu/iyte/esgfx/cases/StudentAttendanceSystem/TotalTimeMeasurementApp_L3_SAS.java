package tr.edu.iyte.esgfx.cases.StudentAttendanceSystem;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement_L234;

public class TotalTimeMeasurementApp_L3_SAS extends CaseStudyUtilities_SAS {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_SAS.coverageLength = 3;
		CaseStudyUtilities_SAS.initializeFilePaths();
		
		/*
		 * Event triple coverage
		 */
		TotalTimeMeasurement_L234 totalTimeMeasurement = new TotalTimeMeasurement_L234();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
