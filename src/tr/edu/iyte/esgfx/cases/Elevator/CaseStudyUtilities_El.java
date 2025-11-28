package tr.edu.iyte.esgfx.cases.Elevator;

import tr.edu.iyte.esgfx.cases.CaseStudyUtilities;

public class CaseStudyUtilities_El extends CaseStudyUtilities {
	
	public static void initializeFilePaths() {
		
		caseStudyFolderPath = "files/Cases/Elevator/";
		SPLName = "El";
		
		mutantEventName = "e";
		mutantFeatureName = "f";
		
		setCoverageType();

	}
}
