package tr.edu.iyte.esgfx.cases.SodaVendingMachine;

import tr.edu.iyte.esgfx.cases.RQ2_ExtremeScalability_L1;

public class RQ2_ExtremeScalability_L1_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_SVM.initializeFilePaths();
		/*
		 * Event coverage
		 */
		RQ2_ExtremeScalability_L1 totalTimeMeasurement = new RQ2_ExtremeScalability_L1();
		totalTimeMeasurement.measurePipelineForESGFxEventCoverage();
	}

}
