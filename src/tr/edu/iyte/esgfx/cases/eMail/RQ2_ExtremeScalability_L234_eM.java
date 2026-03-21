package tr.edu.iyte.esgfx.cases.eMail;

import tr.edu.iyte.esgfx.cases.RQ2_ExtremeScalability_L234;

public class RQ2_ExtremeScalability_L234_eM extends CaseStudyUtilities_eM {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_eM.initializeFilePaths();
		
		/*
		 * Event couple coverage
		 */
		RQ2_ExtremeScalability_L234 totalTimeMeasurement = new RQ2_ExtremeScalability_L234();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
