package tr.edu.iyte.esgfx.cases.Elevator;

import tr.edu.iyte.esgfx.cases.RQ2_ExtremeScalability_L1;

public class RQ2_ExtremeScalability_L1_El extends CaseStudyUtilities_El {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_El.coverageLength = 1;
		
		CaseStudyUtilities_El.initializeFilePaths();
		
		/*
		 * Event coverage
		 */
		RQ2_ExtremeScalability_L1 totalTimeMeasurement = new RQ2_ExtremeScalability_L1();
		totalTimeMeasurement.measurePipelineForESGFxEventCoverage();

	}

}
