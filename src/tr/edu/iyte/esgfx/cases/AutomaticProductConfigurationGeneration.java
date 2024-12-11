package tr.edu.iyte.esgfx.cases;

import tr.edu.iyte.esgfx.productconfigurationgeneration.AutomaticProductConfigurationGenerator;

public class AutomaticProductConfigurationGeneration extends CaseStudyUtilities {
	
	public void generateProductConfigurations() throws Exception {
		featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFilePath,
				ESGFxFilePath);
		printFeatureExpressionMapFromFeatureModel(featureExpressionMapFromFeatureModel);
		
		AutomaticProductConfigurationGenerator automaticProductConfigurationGenerator = new AutomaticProductConfigurationGenerator();
		automaticProductConfigurationGenerator.getAllProductConfigurations(featureModel, featureExpressionMapFromFeatureModel);
	}

}
