package tr.edu.iyte.esgfx.cases.SVM;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.cases.resultrecordingutilities.TestSequenceGenerationTimeMeasurementWriter;
import tr.edu.iyte.esgfx.conversion.mxe.MXEFileToESGFxConverter;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleGeneratorForEventCoverage;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;
import tr.edu.iyte.esgfx.testgeneration.TestSequenceSelectionBasedOnProductConfiguration;
import tr.edu.iyte.esgfx.testgeneration.TestSuiteFileWriter;
import tr.edu.iyte.esgfx.testgeneration.eventcoverage.EventCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;

public class CaseStudyApp_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws IOException {

		int productID = 12;
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
		CaseStudyUtilities_SVM.configureProduct(productID, featureExpressionMap);

		ESG ESGFx = StronglyConnectedBalancedESGFxGeneration.getStronglyConnectedBalancedESGFxGeneration(ESG);
//		System.out.println(ESGFx);

		EulerCycleGeneratorForEventCoverage eulerCycleGeneratorForEventCoverage = new EulerCycleGeneratorForEventCoverage(
				featureExpressionMap);
		eulerCycleGeneratorForEventCoverage.generateEulerCycle(ESGFx);
		List<Vertex> eulerCycle = eulerCycleGeneratorForEventCoverage.getEulerCycle();

		EulerCycleToTestSequenceGenerator eulerCycleToTestSequenceGenerator = new EulerCycleToTestSequenceGenerator();
		eulerCycleToTestSequenceGenerator.CESgenerator(eulerCycle);

		TestSequenceSelectionBasedOnProductConfiguration TestSequenceSelectionBasedOnProductConfiguration = new TestSequenceSelectionBasedOnProductConfiguration();

		Set<EventSequence> CESsOfESG = TestSequenceSelectionBasedOnProductConfiguration
				.selectTestSequences(eulerCycleToTestSequenceGenerator.getEventSequenceFeatureExpressionMap());

		double stopTime1 = System.nanoTime();
		double timeElapsed1 = (stopTime1 - startTime1) / (double) 1000000;

		System.out.println("Product Configuration: ");
		for (Entry<String, FeatureExpression> entry : featureExpressionMap.entrySet()) {
			System.out.print(entry.getKey() + " - " + entry.getValue().evaluate() + "\n");
		}

		EventCoverageAnalyser eventCoverageAnalyser = new EventCoverageAnalyser();
		eventCoverageAnalyser.esgEventSequenceSetPrinter(CESsOfESG);

		System.out.println("Execution time of test product test sequence generation in miliseconds  : " + timeElapsed1);

		double coverage = eventCoverageAnalyser.analyseEventCoverage(ESG, CESsOfESG, featureExpressionMap);
		System.out.println("Event coverage: %" + coverage);

		TestSuiteFileWriter.writeEventSequenceSetAndCoverageAnalysisToFile(testsequencesFolderPath + ESGFxName + ".txt",
				CESsOfESG, coverage);

		TestSequenceGenerationTimeMeasurementWriter.writeTimeMeasurement(timeElapsed1, timemeasurementFolderPath,
				ESGFxName);
	}
}
