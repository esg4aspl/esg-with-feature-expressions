package tr.edu.iyte.esgfx.cases.BankAccountv2;

import tr.edu.iyte.esgfx.cases.RQ2_ExtremeScalability_EFG_L234;

public class RQ2_ExtremeScalability_EFG_L234_BAv2 extends CaseStudyUtilities_BAv2 {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_BAv2.initializeFilePaths();

		RQ2_ExtremeScalability_EFG_L234 rq2 = new RQ2_ExtremeScalability_EFG_L234();
		rq2.measureEFGScalability();
	}

}
