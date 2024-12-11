package tr.edu.iyte.esgfx.cases.edgecoverage.StudentAttendanceSystem;

import tr.edu.iyte.esgfx.cases.edgecoverage.TotalTimeMeasurement;

public class TotalTimeMeasurementApp_SAS extends CaseStudyUtilities_SAS {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_SAS.initializeFilePaths();
		
		TotalTimeMeasurement totalTimeMeasurement = new TotalTimeMeasurement();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
