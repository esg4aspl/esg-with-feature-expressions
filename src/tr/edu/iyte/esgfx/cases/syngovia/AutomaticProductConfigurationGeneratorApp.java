package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.AutomaticProductConfigurationGeneration;

public class AutomaticProductConfigurationGeneratorApp extends CaseStudyUtilities_Svia {

	public static void main(String[] args) throws Exception {
		
		coverageLength = 2;
		
		CaseStudyUtilities_Svia.initializeFilePaths();

		AutomaticProductConfigurationGeneration automaticProductConfigurationGeneration = new AutomaticProductConfigurationGeneration();
		automaticProductConfigurationGeneration.writeAllProductConfigurationsToFile();
	}
}
