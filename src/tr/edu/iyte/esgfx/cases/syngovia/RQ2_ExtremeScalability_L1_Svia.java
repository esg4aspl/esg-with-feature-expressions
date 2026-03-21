package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.RQ2_ExtremeScalability_L1;


public class RQ2_ExtremeScalability_L1_Svia extends CaseStudyUtilities_Svia {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_Svia.initializeFilePaths();
		
		/*
		 * Event coverage
		 */
		RQ2_ExtremeScalability_L1 totalTimeMeasurement = new RQ2_ExtremeScalability_L1();
		totalTimeMeasurement.measurePipelineForESGFxEventCoverage();
	}

}
