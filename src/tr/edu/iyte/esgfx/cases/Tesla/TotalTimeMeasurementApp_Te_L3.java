package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement;

public class TotalTimeMeasurementApp_Te_L3 extends CaseStudyUtilities_Tesla {

	public static void main(String[] args) throws Exception {
		
		coverageLength = 3;
		
		CaseStudyUtilities_Tesla.initializeFilePaths();
		
		TotalTimeMeasurement totalTimeMeasurement = new TotalTimeMeasurement();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
