package tr.edu.iyte.esgfx.cases.Elevator;

import tr.edu.iyte.esgfx.cases.RQ2_ExtremeScalability_EFG_L234;

public class RQ2_ExtremeScalability_EFG_L234_El extends CaseStudyUtilities_El {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_El.initializeFilePaths();

		RQ2_ExtremeScalability_EFG_L234 rq2 = new RQ2_ExtremeScalability_EFG_L234();
		rq2.measureEFGScalability();
	}

}
