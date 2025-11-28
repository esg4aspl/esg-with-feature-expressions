package tr.edu.iyte.esgfx.cases.HockertyShirts;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement_L234;

public class TotalTimeMeasurementApp_L2_HS extends CaseStudyUtilities_HS {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_HS.coverageLength = 2;
		CaseStudyUtilities_HS.initializeFilePaths();
		
		/*
		 * Event couple coverage
		 */
		TotalTimeMeasurement_L234 totalTimeMeasurement = new TotalTimeMeasurement_L234();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
