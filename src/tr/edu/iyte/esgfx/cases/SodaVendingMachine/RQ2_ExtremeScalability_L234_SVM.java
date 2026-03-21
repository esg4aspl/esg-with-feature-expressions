package tr.edu.iyte.esgfx.cases.SodaVendingMachine;

import tr.edu.iyte.esgfx.cases.RQ2_ExtremeScalability_L234;

public class RQ2_ExtremeScalability_L234_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {
		coverageLength = 3;
		CaseStudyUtilities_SVM.initializeFilePaths();
		
		/*
		 * Event couple coverage
		 */
		RQ2_ExtremeScalability_L234 totalTimeMeasurement = new RQ2_ExtremeScalability_L234();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
