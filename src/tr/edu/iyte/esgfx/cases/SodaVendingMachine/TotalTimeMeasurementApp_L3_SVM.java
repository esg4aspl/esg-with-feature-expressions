package tr.edu.iyte.esgfx.cases.SodaVendingMachine;

import tr.edu.iyte.esgfx.cases.RQ2_ExtremeScalability_L234;

public class TotalTimeMeasurementApp_L3_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_SVM.coverageLength = 3;
		CaseStudyUtilities_SVM.initializeFilePaths();
		
		/*
		 * Event triple coverage
		 */
		RQ2_ExtremeScalability_L234 totalTimeMeasurement = new RQ2_ExtremeScalability_L234();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
