package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.RQ2_ExtremeScalability_EFG_L234;

public class RQ2_ExtremeScalability_EFG_L234_Svia extends CaseStudyUtilities_Svia {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_Svia.initializeFilePaths();

		RQ2_ExtremeScalability_EFG_L234 rq2 = new RQ2_ExtremeScalability_EFG_L234();
		rq2.measureEFGScalability();
	}

}
