package tr.edu.iyte.esgfx.cases.StudentAttendanceSystem;

import tr.edu.iyte.esgfx.cases.RQ3_FaultDetection_EdgeOmitter;

public class RQ3_FaultDetection_EdgeOmitter_SAS extends CaseStudyUtilities_SAS {

	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_SAS.initializeFilePaths();
		
		RQ3_FaultDetection_EdgeOmitter edgeOmitterMutantGenerator = new RQ3_FaultDetection_EdgeOmitter();
		edgeOmitterMutantGenerator.evaluateFaultDetection();

	}
}
