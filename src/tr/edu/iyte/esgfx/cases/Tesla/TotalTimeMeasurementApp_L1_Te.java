package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement_L1;

public class TotalTimeMeasurementApp_L1_Te extends CaseStudyUtilities_Te {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_Te.coverageLength = 1;
		CaseStudyUtilities_Te.initializeFilePaths();
		
		/*
		 * Event coverage
		 */
		TotalTimeMeasurement_L1 totalTimeMeasurement = new TotalTimeMeasurement_L1();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
