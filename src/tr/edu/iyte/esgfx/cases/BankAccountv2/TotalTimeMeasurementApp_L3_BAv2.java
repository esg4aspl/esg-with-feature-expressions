package tr.edu.iyte.esgfx.cases.BankAccountv2;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement_L234;

public class TotalTimeMeasurementApp_L3_BAv2 extends CaseStudyUtilities_BAv2 {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_BAv2.coverageLength = 3;
		CaseStudyUtilities_BAv2.initializeFilePaths();
		
		/*
		 * Event triple coverage
		 */
		TotalTimeMeasurement_L234 totalTimeMeasurement = new TotalTimeMeasurement_L234();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
