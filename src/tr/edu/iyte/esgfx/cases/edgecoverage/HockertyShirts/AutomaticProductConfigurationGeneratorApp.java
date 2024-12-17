package tr.edu.iyte.esgfx.cases.edgecoverage.HockertyShirts;

import tr.edu.iyte.esgfx.cases.AutomaticProductConfigurationGeneration;

public class AutomaticProductConfigurationGeneratorApp extends CaseStudyUtilities_HS {

    public static void main(String[] args) throws Exception {

        // Initialize paths and parsers
        CaseStudyUtilities_HS.initializeFilePaths();
		
		AutomaticProductConfigurationGeneration automaticProductConfigurationGeneration = new AutomaticProductConfigurationGeneration();
		automaticProductConfigurationGeneration.writeAllProductConfigurationsToFile();

    }
}

