package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.CaseStudyUtilities;

public class CaseStudyUtilities_Svia extends CaseStudyUtilities {

	public static void initializeFilePaths() {

		caseStudyFolderPath = "files/Cases/syngovia/";
		SPLName = "Svia";
		
		mutantEventName = "Event";
		mutantFeatureName = "Feature";
		
		setCoverageType();

	}

}
