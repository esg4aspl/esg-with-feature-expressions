package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement;

public class TotalTimeMeasurementApp_Te_L4 extends CaseStudyUtilities_Tesla {

	public static void main(String[] args) throws Exception {
		
		coverageLength = 4;
		
		CaseStudyUtilities_Tesla.initializeFilePaths();
		
		TotalTimeMeasurement totalTimeMeasurement = new TotalTimeMeasurement();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
