package tr.edu.iyte.esgfx.cases.Elevator;

import tr.edu.iyte.esgfx.cases.RQ1_ComparativeEfficiency_ESGFx_L1;


public class RQ1_ComparativeEfficiency_ESGFx_L1_El extends CaseStudyUtilities_El {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_El.initializeFilePaths();

		RQ1_ComparativeEfficiency_ESGFx_L1 pipelineMeasurement = new RQ1_ComparativeEfficiency_ESGFx_L1();
		pipelineMeasurement.measurePipelineForEventCoverage();
		
	}

}
