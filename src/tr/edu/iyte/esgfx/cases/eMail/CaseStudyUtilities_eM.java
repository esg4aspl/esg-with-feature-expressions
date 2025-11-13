package tr.edu.iyte.esgfx.cases.eMail;

import tr.edu.iyte.esgfx.cases.CaseStudyUtilities;

public class CaseStudyUtilities_eM extends CaseStudyUtilities {
	
	public static void initializeFilePaths() {
		
		caseStudyFolderPath = "files/Cases/eMail/";
		SPLName = "eM";
		
		mutantEventName = "mark as spam";
		mutantFeatureName = "sp";
		
		setCoverageType();

	}
}
