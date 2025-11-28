package tr.edu.iyte.esgfx.cases.Elevator;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement_L234;


public class TotalTimeMeasurementApp_L4_El extends CaseStudyUtilities_El {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_El.coverageLength = 4;
		CaseStudyUtilities_El.initializeFilePaths();
		
		/*
		 * Event quadruple coverage
		 */
		TotalTimeMeasurement_L234 totalTimeMeasurement = new TotalTimeMeasurement_L234();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
