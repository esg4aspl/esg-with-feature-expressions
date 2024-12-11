package tr.edu.iyte.esgfx.cases.edgecoverage.BankAccountv2;

import tr.edu.iyte.esgfx.cases.edgecoverage.TotalTimeMeasurement;

public class TotalTimeMeasurementApp_BAv2 {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_BAv2.initializeFilePaths();

		TotalTimeMeasurement totalTimeMeasurement = new TotalTimeMeasurement();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}
}
