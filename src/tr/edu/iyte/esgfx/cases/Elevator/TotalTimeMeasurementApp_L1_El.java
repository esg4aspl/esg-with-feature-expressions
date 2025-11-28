package tr.edu.iyte.esgfx.cases.Elevator;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement_L1;

public class TotalTimeMeasurementApp_L1_El extends CaseStudyUtilities_El {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_El.coverageLength = 1;
		CaseStudyUtilities_El.initializeFilePaths();
		
		/*
		 * Event coverage
		 */
		TotalTimeMeasurement_L1 totalTimeMeasurement = new TotalTimeMeasurement_L1();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
