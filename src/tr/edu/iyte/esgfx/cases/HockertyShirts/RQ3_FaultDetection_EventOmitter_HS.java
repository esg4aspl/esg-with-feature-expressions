package tr.edu.iyte.esgfx.cases.HockertyShirts;

import tr.edu.iyte.esgfx.cases.RQ3_FaultDetection_EventOmitter;

public class RQ3_FaultDetection_EventOmitter_HS extends CaseStudyUtilities_HS {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_HS.initializeFilePaths();
		
		RQ3_FaultDetection_EventOmitter mutantGeneratorEventOmitter = new RQ3_FaultDetection_EventOmitter();
		mutantGeneratorEventOmitter.evaluateFaultDetection();
	}
}
