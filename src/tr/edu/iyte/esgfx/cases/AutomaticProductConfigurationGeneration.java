package tr.edu.iyte.esgfx.cases;

import tr.edu.iyte.esgfx.productconfigurationgeneration.AutomaticProductConfigurationGenerator;

public class AutomaticProductConfigurationGeneration extends CaseStudyUtilities {
	
	public void writeAllProductConfigurationsToFile() throws Exception {
		featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFile,
				ESGFxFile);
//		printFeatureExpressionMapFromFeatureModel(featureExpressionMapFromFeatureModel);
		
		AutomaticProductConfigurationGenerator automaticProductConfigurationGenerator = new AutomaticProductConfigurationGenerator();
		automaticProductConfigurationGenerator.writeAllProductConfigurationsToFile(featureModel, featureExpressionMapFromFeatureModel);
	}

}
