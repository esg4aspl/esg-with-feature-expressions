package tr.edu.iyte.esgfx.cases.SodaVendingMachine;

import tr.edu.iyte.esgfx.cases.RQ3_FaultDetection_EdgeOmitter;

public class RQ3_FaultDetection_EdgeOmitter_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {

		
		CaseStudyUtilities_SVM.initializeFilePaths();
		
		RQ3_FaultDetection_EdgeOmitter edgeOmitterMutantGenerator = new RQ3_FaultDetection_EdgeOmitter();
		edgeOmitterMutantGenerator.evaluateFaultDetection();

	}
}
