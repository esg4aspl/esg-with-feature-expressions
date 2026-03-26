package tr.edu.iyte.esgfx.cases.Elevator;

import tr.edu.iyte.esgfx.cases.RQ2_ExtremeScalability_RandomWalk;

public class RQ2_ExtremeScalability_RandomWalk_El extends CaseStudyUtilities_El {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_El.initializeFilePaths();
		
		RQ2_ExtremeScalability_RandomWalk rq2 = new RQ2_ExtremeScalability_RandomWalk();
		rq2.measureRandomWalkScalability();
	}

}
