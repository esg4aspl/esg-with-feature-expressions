package tr.edu.iyte.esgfx.cases.StudentAttendanceSystem;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement;

public class TotalTimeMeasurementApp_SAS_L4 extends CaseStudyUtilities_SAS {

	public static void main(String[] args) throws Exception {
		
		coverageLength = 4;
		
		CaseStudyUtilities_SAS.initializeFilePaths();
		TotalTimeMeasurement totalTimeMeasurement = new TotalTimeMeasurement();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
