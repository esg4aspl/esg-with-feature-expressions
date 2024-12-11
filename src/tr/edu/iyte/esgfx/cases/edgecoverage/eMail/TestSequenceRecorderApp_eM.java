package tr.edu.iyte.esgfx.cases.edgecoverage.eMail;

import tr.edu.iyte.esgfx.cases.edgecoverage.TestSequenceRecorder;

public class TestSequenceRecorderApp_eM extends CaseStudyUtilities_eM {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_eM.initializeFilePaths();
		
		TestSequenceRecorder tsr = new TestSequenceRecorder();
		tsr.recordTestSequences();
	}
}
