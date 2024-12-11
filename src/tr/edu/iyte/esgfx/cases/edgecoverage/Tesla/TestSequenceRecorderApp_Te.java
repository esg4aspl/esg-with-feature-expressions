package tr.edu.iyte.esgfx.cases.edgecoverage.Tesla;

import tr.edu.iyte.esgfx.cases.edgecoverage.TestSequenceRecorder;

public class TestSequenceRecorderApp_Te extends CaseStudyUtilities_Tesla {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_Tesla.initializeFilePaths();

		TestSequenceRecorder tsr = new TestSequenceRecorder();
		tsr.recordTestSequences();

	}
}
