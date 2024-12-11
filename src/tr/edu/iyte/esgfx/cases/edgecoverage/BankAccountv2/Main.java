package tr.edu.iyte.esgfx.cases.edgecoverage.BankAccountv2;

import tr.edu.iyte.esgfx.cases.AutomaticProductConfigurationGeneration;

public class Main extends CaseStudyUtilities_BAv2 {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_BAv2.initializeFilePaths();
		
		AutomaticProductConfigurationGeneration automaticProductConfigurationGeneration = new AutomaticProductConfigurationGeneration();
		automaticProductConfigurationGeneration.generateProductConfigurations();
	}

}
