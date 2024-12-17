package tr.edu.iyte.esgfx.cases.edgecoverage.StudentAttendanceSystem;

import tr.edu.iyte.esgfx.cases.AutomaticProductConfigurationGeneration;

public class AutomaticProductConfigurationGeneratorApp extends CaseStudyUtilities_SAS {

	public static void main(String[] args) throws Exception {

        // Initialize paths and parsers
    	CaseStudyUtilities_SAS.initializeFilePaths();

    	AutomaticProductConfigurationGeneration automaticProductConfigurationGeneration = new AutomaticProductConfigurationGeneration();
    	automaticProductConfigurationGeneration.writeAllProductConfigurationsToFile();
    }
}
