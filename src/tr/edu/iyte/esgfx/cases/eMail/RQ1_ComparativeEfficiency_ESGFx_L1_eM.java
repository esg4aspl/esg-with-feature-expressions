package tr.edu.iyte.esgfx.cases.eMail;

import tr.edu.iyte.esgfx.cases.RQ1_ComparativeEfficiency_ESGFx_L1;

public class RQ1_ComparativeEfficiency_ESGFx_L1_eM extends CaseStudyUtilities_eM {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_eM.initializeFilePaths();
		
		RQ1_ComparativeEfficiency_ESGFx_L1 pipelineMeasurement = new RQ1_ComparativeEfficiency_ESGFx_L1();
		pipelineMeasurement.measurePipelineForEventCoverage();
	}

}
