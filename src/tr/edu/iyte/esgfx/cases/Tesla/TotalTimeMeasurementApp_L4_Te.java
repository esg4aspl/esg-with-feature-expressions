package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement_L234;


public class TotalTimeMeasurementApp_L4_Te extends CaseStudyUtilities_Te {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_Te.coverageLength = 4;
		CaseStudyUtilities_Te.initializeFilePaths();
		
		/*
		 * Event quadruple coverage
		 */
		TotalTimeMeasurement_L234 totalTimeMeasurement = new TotalTimeMeasurement_L234();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
