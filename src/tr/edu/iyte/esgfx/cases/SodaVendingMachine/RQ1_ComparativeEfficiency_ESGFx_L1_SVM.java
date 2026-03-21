package tr.edu.iyte.esgfx.cases.SodaVendingMachine;

import tr.edu.iyte.esgfx.cases.RQ1_ComparativeEfficiency_ESGFx_L1;

public class RQ1_ComparativeEfficiency_ESGFx_L1_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_SVM.initializeFilePaths();
		
		
		RQ1_ComparativeEfficiency_ESGFx_L1 pipelineMeasurement = new RQ1_ComparativeEfficiency_ESGFx_L1();
		pipelineMeasurement.measurePipelineForEventCoverage();

	}

}
