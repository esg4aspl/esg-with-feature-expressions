package tr.edu.iyte.esgfx.cases.eventcoverage.BankAccount;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.cases.resultrecordingutilities.TestSequenceGenerationTimeMeasurementWriter;
import tr.edu.iyte.esgfx.conversion.mxe.MXEFileToESGFxConverter;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;
import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;
import tr.edu.iyte.esgfx.testgeneration.TestSequenceSelectionBasedOnProductConfiguration;
import tr.edu.iyte.esgfx.testgeneration.TestSuiteFileWriter;
import tr.edu.iyte.esgfx.testgeneration.eventcoverage.EulerCycleGeneratorForEventCoverage;
import tr.edu.iyte.esgfx.testgeneration.eventcoverage.EventCoverageAnalyser;

public class CaseStudyApp_EventCoverage_BA extends CaseStudyUtilities_BA {

	public static void main(String[] args) throws Exception {

		testsequencesFolderPath += "eventcoverage/";
		timemeasurementFolderPath += "eventcoverage/";

		int productID = 24;

		String productName = "P";
		if (productID < 10)
			productName = "P0";

		String ESGFxName = productName + Integer.toString(productID);

		MXEFileToESGFxConverter MXEFileToESGFxConverter = new MXEFileToESGFxConverter();
		MXEFileToESGFxConverter.parseFeatureModel(featureModelFilePath);

		ESG ESG = null;
		try {
			ESG = MXEFileToESGFxConverter.parseMXEFileForESGFxCreation(ESGFxFilePath);

		} catch (Exception e) {
			e.printStackTrace();
		}

		Map<String, FeatureExpression> featureExpressionMap = MXEFileToESGFxConverter.getFeatureExpressionMap();
		double startTime1 = System.nanoTime();
		CaseStudyUtilities_BA.configureProduct(productID, featureExpressionMap);
		System.out.println("Product Configuration: ");
		for (Entry<String, FeatureExpression> entry : featureExpressionMap.entrySet()) {
			System.out.print(entry.getKey() + " - " + entry.getValue().evaluate() + "\n");
		}

		ESG ESGFx = StronglyConnectedBalancedESGFxGeneration.getStronglyConnectedBalancedESGFxGeneration(ESG);
//		System.out.println(ESGFx);

		EulerCycleGeneratorForEventCoverage eulerCycleGeneratorForEventCoverage = new EulerCycleGeneratorForEventCoverage(
				featureExpressionMap);
		eulerCycleGeneratorForEventCoverage.generateEulerCycle(ESGFx);
		List<Vertex> eulerCycle = eulerCycleGeneratorForEventCoverage.getEulerCycle();

		EulerCycleToTestSequenceGenerator eulerCycleToTestSequenceGenerator = new EulerCycleToTestSequenceGenerator();
		eulerCycleToTestSequenceGenerator.CESgenerator(eulerCycle);

		CaseStudyUtilities_BA.configureProduct(productID, featureExpressionMap);

		TestSequenceSelectionBasedOnProductConfiguration TestSequenceSelectionBasedOnProductConfiguration = new TestSequenceSelectionBasedOnProductConfiguration();

		Set<EventSequence> CESsOfESG = TestSequenceSelectionBasedOnProductConfiguration
				.selectTestSequences(eulerCycleToTestSequenceGenerator.getEventSequenceFeatureExpressionMap());

		double stopTime1 = System.nanoTime();
		double timeElapsed1 = (stopTime1 - startTime1) / (double) 1000000;

		EventCoverageAnalyser eventCoverageAnalyser = new EventCoverageAnalyser();
		eventCoverageAnalyser.esgEventSequenceSetPrinter(CESsOfESG);

		System.out.println("Execution time of test product test sequence generation in miliseconds  : " + timeElapsed1);

		double coverage = eventCoverageAnalyser.analyseEventCoverage(ESG, CESsOfESG, featureExpressionMap);
		System.out.println("Event coverage: %" + coverage);

		TestSuiteFileWriter.writeEventSequenceSetAndEventCoverageAnalysisToFile(
				testsequencesFolderPath + ESGFxName + ".txt", CESsOfESG, coverage);

		TestSequenceGenerationTimeMeasurementWriter.writeTimeMeasurement(timeElapsed1, timemeasurementFolderPath,
				ESGFxName);

	}

}