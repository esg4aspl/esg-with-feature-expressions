package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement_L234;

public class TotalTimeMeasurementApp_L3_Te extends CaseStudyUtilities_Te {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_Te.coverageLength = 3;
		CaseStudyUtilities_Te.initializeFilePaths();
		
		/*
		 * Event triple coverage
		 */
		TotalTimeMeasurement_L234 totalTimeMeasurement = new TotalTimeMeasurement_L234();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
