package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.RQ3_FaultDetection_EdgeOmitter;

public class RQ3_FaultDetection_EdgeOmitter_Te extends CaseStudyUtilities_Te {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_Te.initializeFilePaths();
		
		RQ3_FaultDetection_EdgeOmitter edgeOmitterMutantGenerator = new RQ3_FaultDetection_EdgeOmitter();
		edgeOmitterMutantGenerator.evaluateFaultDetection();

	}
}
