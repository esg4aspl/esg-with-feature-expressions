package tr.edu.iyte.esgfx.cases.edgecoverage.Tesla;

import tr.edu.iyte.esgfx.cases.edgecoverage.TotalTimeMeasurement;

public class TotalTimeMeasurementApp_Te extends CaseStudyUtilities_Tesla {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_Tesla.initializeFilePaths();
		
		TotalTimeMeasurement totalTimeMeasurement = new TotalTimeMeasurement();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
