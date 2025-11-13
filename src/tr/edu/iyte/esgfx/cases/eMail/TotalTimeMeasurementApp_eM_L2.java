package tr.edu.iyte.esgfx.cases.eMail;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement;

public class TotalTimeMeasurementApp_eM_L2 extends CaseStudyUtilities_eM {

	public static void main(String[] args) throws Exception {
		
		coverageLength = 2;
		
		CaseStudyUtilities_eM.initializeFilePaths();

		TotalTimeMeasurement totalTimeMeasurement = new TotalTimeMeasurement();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();

	}
}
