package tr.edu.iyte.esgfx.cases.eMail;

import tr.edu.iyte.esgfx.cases.RQ3_FaultDetection_EdgeOmitter;

public class RQ3_FaultDetection_EdgeOmitter_eM extends CaseStudyUtilities_eM {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_eM.initializeFilePaths();
		
		RQ3_FaultDetection_EdgeOmitter edgeOmitterMutantGenerator = new RQ3_FaultDetection_EdgeOmitter();
		edgeOmitterMutantGenerator.evaluateFaultDetection();

	}
}
