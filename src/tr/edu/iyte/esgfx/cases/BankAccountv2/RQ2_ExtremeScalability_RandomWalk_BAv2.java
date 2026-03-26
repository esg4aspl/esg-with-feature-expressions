package tr.edu.iyte.esgfx.cases.BankAccountv2;

import tr.edu.iyte.esgfx.cases.RQ2_ExtremeScalability_RandomWalk;

public class RQ2_ExtremeScalability_RandomWalk_BAv2 extends CaseStudyUtilities_BAv2 {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_BAv2.initializeFilePaths();
		
		RQ2_ExtremeScalability_RandomWalk rq2 = new RQ2_ExtremeScalability_RandomWalk();
		rq2.measureRandomWalkScalability();
	}

}
