package tr.edu.iyte.esgfx.cases.edgecoverage.BankAccountv2;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.cases.eventcoverage.BankAccount.CaseStudyUtilities_BA;
import tr.edu.iyte.esgfx.conversion.mxe.FeatureESGSetGenerator;
import tr.edu.iyte.esgfx.conversion.mxe.MXEFileToESGFxConverter;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;
import tr.edu.iyte.esgfx.mutationtesting.faultdetection.FaultDetector;

import tr.edu.iyte.esgfx.mutationtesting.mutationoperators.FeatureInserter;
import tr.edu.iyte.esgfx.mutationtesting.mutationoperators.FeatureOmitter;
import tr.edu.iyte.esgfx.mutationtesting.mutationoperators.MutationOperator;
import tr.edu.iyte.esgfx.mutationtesting.resultutils.FaultDetectionResultRecorder;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;

import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EulerCycleGeneratorForEdgeCoverage;

import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;

public class OLD_CaseStudyApp_FeatureInserterExperimental2_BAv2 extends CaseStudyUtilities_BAv2 {

	public static void main(String[] args) throws Exception {

		testsequencesFolderPath += "edgecoverage/";
		timemeasurementFolderPath += "edgecoverage/";

		int productID = 1;

		String productName = "P";
		if (productID < 10)
			productName = "P0";

		String ESGFxName = productName + Integer.toString(productID);

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
		
//		CaseStudyUtilities_BAv2.configureProduct(productID, featureExpressionMapFromFeatureModel);

		String productConfiguration = ESGFxName + ": <";
		for (Entry<String, FeatureExpression> entry : featureExpressionMapFromFeatureModel.entrySet()) {
//					System.out.print(entry.getKey() + " - " + entry.getValue().evaluate() + "\n");
			if (entry.getValue().evaluate() == true)
				productConfiguration += entry.getKey() + ", ";
		}
		productConfiguration = productConfiguration.substring(0, productConfiguration.length() - 2);
		productConfiguration += ">";
		System.out.println("Product Configuration: " + productConfiguration);
//		System.out.println("Product ID: " + productID);

		ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();
		ESG productESGFx = productESGFxGenerator.generateProductESGFx(productID, ESGFxName, ESGFx);

		ESG stronglyConnectedBalancedESGFx = StronglyConnectedBalancedESGFxGeneration
				.getStronglyConnectedBalancedESGFxGeneration(productESGFx);
		EulerCycleGeneratorForEdgeCoverage eulerCycleGeneratorForEdgeCoverage = new EulerCycleGeneratorForEdgeCoverage();
		eulerCycleGeneratorForEdgeCoverage.generateEulerCycle(stronglyConnectedBalancedESGFx);
		List<Vertex> eulerCycle = eulerCycleGeneratorForEdgeCoverage.getEulerCycle();
//			System.out.println("Euler Cycle: " + eulerCycle);
		EulerCycleToTestSequenceGenerator eulerCycleToTestSequenceGenerator = new EulerCycleToTestSequenceGenerator();
		Set<EventSequence> CESsOfESG = eulerCycleToTestSequenceGenerator.CESgenerator(eulerCycle);

		Set<ESG> featureESGSet = FeatureESGSetGenerator
				.createFeatureESGSet("files/Cases/BankAccountv2/featureESGModels");

		MutationOperator mutationOperator = new FeatureOmitter();
		((FeatureOmitter) mutationOperator).setFeatureESGSet(featureESGSet);
		((FeatureOmitter) mutationOperator).setFeatureExpressionMap(featureExpressionMapFromFeatureModel);

		mutationOperator.generateMutantESGFxSets(productESGFx);
		mutationOperator.reportNumberOfMutants();

		Set<ESG> mutantESGFxSet = mutationOperator.getValidMutantESGFxSet();
		mutantESGFxSet.addAll(mutationOperator.getInvalidMutantESGFxSet());

		int totalFaultCount = 0;
		int validMutantFaultCount = 0;
		int invalidMutantFaultCount = 0;
		for (Entry<String, ESG> entry : ((FeatureOmitter) mutationOperator).getFeatureNameMutantMap().entrySet()) {
			FaultDetector faultDetector = new FaultDetector(CESsOfESG);
			String mutationElement = entry.getKey();
			ESG mutantESGFx = entry.getValue();
			int mutantID = ((ESGFx) mutantESGFx).getID();
					System.out.println("Feature: " + mutationElement + " Mutant: " + mutantID);

			boolean isFaultDetected = faultDetector.isFaultDetected(mutantESGFx);
			if (isFaultDetected) {
				totalFaultCount++;
				if (mutationOperator.getValidMutantESGFxSet().contains(mutantESGFx)) {
					validMutantFaultCount++;
				} else {
					invalidMutantFaultCount++;
				}
			}
					System.out.println(" isFaultDetected: " + isFaultDetected);
					System.out.println();

//			boolean isMutantValid = mutationOperator.getValidMutantESGFxSet().contains(mutantESGFx);
//			FaultDetectionResultRecorder.writeDetailedFaultDetectionResult(
//					detailedFaultDetectionResults + "_FeatureInserter", productID, productConfiguration,
//					mutationOperator.getName(), mutationElement, mutantID, isMutantValid, isFaultDetected);
		}

//		FaultDetectionResultRecorder.writeFaultDetectionResultsForSPL(faultDetectionResultsForSPL,
//				mutationOperator.getName(), productID, mutationOperator.getValidMutantESGFxSet().size(),
//				mutationOperator.getInvalidMutantESGFxSet().size(), validMutantFaultCount, invalidMutantFaultCount,
//				mutantESGFxSet.size(), totalFaultCount);
//				System.out.println("Mutant count: " + mutantESGFxSet.size());
//				System.out.println("Fault count: " + totalFaultCount);

	}
}
