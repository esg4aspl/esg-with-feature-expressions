package tr.edu.iyte.esgfx.cases.SodaVendingMachine;

import tr.edu.iyte.esgfx.cases.RQ2_ExtremeScalability_RandomWalk;

public class RQ2_ExtremeScalability_RandomWalk_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_SVM.initializeFilePaths();
		
		RQ2_ExtremeScalability_RandomWalk rq2 = new RQ2_ExtremeScalability_RandomWalk();
		rq2.measureRandomWalkScalability();
	}

}
