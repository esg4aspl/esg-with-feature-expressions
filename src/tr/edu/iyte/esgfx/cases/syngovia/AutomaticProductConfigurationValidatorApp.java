package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.AutomaticProductConfigurationValidator;

public class AutomaticProductConfigurationValidatorApp extends CaseStudyUtilities_Svia {

	public static void main(String[] args) throws Exception {
		
		coverageLength = 2;
		
		CaseStudyUtilities_Svia.initializeFilePaths();

		AutomaticProductConfigurationValidator automaticProductConfigurationValidator = new AutomaticProductConfigurationValidator();
		automaticProductConfigurationValidator.validateProductConfigurations();
	}
}
