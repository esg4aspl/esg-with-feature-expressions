package tr.edu.iyte.esgfx.cases.edgecoverage.StudentAttendanceSystem;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import java.util.Map.Entry;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.conversion.mxe.MXEFileToESGFxConverter;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;
import tr.edu.iyte.esgfx.productconfigurationgeneration.AutomaticProductConfigurationGenerator;

import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EdgeCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EulerCycleGeneratorForEdgeCoverage;
import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;

public class AutomaticProductConfigurationGeneratorApp extends CaseStudyUtilities_SAS {

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
		
//		Set<Feature> featureSet = featureModel.getFeatureSet();
//		for (Feature feature : featureSet) {
//			System.out.println(feature.getName());
//		}
//		System.out.println("-----------------------------------------------");

		ESG ESGFx = null;
		try {
			ESGFx = MXEFileToESGFxConverter.parseMXEFileForESGFxCreation(ESGFxFilePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Map<String, FeatureExpression> featureExpressionMapFromFeatureModel = MXEFileToESGFxConverter
				.getFeatureExpressionMap();

//		for (Map.Entry<String, FeatureExpression> entry : featureExpressionMapFromFeatureModel.entrySet()) {
//			String key = entry.getKey();
//			FeatureExpression value = entry.getValue();
//			Feature feature = value.getFeature();
//			System.out.println(key + " - " + value.evaluate() + " - " + feature.getName());
//		}
//		System.out.println("-----------------------------------------------");

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
			
//			for (Map.Entry<String, FeatureExpression> entry : productConfigurationMap.entrySet()) {
//				String key = entry.getKey();
//				FeatureExpression value = entry.getValue();
//				Feature feature = value.getFeature();
//				System.out.println(key + " - " + value.evaluate());
//			}

			String productName = "P";
			if (productID < 10)
				productName = "P0";

			boolean isValid = isProductConfigurationValid(featureModel,featureExpressionMapFromFeatureModel);
			if (isValid) {
				
				

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


				double coverage = edgeCoverageAnalyser.analyseEdgeCoverage(stronglyConnectedBalancedESGFx, CESsOfESG,
						productConfigurationMap);
				
				if(coverage < 100) {
					System.out.println(productESGFx.toString());
					edgeCoverageAnalyser.esgEventSequenceSetPrinter(CESsOfESG);
					System.out.println("Edge coverage: %" + coverage);
					System.out.println("-----------------------------");
				}
				
				

			} else {
				System.out.println("Product configuration is not valid.");
				break;
			}
		}
	}

	private static boolean isProductConfigurationValid(
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {

		boolean atLeastOneUserAccessMethodIsSelected = featureExpressionMapFromFeatureModel.get("ta").evaluate()
				|| featureExpressionMapFromFeatureModel.get("sa").evaluate();

		boolean atLeastOneRecordInformationIsSelected = featureExpressionMapFromFeatureModel.get("vr").evaluate()
				|| featureExpressionMapFromFeatureModel.get("ur").evaluate()
				|| featureExpressionMapFromFeatureModel.get("mas").evaluate()
				|| featureExpressionMapFromFeatureModel.get("taa").evaluate();

		boolean atLeastOneClassManagementIsSelected = featureExpressionMapFromFeatureModel.get("vc").evaluate()
				|| featureExpressionMapFromFeatureModel.get("anc").evaluate()
				|| featureExpressionMapFromFeatureModel.get("ucd").evaluate()
				|| featureExpressionMapFromFeatureModel.get("dc").evaluate();

		boolean atLeastOneClassScheduleIsSelected = featureExpressionMapFromFeatureModel.get("vs").evaluate()
				|| featureExpressionMapFromFeatureModel.get("ans").evaluate()
				|| featureExpressionMapFromFeatureModel.get("es").evaluate()
				|| featureExpressionMapFromFeatureModel.get("ass").evaluate();

		boolean atLeastOneAttendanceMethodIsSelected = featureExpressionMapFromFeatureModel.get("a").evaluate()
				|| featureExpressionMapFromFeatureModel.get("b").evaluate()
				|| featureExpressionMapFromFeatureModel.get("f").evaluate()
				|| featureExpressionMapFromFeatureModel.get("q").evaluate();

		boolean onlyOneAttendanceMethodIsSelected = (featureExpressionMapFromFeatureModel.get("a").evaluate()
				&& !featureExpressionMapFromFeatureModel.get("b").evaluate()
				&& !featureExpressionMapFromFeatureModel.get("f").evaluate()
				&& !featureExpressionMapFromFeatureModel.get("q").evaluate())

				|| (!featureExpressionMapFromFeatureModel.get("a").evaluate()
						&& featureExpressionMapFromFeatureModel.get("b").evaluate()
						&& !featureExpressionMapFromFeatureModel.get("f").evaluate()
						&& !featureExpressionMapFromFeatureModel.get("q").evaluate())
				|| (!featureExpressionMapFromFeatureModel.get("a").evaluate()
						&& !featureExpressionMapFromFeatureModel.get("b").evaluate()
						&& featureExpressionMapFromFeatureModel.get("f").evaluate()
						&& !featureExpressionMapFromFeatureModel.get("q").evaluate())
				|| (!featureExpressionMapFromFeatureModel.get("a").evaluate()
						&& !featureExpressionMapFromFeatureModel.get("b").evaluate()
						&& !featureExpressionMapFromFeatureModel.get("f").evaluate()
						&& featureExpressionMapFromFeatureModel.get("q").evaluate());

		boolean atLeastOneNotificationSelected = featureExpressionMapFromFeatureModel.get("e").evaluate()
				|| featureExpressionMapFromFeatureModel.get("s").evaluate();

		boolean onlyOneNotificationSelected = (featureExpressionMapFromFeatureModel.get("e").evaluate()
				&& !featureExpressionMapFromFeatureModel.get("s").evaluate())
				|| (!featureExpressionMapFromFeatureModel.get("e").evaluate()
						&& featureExpressionMapFromFeatureModel.get("s").evaluate());

		return atLeastOneUserAccessMethodIsSelected && atLeastOneRecordInformationIsSelected
				&& atLeastOneClassManagementIsSelected && atLeastOneClassScheduleIsSelected
				&& atLeastOneAttendanceMethodIsSelected && onlyOneAttendanceMethodIsSelected
				&& atLeastOneNotificationSelected && onlyOneNotificationSelected;

	}
	

}
