package tr.edu.iyte.esgfx.cases.edgecoverage.BankAccountv2;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.conversion.mxe.MXEFileToESGFxConverter;

import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;

import tr.edu.iyte.esgfx.testgeneration.TestSuiteFileWriter;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EdgeCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EulerCycleGeneratorForEdgeCoverage;

import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;

public class OLD_CaseStudyApp_EdgeCoverage_TestSequenceRecording_BAv2 extends CaseStudyUtilities_BAv2 {

	public static void main(String[] args) throws Exception {

		testsequencesFolderPath += "edgecoverage/";
		timemeasurementFolderPath += "edgecoverage/";

		MXEFileToESGFxConverter MXEFileToESGFxConverter = new MXEFileToESGFxConverter();
		FeatureModel featureModel = MXEFileToESGFxConverter.parseFeatureModel(featureModelFilePath);
//		System.out.println(featureModel.toString());

		ESG ESGFx = null;
		try {
			ESGFx = MXEFileToESGFxConverter.parseMXEFileForESGFxCreation(ESGFxFilePath);

		} catch (Exception e) {
			e.printStackTrace();
		}
		Map<String, FeatureExpression> featureExpressionMapFromFeatureModel = MXEFileToESGFxConverter
				.getFeatureExpressionMap();

//		for (Entry<String, FeatureExpression> entry : featureExpressionMapFromFeatureModel.entrySet()) {
//			String featureName = entry.getKey();
//			FeatureExpression featureExpression = entry.getValue();
//			System.out.print(featureName + " - " + featureExpression + "\n");
//		}
//		System.out.println("-----------------------------");

		Set<Map<String, FeatureExpression>> setOfFeatureExpressionMaps = OLD_AutomaticProductConfiguration_BAv2
				.getAllProductConfigurations(featureExpressionMapFromFeatureModel);
		Iterator<Map<String, FeatureExpression>> setOfFeatureExpressionMapsIterator = setOfFeatureExpressionMaps
				.iterator();

		int productID = 0;
		while (setOfFeatureExpressionMapsIterator.hasNext()) {
			Map<String, FeatureExpression> productConfigurationMap = setOfFeatureExpressionMapsIterator.next();
			OLD_AutomaticProductConfiguration_BAv2.matchFeatureExpressions(featureExpressionMapFromFeatureModel,
					productConfigurationMap);

			String productName = "P";
			if (productID < 10)
				productName = "P0";

			boolean allMandatoryFeatruesSelected = featureExpressionMapFromFeatureModel.get("b").evaluate()
					&& featureExpressionMapFromFeatureModel.get("d").evaluate()
					&& featureExpressionMapFromFeatureModel.get("w").evaluate();

			System.out.println("allMandatoryFeatruesSelected " + allMandatoryFeatruesSelected );
			boolean atLeastOneCurrencyIsSelected = featureExpressionMapFromFeatureModel.get("tl").evaluate()
					|| featureExpressionMapFromFeatureModel.get("eu").evaluate()
					|| featureExpressionMapFromFeatureModel.get("us").evaluate();

			boolean onlyOneCurrencyIsSelected = (featureExpressionMapFromFeatureModel.get("tl").evaluate()
					&& !featureExpressionMapFromFeatureModel.get("eu").evaluate()
					&& !featureExpressionMapFromFeatureModel.get("us").evaluate())
					|| (!featureExpressionMapFromFeatureModel.get("tl").evaluate()
							&& featureExpressionMapFromFeatureModel.get("eu").evaluate()
							&& !featureExpressionMapFromFeatureModel.get("us").evaluate())
					|| (!featureExpressionMapFromFeatureModel.get("tl").evaluate()
							&& !featureExpressionMapFromFeatureModel.get("eu").evaluate()
							&& featureExpressionMapFromFeatureModel.get("us").evaluate());

			System.out.println("atLeastOneCurrencyIsSelected " + atLeastOneCurrencyIsSelected);
			System.out.println("onlyOneCurrencyIsSelected " + onlyOneCurrencyIsSelected);
//			if (allMandatoryFeatruesSelected && atLeastOneCurrencyIsSelected && onlyOneCurrencyIsSelected) {
				
				productID++;
				String ESGFxName = productName + Integer.toString(productID);
				
				String productConfiguration = ESGFxName + ": <";
				for (Entry<String, FeatureExpression> entry : featureExpressionMapFromFeatureModel.entrySet()) {
//					System.out.print(entry.getKey() + " - " + entry.getValue().evaluate() + "\n");
					if (entry.getValue().evaluate() == true)
						productConfiguration += entry.getKey() + ", ";
				}
				productConfiguration = productConfiguration.substring(0, productConfiguration.length() - 2);
				productConfiguration += ">";
				System.out.println(productConfiguration);
				
				ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();
				ESG productESGFx = productESGFxGenerator.generateProductESGFx(productID, ESGFxName, ESGFx);

//			System.out.println(productESGFx.toString());

				ESG stronglyConnectedBalancedESGFx = StronglyConnectedBalancedESGFxGeneration
						.getStronglyConnectedBalancedESGFxGeneration(productESGFx);
//			 System.out.println(ESGFx);

				EulerCycleGeneratorForEdgeCoverage eulerCycleGeneratorForEdgeCoverage = new EulerCycleGeneratorForEdgeCoverage();
				eulerCycleGeneratorForEdgeCoverage.generateEulerCycle(stronglyConnectedBalancedESGFx);
				List<Vertex> eulerCycle = eulerCycleGeneratorForEdgeCoverage.getEulerCycle();
//			System.out.println("Euler Cycle: " + eulerCycle);
				EulerCycleToTestSequenceGenerator eulerCycleToTestSequenceGenerator = new EulerCycleToTestSequenceGenerator();
				Set<EventSequence> CESsOfESG = eulerCycleToTestSequenceGenerator.CESgenerator(eulerCycle);

				EdgeCoverageAnalyser edgeCoverageAnalyser = new EdgeCoverageAnalyser();
				edgeCoverageAnalyser.esgEventSequenceSetPrinter(CESsOfESG);

				double coverage = edgeCoverageAnalyser.analyseEdgeCoverage(stronglyConnectedBalancedESGFx, CESsOfESG,
						productConfigurationMap);
				System.out.println("Edge coverage: %" + coverage);
				System.out.println("-----------------------------");

//				TestSuiteFileWriter.writeEventSequenceSetAndEdgeCoverageAnalysisToFile(testsequencesFolderPath + "BA_EdgeCoverage.txt", productConfiguration, CESsOfESG, coverage);
//			} else {
//				System.out.println("Product configuration is not valid for the Bank Account v2 case study.");
//				continue;
//
//			}

		}

//		System.out.println("Test sequences are recorded in the file: " + testsequencesFolderPath + "BA_EdgeCoverage.txt");
	}
}
