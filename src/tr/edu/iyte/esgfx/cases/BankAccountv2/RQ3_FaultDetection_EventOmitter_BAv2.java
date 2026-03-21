package tr.edu.iyte.esgfx.cases.BankAccountv2;

import tr.edu.iyte.esgfx.cases.RQ3_FaultDetection_EventOmitter;

public class RQ3_FaultDetection_EventOmitter_BAv2 extends CaseStudyUtilities_BAv2 {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_BAv2.initializeFilePaths();
		
		RQ3_FaultDetection_EventOmitter mutantGeneratorEventOmitter = new RQ3_FaultDetection_EventOmitter();
		mutantGeneratorEventOmitter.evaluateFaultDetection();

	}
}
