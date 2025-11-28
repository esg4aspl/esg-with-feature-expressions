package tr.edu.iyte.esgfx.cases.eMail;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement_L234;

public class TotalTimeMeasurementApp_L2_eM extends CaseStudyUtilities_eM {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_eM.coverageLength = 2;
		CaseStudyUtilities_eM.initializeFilePaths();
		
		/*
		 * Event couple coverage
		 */
		TotalTimeMeasurement_L234 totalTimeMeasurement = new TotalTimeMeasurement_L234();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
