package tr.edu.iyte.esgfx.cases.SVM;

import tr.edu.iyte.esgfx.cases.TestSequenceRecorder;

public class TestSequenceRecorderApp_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_SVM.initializeFilePaths();

		TestSequenceRecorder tsr = new TestSequenceRecorder();
		tsr.recordTestSequences();

	}
}
