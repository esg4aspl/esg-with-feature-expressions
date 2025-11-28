package tr.edu.iyte.esgfx.cases.BankAccountv2;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement_L1;

public class TotalTimeMeasurementApp_L1_BAv2 extends CaseStudyUtilities_BAv2 {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_BAv2.coverageLength = 1;
		CaseStudyUtilities_BAv2.initializeFilePaths();
		
		/*
		 * Event coverage
		 */
		TotalTimeMeasurement_L1 totalTimeMeasurement = new TotalTimeMeasurement_L1();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
