package tr.edu.iyte.esgfx.cases.StudentAttendanceSystem;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement_L234;


public class TotalTimeMeasurementApp_L4_SAS extends CaseStudyUtilities_SAS {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_SAS.coverageLength = 4;
		CaseStudyUtilities_SAS.initializeFilePaths();
		
		/*
		 * Event quadruple coverage
		 */
		TotalTimeMeasurement_L234 totalTimeMeasurement = new TotalTimeMeasurement_L234();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
