package tr.edu.iyte.esgfx.cases.Elevator;

import tr.edu.iyte.esgfx.cases.AutomaticProductConfigurationGeneration;

public class AutomaticProductConfigurationGenerator_El extends CaseStudyUtilities_El {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_El.initializeFilePaths();

		AutomaticProductConfigurationGeneration automaticProductConfigurationGeneration = new AutomaticProductConfigurationGeneration();
		automaticProductConfigurationGeneration.writeAllProductConfigurationsToFile();
	}
}
