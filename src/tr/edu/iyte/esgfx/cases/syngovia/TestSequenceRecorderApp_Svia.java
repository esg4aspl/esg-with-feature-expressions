package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.TestSequenceRecorder;

public class TestSequenceRecorderApp_Svia extends CaseStudyUtilities_Svia {

	public static void main(String[] args) throws Exception {


		CaseStudyUtilities_Svia.initializeFilePaths();

		TestSequenceRecorder tsr = new TestSequenceRecorder();
		tsr.recordTestSequences();

	}
}
