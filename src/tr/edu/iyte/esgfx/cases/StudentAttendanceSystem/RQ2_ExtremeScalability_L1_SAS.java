package tr.edu.iyte.esgfx.cases.StudentAttendanceSystem;

import tr.edu.iyte.esgfx.cases.RQ2_ExtremeScalability_L1;

public class RQ2_ExtremeScalability_L1_SAS extends CaseStudyUtilities_SAS {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_SAS.initializeFilePaths();
		/*
		 * Event coverage
		 */
		RQ2_ExtremeScalability_L1 totalTimeMeasurement = new RQ2_ExtremeScalability_L1();
		totalTimeMeasurement.measurePipelineForESGFxEventCoverage();
	}

}
