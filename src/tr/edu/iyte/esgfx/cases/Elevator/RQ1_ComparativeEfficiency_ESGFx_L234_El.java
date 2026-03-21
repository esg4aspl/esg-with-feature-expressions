package tr.edu.iyte.esgfx.cases.Elevator;

import tr.edu.iyte.esgfx.cases.RQ1_ComparativeEfficiency_ESGFx_L234;

public class RQ1_ComparativeEfficiency_ESGFx_L234_El extends CaseStudyUtilities_El {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_El.initializeFilePaths();
		
		RQ1_ComparativeEfficiency_ESGFx_L234 pipelineMeasurement = new RQ1_ComparativeEfficiency_ESGFx_L234();
		pipelineMeasurement.measurePipelineForEdgeCoverage();


	}

}
