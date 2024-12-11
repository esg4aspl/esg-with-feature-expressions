package tr.edu.iyte.esgfx.cases.edgecoverage.eMail;

import tr.edu.iyte.esgfx.cases.edgecoverage.TotalTimeMeasurement;

public class TotalTimeMeasurementApp_eM extends CaseStudyUtilities_eM {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_eM.initializeFilePaths();

		TotalTimeMeasurement totalTimeMeasurement = new TotalTimeMeasurement();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();

	}
}
