package tr.edu.iyte.esgfx.cases.HockertyShirts;

import tr.edu.iyte.esgfx.cases.TestSequenceRecorder;

public class TestSequenceRecorderApp_HS extends CaseStudyUtilities_HS {

	public static void main(String[] args) throws Exception {


			CaseStudyUtilities_HS.initializeFilePaths();

			TestSequenceRecorder tsr = new TestSequenceRecorder();
			tsr.recordTestSequences();

		

	}
}
