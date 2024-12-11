package tr.edu.iyte.esgfx.cases.edgecoverage.BankAccountv2;

import tr.edu.iyte.esgfx.cases.edgecoverage.TestSequenceRecorder;

public class TestSequenceRecorderApp_BAv2 {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_BAv2.initializeFilePaths();

		TestSequenceRecorder tsr = new TestSequenceRecorder();
		tsr.recordTestSequences();

	}
}
