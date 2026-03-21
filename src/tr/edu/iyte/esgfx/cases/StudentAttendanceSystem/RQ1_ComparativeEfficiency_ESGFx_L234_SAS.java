package tr.edu.iyte.esgfx.cases.StudentAttendanceSystem;

import tr.edu.iyte.esgfx.cases.RQ1_ComparativeEfficiency_ESGFx_L234;

public class RQ1_ComparativeEfficiency_ESGFx_L234_SAS extends CaseStudyUtilities_SAS {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_SAS.initializeFilePaths();
		
		RQ1_ComparativeEfficiency_ESGFx_L234 pipelineMeasurement = new RQ1_ComparativeEfficiency_ESGFx_L234();
		pipelineMeasurement.measurePipelineForEdgeCoverage();
		
	}

}
