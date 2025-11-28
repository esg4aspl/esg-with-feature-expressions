package tr.edu.iyte.esgfx.cases.Elevator;

import tr.edu.iyte.esgfx.cases.TestSequenceRecorder;

public class TestSequenceRecorderApp_El extends CaseStudyUtilities_El {

	public static void main(String[] args) throws Exception {


			CaseStudyUtilities_El.initializeFilePaths();

			TestSequenceRecorder tsr = new TestSequenceRecorder();
			tsr.recordTestSequences();

		
	}
}
