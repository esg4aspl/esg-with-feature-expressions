package tr.edu.iyte.esgfx.cases.eMail;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement;

public class TotalTimeMeasurementApp_eM_L4 extends CaseStudyUtilities_eM {

	public static void main(String[] args) throws Exception {
		
		coverageLength = 4;
		
		CaseStudyUtilities_eM.initializeFilePaths();

		TotalTimeMeasurement totalTimeMeasurement = new TotalTimeMeasurement();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();

	}
}
