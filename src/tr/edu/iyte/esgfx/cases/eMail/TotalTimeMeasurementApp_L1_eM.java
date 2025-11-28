package tr.edu.iyte.esgfx.cases.eMail;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement_L1;

public class TotalTimeMeasurementApp_L1_eM extends CaseStudyUtilities_eM {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_eM.coverageLength = 1;
		CaseStudyUtilities_eM.initializeFilePaths();
		
		/*
		 * Event coverage
		 */
		TotalTimeMeasurement_L1 totalTimeMeasurement = new TotalTimeMeasurement_L1();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
