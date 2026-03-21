package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.RQ3_FaultDetection_EdgeOmitter;

public class RQ3_FaultDetection_EdgeOmitter_Svia extends CaseStudyUtilities_Svia {

	public static void main(String[] args) throws Exception {
		
		
		CaseStudyUtilities_Svia.initializeFilePaths();
		
		RQ3_FaultDetection_EdgeOmitter edgeOmitterMutantGenerator = new RQ3_FaultDetection_EdgeOmitter();
		edgeOmitterMutantGenerator.evaluateFaultDetection();

	}
}
