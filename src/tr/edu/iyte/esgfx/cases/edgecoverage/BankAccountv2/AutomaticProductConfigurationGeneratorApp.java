package tr.edu.iyte.esgfx.cases.edgecoverage.BankAccountv2;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import java.util.Map.Entry;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.conversion.mxe.MXEFileToESGFxConverter;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featuremodel.Feature;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;
import tr.edu.iyte.esgfx.productconfigurationgeneration.AutomaticProductConfigurationGenerator;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EdgeCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EulerCycleGeneratorForEdgeCoverage;
import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;



public class AutomaticProductConfigurationGeneratorApp extends CaseStudyUtilities_BAv2 {
	
	private static Set<String> f4 = new LinkedHashSet<String>();
	private static Set<String> f5 = new LinkedHashSet<String>();
	private static Set<String> f6 = new LinkedHashSet<String>();
	private static Set<String> f7 = new LinkedHashSet<String>();
	private static Set<String> f8 = new LinkedHashSet<String>();
	private static Set<String> f9 = new LinkedHashSet<String>();
	private static Set<String> f10 = new LinkedHashSet<String>();
	private static Set<String> f11 = new LinkedHashSet<String>();
	private static Set<String> f12 = new LinkedHashSet<String>();

	

	public static void main(String[] args) {

		MXEFileToESGFxConverter MXEFileToESGFxConverter = new MXEFileToESGFxConverter();
		FeatureModel featureModel = null;
		try {
			featureModel = MXEFileToESGFxConverter.parseFeatureModel(featureModelFilePath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Feature Model:" + featureModel);
		System.out.println("-----------------------------------------------");
		
		Set<Feature> featureSet = featureModel.getFeatureSet();
		for (Feature feature : featureSet) {
			System.out.println(feature.getName());
		}
		System.out.println("-----------------------------------------------");

		ESG ESGFx = null;
		try {
			ESGFx = MXEFileToESGFxConverter.parseMXEFileForESGFxCreation(ESGFxFilePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Map<String, FeatureExpression> featureExpressionMapFromFeatureModel = MXEFileToESGFxConverter
				.getFeatureExpressionMap();

		for (Map.Entry<String, FeatureExpression> entry : featureExpressionMapFromFeatureModel.entrySet()) {
			String key = entry.getKey();
			FeatureExpression value = entry.getValue();
			Feature feature = value.getFeature();
			System.out.println(key + " - " + value.evaluate() + " - " + feature.getName());
		}
		System.out.println("-----------------------------------------------");

		AutomaticProductConfigurationGenerator automaticProductConfigurationGenerator = new AutomaticProductConfigurationGenerator();
		Set<Map<String, FeatureExpression>> setOfFeatureExpressionMaps = null;
		try {
			setOfFeatureExpressionMaps = automaticProductConfigurationGenerator
					.getAllProductConfigurations(featureModel, featureExpressionMapFromFeatureModel);
		} catch (ContradictionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Iterator<Map<String, FeatureExpression>> setOfFeatureExpressionMapsIterator = setOfFeatureExpressionMaps
				.iterator();

		int productID = 0;
		while (setOfFeatureExpressionMapsIterator.hasNext()) {
			Map<String, FeatureExpression> productConfigurationMap = setOfFeatureExpressionMapsIterator.next();
			automaticProductConfigurationGenerator.matchFeatureExpressions(featureExpressionMapFromFeatureModel,
					productConfigurationMap);

			String productName = "P";
			if (productID < 10)
				productName = "P0";
			
			boolean isProductConfigurationValid = false;
			try {
				isProductConfigurationValid = isProductConfigurationValid(featureModel,featureExpressionMapFromFeatureModel);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		

			if (isProductConfigurationValid) {
				productID++;
				String ESGFxName = productName + Integer.toString(productID);

				String productConfiguration = ESGFxName + ": <";
				int numberOfFeatures = 0;
				for (Entry<String, FeatureExpression> entry : featureExpressionMapFromFeatureModel.entrySet()) {
//					System.out.print(entry.getKey() + " - " + entry.getValue().evaluate() + "\n");
					if (entry.getValue().evaluate() == true) {
						productConfiguration += entry.getKey() + ", ";
						numberOfFeatures++;
					}
				}
				productConfiguration = productConfiguration.substring(0, productConfiguration.length() - 2);
				productConfiguration += ">";
//				System.out.println(productConfiguration);
				categorizeProductConfiguration(productConfiguration, numberOfFeatures);

				ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();
				ESG productESGFx = productESGFxGenerator.generateProductESGFx(productID, ESGFxName, ESGFx);

//				System.out.println(productESGFx.toString());

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
//				edgeCoverageAnalyser.esgEventSequenceSetPrinter(CESsOfESG);

				double coverage = edgeCoverageAnalyser.analyseEdgeCoverage(stronglyConnectedBalancedESGFx, CESsOfESG,
						productConfigurationMap);
				
				if(coverage < 100) {
					System.out.println(productESGFx.toString());
					edgeCoverageAnalyser.esgEventSequenceSetPrinter(CESsOfESG);
					System.out.println("Edge coverage: %" + coverage);
					System.out.println("-----------------------------");
				}
				

			}
			else {
				System.out.println("Product configuration is not valid");
				break;
			}
		}
		

		printProductSet(f4, 4);
		printProductSet(f5, 5);
		printProductSet(f6, 6);
		printProductSet(f7, 7);
		printProductSet(f8, 8);
		printProductSet(f9, 9);
		printProductSet(f10, 10);
		printProductSet(f11, 11);
		printProductSet(f12,12);
		System.out.println("Total Number of products" + totalNumberOfProducts());

	}
	
	private static void printProductSet(Set<String> productSet, int numberOfFeatures) {
		System.out.println("Number of products with "+ numberOfFeatures + " features:"+ productSet.size());
        for (String product : productSet) {
            System.out.println(product);
        }
	}
		
	private static int totalNumberOfProducts() {
		return f4.size() + f5.size() + f6.size() + f7.size() + f8.size() + f9.size() + f10.size() + f11.size() + f12.size();
	}
	
	private static void categorizeProductConfiguration(String productConfiguration, int numberOfFeatures) {
		switch (numberOfFeatures) {
		case 4:
			f4.add(productConfiguration);
			break;
		case 5:
			f5.add(productConfiguration);
			break;
		case 6:
			f6.add(productConfiguration);
			break;
		case 7:
			f7.add(productConfiguration);
			break;
		case 8:
			f8.add(productConfiguration);
			break;
		case 9:
			f9.add(productConfiguration);
			break;
		case 10:
			f10.add(productConfiguration);
			break;
		case 11:
			f11.add(productConfiguration);
			break;
		case 12:
			f12.add(productConfiguration);
			break;
		}
	}

}
