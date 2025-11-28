package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement_L234;

public class TotalTimeMeasurementApp_L2_Svia extends CaseStudyUtilities_Svia {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_Svia.coverageLength = 2;
		CaseStudyUtilities_Svia.initializeFilePaths();
		
		/*
		 * Event couple coverage
		 */
		TotalTimeMeasurement_L234 totalTimeMeasurement = new TotalTimeMeasurement_L234();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
