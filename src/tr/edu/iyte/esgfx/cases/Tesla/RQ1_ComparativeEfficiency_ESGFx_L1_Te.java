package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.RQ1_ComparativeEfficiency_ESGFx_L1;

public class RQ1_ComparativeEfficiency_ESGFx_L1_Te extends CaseStudyUtilities_Te{

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_Te.initializeFilePaths();
		
		RQ1_ComparativeEfficiency_ESGFx_L1 pipelineMeasurement = new RQ1_ComparativeEfficiency_ESGFx_L1();
		pipelineMeasurement.measurePipelineForEventCoverage();
	}

}
