package tr.edu.iyte.esgfx.cases.BankAccountv2;

import tr.edu.iyte.esgfx.cases.CaseStudyUtilities;

public class CaseStudyUtilities_BAv2 extends CaseStudyUtilities {

	public static void initializeFilePaths() {

		caseStudyFolderPath = "files/Cases/BankAccountv2/";
		SPLName = "BAv2";
		
		mutantEventName = "update credentials";
		mutantFeatureName = "b";
		
		setCoverageType();
	}

}
