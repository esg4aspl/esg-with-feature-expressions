package tr.edu.iyte.esgfx.cases.Elevator;

import tr.edu.iyte.esgfx.cases.RQ2_ExtremeScalability_L234;

public class RQ2_ExtremeScalability_L234_El extends CaseStudyUtilities_El {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_El.initializeFilePaths();
		
		/*
		 * Event couple/triple/quadruple coverage
		 */
		RQ2_ExtremeScalability_L234 totalTimeMeasurement = new RQ2_ExtremeScalability_L234();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
