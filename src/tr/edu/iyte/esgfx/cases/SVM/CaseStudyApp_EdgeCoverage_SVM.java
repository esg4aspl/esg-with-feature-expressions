package tr.edu.iyte.esgfx.cases.SVM;

import java.util.List;
import java.util.Map;
import java.util.Set;


import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.cases.eventcoverage.SVM.CaseStudyUtilities_SVM;
import tr.edu.iyte.esgfx.cases.resultrecordingutilities.TestSequenceGenerationTimeMeasurementWriter;
import tr.edu.iyte.esgfx.conversion.mxe.MXEFileToESGFxConverter;

import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EdgeCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EulerCycleGeneratorForEdgeCoverage;

import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;

public class CaseStudyApp_EdgeCoverage_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {

		testsequencesFolderPath += "edgecoverage/";
		timemeasurementFolderPath += "edgecoverage/";

		int productID = 1;
		String productName = "P";
		if (productID < 10)
			productName = "P0";

		String ESGFxName = productName + Integer.toString(productID);

		MXEFileToESGFxConverter MXEFileToESGFxConverter = new MXEFileToESGFxConverter();
		MXEFileToESGFxConverter.parseFeatureModel(featureModelFilePath);

		ESG ESGFx = null;
		try {
			ESGFx = MXEFileToESGFxConverter.parseMXEFileForESGFxCreation(ESGFxFilePath);

		} catch (Exception e) {
			e.printStackTrace();
		}

		Map<String, FeatureExpression> featureExpressionMap = MXEFileToESGFxConverter.getFeatureExpressionMap();

		double startTime1 = System.nanoTime();
		CaseStudyUtilities_SVM.configureProduct(productID, featureExpressionMap);
		
		ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();
		ESG productESGFx = productESGFxGenerator.generateProductESGFx(productID, ESGFxName, ESGFx);
//		System.out.println(productESGFx.toString());

		ESG stronglyConnectedBalancedESGFx = StronglyConnectedBalancedESGFxGeneration
				.getStronglyConnectedBalancedESGFxGeneration(productESGFx);
		// System.out.println(ESGFx);

		EulerCycleGeneratorForEdgeCoverage eulerCycleGeneratorForEdgeCoverage = new EulerCycleGeneratorForEdgeCoverage();
		eulerCycleGeneratorForEdgeCoverage.generateEulerCycle(stronglyConnectedBalancedESGFx);
		List<Vertex> eulerCycle = eulerCycleGeneratorForEdgeCoverage.getEulerCycle();
//			System.out.println("Euler Cycle: " + eulerCycle);
		EulerCycleToTestSequenceGenerator eulerCycleToTestSequenceGenerator = new EulerCycleToTestSequenceGenerator();
		Set<EventSequence> CESsOfESG = eulerCycleToTestSequenceGenerator.CESgenerator(eulerCycle);
		System.out.println("Number of test sequences: " + CESsOfESG);

		EdgeCoverageAnalyser edgeCoverageAnalyser = new EdgeCoverageAnalyser();
		edgeCoverageAnalyser.esgEventSequenceSetPrinter(CESsOfESG);

		double coverage = edgeCoverageAnalyser.analyseEdgeCoverage(stronglyConnectedBalancedESGFx, CESsOfESG,
				featureExpressionMap);
		System.out.println("Edge coverage: %" + coverage);
		System.out.println("-----------------------------");

		double stopTime1 = System.nanoTime();
		double timeElapsed1 = (stopTime1 - startTime1) / (double) 1000000;
		System.out.println(
				"Execution time of " + ESGFxName + " test sequence generation in miliseconds  : " + timeElapsed1);
		TestSequenceGenerationTimeMeasurementWriter.writeTimeMeasurement(timeElapsed1, timemeasurementFolderPath,
				ESGFxName);
	}

}
