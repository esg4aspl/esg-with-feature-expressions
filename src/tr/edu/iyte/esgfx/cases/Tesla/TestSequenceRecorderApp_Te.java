package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.TestSequenceRecorder;

public class TestSequenceRecorderApp_Te extends CaseStudyUtilities_Tesla {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_Tesla.initializeFilePaths();

		TestSequenceRecorder tsr = new TestSequenceRecorder();
		tsr.recordTestSequences();

	}
}
