package tr.edu.iyte.esgfx.cases;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esgfx.conversion.mxe.MXEFileToESGFxConverter;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;
import tr.edu.iyte.esgfx.productconfigurationgeneration.ProductConfigurationValidator;

public class CaseStudyUtilities {

	protected static String featureModelFilePath;
	protected static String ESGFxFilePath;

	protected static String detailedFaultDetectionResults;
	protected static String faultDetectionResultsForSPL;

	protected static String testsequencesFolderPath;
	protected static String timemeasurementFolderPath;

	protected static String testSuiteFilePath_edgeCoverage;
	protected static String SPLName;
	
	protected static String featureESGSetFolderPath_FeatureInsertion;
	protected static String featureESGSetFolderPath_FeatureOmission;
	
	protected static String productConfigurationFilePath;

	protected FeatureModel featureModel;
	protected ESG ESGFx;
	protected Map<String, FeatureExpression> featureExpressionMapFromFeatureModel;

	protected Map<String, FeatureExpression> generateFeatureExpressionMapFromFeatureModel(String featureModelFilePath,
			String ESGFxFilePath) throws Exception {
		MXEFileToESGFxConverter MXEFileToESGFxConverter = new MXEFileToESGFxConverter();

		featureModel = MXEFileToESGFxConverter.parseFeatureModel(featureModelFilePath);

		System.out.println(featureModel.toString());

		ESGFx = MXEFileToESGFxConverter.parseMXEFileForESGFxCreation(ESGFxFilePath);

		featureExpressionMapFromFeatureModel = MXEFileToESGFxConverter.getFeatureExpressionMap();

		return featureExpressionMapFromFeatureModel;
	}

	protected static boolean isProductConfigurationValid(FeatureModel featureModel,
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {

		ProductConfigurationValidator productConfigurationValidator = new ProductConfigurationValidator();
		boolean isValid = productConfigurationValidator.validate(featureModel, featureExpressionMapFromFeatureModel);
		return isValid;
	}

	/*
	 * This method puts feature expression objects into an array list starting from
	 * index 0 to use the indices as the variable in SAT problem
	 */
	protected List<FeatureExpression> getFeatureExpressionList(
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {

		Set<Entry<String, FeatureExpression>> entrySet = featureExpressionMapFromFeatureModel.entrySet();
		Iterator<Entry<String, FeatureExpression>> entrySetIterator = entrySet.iterator();
		List<FeatureExpression> featureExpressionList = new ArrayList<FeatureExpression>(
				featureExpressionMapFromFeatureModel.size() + 1);

		int index = 0;
		while (entrySetIterator.hasNext()) {

			Entry<String, FeatureExpression> entry = entrySetIterator.next();
			String featureName = entry.getKey();
			FeatureExpression featureExpression = entry.getValue();
			if (!featureName.contains("!") && !(featureExpression == null)
					&& !(featureExpression.getFeature().getName() == null)) {
				featureExpressionList.add(index, featureExpression);
//				System.out.println(featureName + " - " + (index));
				index++;
			}

		}
//		System.out.println("------------------------------");

		return featureExpressionList;
	}

	protected void printFeatureExpressionList(List<FeatureExpression> featureExpressionList) {

		Iterator<FeatureExpression> featureExpressionListIterator = featureExpressionList.iterator();

		while (featureExpressionListIterator.hasNext()) {
			FeatureExpression featureExpression = featureExpressionListIterator.next();
			int index = featureExpressionList.indexOf(featureExpression);
			System.out.println(featureExpression.getFeature().getName() + " - " + (index + 1));

		}

	}

	protected void printFeatureExpressionMapFromFeatureModel(
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {
		for (Map.Entry<String, FeatureExpression> entry : featureExpressionMapFromFeatureModel.entrySet()) {
			String featureName = entry.getKey();
			FeatureExpression featureExpression = entry.getValue();
			System.out.print(featureName + " - " + featureExpression + "\n");
		}
		System.out.println("-----------------------------");
	}

}
