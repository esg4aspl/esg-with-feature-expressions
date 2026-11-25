package tr.edu.iyte.esgfx.cases.HockertyShirts;

import tr.edu.iyte.esgfx.cases.CaseStudyUtilities;

public class CaseStudyUtilities_HS extends CaseStudyUtilities {

	public static void initializeFilePaths() {

		caseStudyFolder = "files/Cases/HockertyShirts/";
		SPLName = "HS";
		// The feature model file declares Accents optional. A shirt without Accents
		// has no button and its product-level model has no complete event sequence
		// (found by the static analysis), so Accents is taken as mandatory here. The
		// model file and its DIMACS file are left as they are.
		mandatoryFeatures = new String[] { "Accents" };
		
		mutantEventName = "Event";
		mutantFeatureName = "Feature";

		setCoverageType();

	}

}
