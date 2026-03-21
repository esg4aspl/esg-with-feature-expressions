package tr.edu.iyte.esgfx.cases.BankAccountv2;

import tr.edu.iyte.esgfx.cases.RQ1_ComparativeEfficiency_ESGFx_L1;

public class RQ1_ComparativeEfficiency_ESGFx_L1_BAv2 extends CaseStudyUtilities_BAv2{

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_BAv2.initializeFilePaths();
		
		RQ1_ComparativeEfficiency_ESGFx_L1 pipelineMeasurement = new RQ1_ComparativeEfficiency_ESGFx_L1();
		pipelineMeasurement.measurePipelineForEventCoverage();
	}

}
