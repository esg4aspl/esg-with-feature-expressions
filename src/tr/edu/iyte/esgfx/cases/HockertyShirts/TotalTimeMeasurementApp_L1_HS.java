package tr.edu.iyte.esgfx.cases.HockertyShirts;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement_L1;

public class TotalTimeMeasurementApp_L1_HS extends CaseStudyUtilities_HS {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_HS.coverageLength = 1;
		CaseStudyUtilities_HS.initializeFilePaths();
		
		/*
		 * Event coverage
		 */
		TotalTimeMeasurement_L1 totalTimeMeasurement = new TotalTimeMeasurement_L1();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
