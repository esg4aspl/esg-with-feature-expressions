package tr.edu.iyte.esgfx.cases.BankAccountv2;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement;

public class TotalTimeMeasurementApp_BAv2_L3 extends CaseStudyUtilities_BAv2 {

	public static void main(String[] args) throws Exception {
		coverageLength = 3;
		initializeFilePaths();

		TotalTimeMeasurement totalTimeMeasurement = new TotalTimeMeasurement();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}
}
