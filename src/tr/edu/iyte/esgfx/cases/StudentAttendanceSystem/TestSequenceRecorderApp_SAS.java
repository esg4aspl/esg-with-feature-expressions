package tr.edu.iyte.esgfx.cases.StudentAttendanceSystem;

import tr.edu.iyte.esgfx.cases.TestSequenceRecorder;

public class TestSequenceRecorderApp_SAS extends CaseStudyUtilities_SAS {

	public static void main(String[] args) throws Exception {


		CaseStudyUtilities_SAS.initializeFilePaths();

		TestSequenceRecorder tsr = new TestSequenceRecorder();
		tsr.recordTestSequences();

	}
}
