package tr.edu.iyte.esgfx.cases.HockertyShirts;

import tr.edu.iyte.esgfx.cases.RQ2_ExtremeScalability_L234;

public class RQ2_ExtremeScalability_L234_HS extends CaseStudyUtilities_HS {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_HS.initializeFilePaths();
		
		/*
		 * Event couple coverage
		 */
		RQ2_ExtremeScalability_L234 totalTimeMeasurement = new RQ2_ExtremeScalability_L234();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
