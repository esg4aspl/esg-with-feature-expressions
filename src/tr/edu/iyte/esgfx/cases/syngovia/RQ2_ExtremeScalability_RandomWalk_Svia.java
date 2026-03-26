package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.RQ2_ExtremeScalability_RandomWalk;

public class RQ2_ExtremeScalability_RandomWalk_Svia extends CaseStudyUtilities_Svia {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_Svia.initializeFilePaths();
		
		RQ2_ExtremeScalability_RandomWalk rq2 = new RQ2_ExtremeScalability_RandomWalk();
		rq2.measureRandomWalkScalability();
	}

}
