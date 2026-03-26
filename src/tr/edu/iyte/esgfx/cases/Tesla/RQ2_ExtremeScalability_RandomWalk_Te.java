package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.RQ2_ExtremeScalability_RandomWalk;

public class RQ2_ExtremeScalability_RandomWalk_Te extends CaseStudyUtilities_Te {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_Te.initializeFilePaths();
		
		RQ2_ExtremeScalability_RandomWalk rq2 = new RQ2_ExtremeScalability_RandomWalk();
		rq2.measureRandomWalkScalability();
	}

}
