package tr.edu.iyte.esgfx.cases.SVM;

import tr.edu.iyte.esgfx.cases.CaseStudyUtilities;

public class CaseStudyUtilities_SVM extends CaseStudyUtilities {

	public static void initializeFilePaths() {
		
		caseStudyFolderPath = "files/Cases/SodaVendingMachine/";
		SPLName = "SVM";
		
		mutantEventName = "Event";
		mutantFeatureName = "Feature";

		setCoverageType();

	}

}
