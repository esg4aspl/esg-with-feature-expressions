package tr.edu.iyte.esgfx.cases.SVM;

import tr.edu.iyte.esgfx.cases.AutomaticProductConfigurationGeneration;

public class AutomaticProductConfigurationGeneratorApp extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {
		
		coverageLength = 2;
		
		CaseStudyUtilities_SVM.initializeFilePaths();

		AutomaticProductConfigurationGeneration automaticProductConfigurationGeneration = new AutomaticProductConfigurationGeneration();
		automaticProductConfigurationGeneration.writeAllProductConfigurationsToFile();
	}
}
