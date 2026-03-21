package tr.edu.iyte.esgfx.cases.HockertyShirts;

import tr.edu.iyte.esgfx.cases.RQ1_ComparativeEfficiency_EFG_L234;

public class RQ1_ComparativeEfficiency_EFG_L234_HS extends CaseStudyUtilities_HS {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_HS.initializeFilePaths();

		RQ1_ComparativeEfficiency_EFG_L234 pipelineMeasurement = new RQ1_ComparativeEfficiency_EFG_L234();
		pipelineMeasurement.measureTotalTimeForEFGPipeline();

	}
}
