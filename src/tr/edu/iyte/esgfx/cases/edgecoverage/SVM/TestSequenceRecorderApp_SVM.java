package tr.edu.iyte.esgfx.cases.edgecoverage.SVM;

import tr.edu.iyte.esgfx.cases.edgecoverage.TestSequenceRecorder;

public class TestSequenceRecorderApp_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_SVM.initializeFilePaths();

		TestSequenceRecorder tsr = new TestSequenceRecorder();
		tsr.recordTestSequences();

	}
}
