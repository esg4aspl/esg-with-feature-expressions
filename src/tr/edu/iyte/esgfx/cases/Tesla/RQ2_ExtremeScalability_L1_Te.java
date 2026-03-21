package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.RQ2_ExtremeScalability_L1;

public class RQ2_ExtremeScalability_L1_Te extends CaseStudyUtilities_Te {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_Te.coverageLength = 1;
		CaseStudyUtilities_Te.initializeFilePaths();
		
		/*
		 * Event coverage
		 */
		RQ2_ExtremeScalability_L1 totalTimeMeasurement = new RQ2_ExtremeScalability_L1();
		totalTimeMeasurement.measurePipelineForESGFxEventCoverage();
	}

}
