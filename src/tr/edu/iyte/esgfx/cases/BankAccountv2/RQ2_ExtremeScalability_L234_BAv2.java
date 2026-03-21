package tr.edu.iyte.esgfx.cases.BankAccountv2;

import tr.edu.iyte.esgfx.cases.RQ2_ExtremeScalability_L234;

public class RQ2_ExtremeScalability_L234_BAv2 extends CaseStudyUtilities_BAv2 {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_BAv2.coverageLength = 2;
		CaseStudyUtilities_BAv2.initializeFilePaths();
		
		/*
		 * Event couple/triple/quadruple coverage
		 */
		RQ2_ExtremeScalability_L234 totalTimeMeasurement = new RQ2_ExtremeScalability_L234();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
