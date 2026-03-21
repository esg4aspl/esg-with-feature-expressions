package tr.edu.iyte.esgfx.cases;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
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

	protected FeatureModel featureModel;
	protected ESG ESGFx;
	protected Map<String, FeatureExpression> featureExpressionMapFromFeatureModel;

	protected static String SPLName;
	protected static String caseStudyFolder;
	protected static String featureModelFile;
	protected static String ESGFxFile;

	protected static String productConfigurationFolder;
	protected static String productConfigurationFile;

	// Paths for DIMACS, mapping and samples file
	protected static String DIMACSFile;
	protected static String DIMACSMappingFile;
	protected static String SamplesFile;

	// Paths for Test Generation for ESG-Fx (Event Coverage, Event Couple Coverage,
	// Event Triple Coverage, Event Quadruple Coverage)
	protected static String testsequencesFolder;
	protected static String testSuiteFile;
	protected static String TestSuiteSummaryESGFx;

	// Paths for Test Generation and Execution Pipelines
	// for ESG-Fx (Random Walk, Event Coverage, Event Couple Coverage, Event Triple
	// Coverage, Event Quadruple Coverage)
	protected static String comparativeEfficiencyTestPipelineMeasurementFolder;
	protected static String extremeScalabilityTestPipelineMeasurementFolder;
	protected static String coverageType;
	protected static int coverageLength;

	protected static String DOTFolder;

	// Paths for Mutation Testing for ESG-Fx
	// (Random Walk, Event Coverage, Event Couple Coverage, Event Triple Coverage,
	// Event Quadruple Coverage)
	protected static String faultDetectionFolder;
	protected static String detailedFaultDetectionResults;
	protected static String faultDetectionResultsForPerProductInSPL;
	protected static String FaultDetectionSummaryESGFx;

	// Paths for Test Generation and Execution Pipelines
	// for EFG L=2,3,4
	protected static String EFGFolder;
	protected static String efg_testsequencesFolder;
	protected static String efg_resultsFolder;
	protected static String efg_mutantgenerator_edgeomitterFolder;
	protected static String efg_mutantgenerator_eventomitterFolder;
	protected static String TestSuiteSummaryEFG;
	protected static String FaultDetectionSummaryEFG;

	// Paths for Mutation Testing
	protected static Set<ESG> featureESGSet;
	protected static String mutationOperatorName;
	protected static String mutantEventName;
	protected static String mutantFeatureName;
	protected static String featureESGSetFolder_FeatureInsertion;
	protected static String featureESGSetFolder_FeatureOmission;

	//Path for test suite analysis
	protected static String testSequenceAnalysisFolder;

	private static void setSPLRelatedPaths() {

		featureModelFile = caseStudyFolder + "configs/model.xml";
		ESGFxFile = caseStudyFolder + SPLName + "_ESGFx.mxe";

		productConfigurationFolder = caseStudyFolder + "productConfigurations/";
		productConfigurationFile = caseStudyFolder + "productConfigurations_" + SPLName + ".txt";

		DIMACSFile = caseStudyFolder + "configs/" + SPLName + ".dimacs";
		DIMACSMappingFile = caseStudyFolder + "configs/" + SPLName + "_dimacsmapping.txt";
		SamplesFile = caseStudyFolder + "configs/" + SPLName + "_400.samples";

		testsequencesFolder = caseStudyFolder + "testsequences/";
		testSuiteFile = testsequencesFolder + SPLName + "_" + coverageType + ".txt";
		TestSuiteSummaryESGFx = "files/Cases/" + "TestSuiteSummaryESGFx.csv";

		comparativeEfficiencyTestPipelineMeasurementFolder = caseStudyFolder + "comparativeEfficiencyTestPipeline/";
		extremeScalabilityTestPipelineMeasurementFolder = caseStudyFolder + "extremeScalabilityTestPipeline/";

		faultDetectionFolder = caseStudyFolder + "faultdetection/";
		detailedFaultDetectionResults = faultDetectionFolder + SPLName + "_detailedFaultDetectionResults";
		faultDetectionResultsForPerProductInSPL = faultDetectionFolder + SPLName + "_faultDetectionResultsForSPL.csv";
		FaultDetectionSummaryESGFx = "files/Cases/" + "FaultDetectionSummaryESGFx.csv";

		EFGFolder = caseStudyFolder + "EFGs/";
		efg_testsequencesFolder = EFGFolder + "efg_testsequences/";
		efg_resultsFolder = EFGFolder + "efg_results/";
		efg_mutantgenerator_edgeomitterFolder = EFGFolder + "efg_mutantgenerator_edgeomitter/";
		efg_mutantgenerator_eventomitterFolder = EFGFolder + "efg_mutantgenerator_eventomitter/";

		DOTFolder = caseStudyFolder + "DOTs/";

		featureESGSetFolder_FeatureOmission = caseStudyFolder + "featureESGModels";
		featureESGSetFolder_FeatureInsertion = "files/Cases/SodaVendingMachinev2/featureESGModels";

		featureESGSet = new LinkedHashSet<>();

		testSequenceAnalysisFolder = caseStudyFolder + "/testsequenceanalysis";

		try {
			createDir(caseStudyFolder);
			createDir(productConfigurationFolder);
			createDir(testsequencesFolder);
			createDir(comparativeEfficiencyTestPipelineMeasurementFolder);
			createDir(extremeScalabilityTestPipelineMeasurementFolder);
			createDir(faultDetectionFolder);
			createDir(DOTFolder);
			createDir(EFGFolder);
			createDir(featureESGSetFolder_FeatureOmission);
			createDir(featureESGSetFolder_FeatureInsertion);
			createDir(efg_testsequencesFolder);
			createDir(efg_resultsFolder);
			createDir(efg_mutantgenerator_edgeomitterFolder);
			createDir(efg_mutantgenerator_eventomitterFolder);
			createDir(testSequenceAnalysisFolder);
		} catch (Exception e) {
			System.err.println("Warning: Could not create SPL directories: " + e.getMessage());
		}
	}

	public static void setCoverageType() {

		if (coverageLength == 0) {
			coverageType = "L0";
		} else if (coverageLength == 1) {
			coverageType = "L1";
		} else if (coverageLength == 2) {
			coverageType = "L2";
		} else if (coverageLength == 3) {
			coverageType = "L3";
		} else if (coverageLength == 4) {
			coverageType = "L4";
		} else {
			coverageType = "undetermined";
		}

		setSPLRelatedPaths();
	}

	// Helper method to create directory if it doesn't exist
	private static void createDir(String path) {
		File dir = new File(path);
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}

	protected Map<String, FeatureExpression> generateFeatureExpressionMapFromFeatureModel(String featureModelFilePath,
			String ESGFxFilePath) throws Exception {
		MXEFileToESGFxConverter MXEFileToESGFxConverter = new MXEFileToESGFxConverter();

		featureModel = MXEFileToESGFxConverter.parseFeatureModel(featureModelFilePath);

//        System.out.println(featureModel);

		ESGFx = MXEFileToESGFxConverter.parseMXEFileForESGFxCreation(ESGFxFilePath);
//        System.out.println("Number of vertices:" + ESGFx.getVertexList().size());
//        System.out.println("Number of edges:" + ESGFx.getEdgeList().size());

		featureExpressionMapFromFeatureModel = MXEFileToESGFxConverter.getFeatureExpressionMap();

//        printFeatureExpressionMapFromFeatureModel(featureExpressionMapFromFeatureModel);
		return featureExpressionMapFromFeatureModel;
	}

	protected static boolean isProductConfigurationValid(FeatureModel featureModel,
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {

		ProductConfigurationValidator productConfigurationValidator = new ProductConfigurationValidator();
		boolean isValid = productConfigurationValidator.validate(featureModel, featureExpressionMapFromFeatureModel);
		return isValid;
	}

	protected void updateFeatureExpressionMapFromConfigFile(String configFilePath) throws Exception {
		File configFile = new File(configFilePath);
		if (!configFile.exists()) {
			throw new Exception("Configuration file not found: " + configFilePath);
		}

		try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().isEmpty())
					continue;
				String[] parts = line.split("=");
				if (parts.length == 2) {
					String featureName = parts[0].trim();
					boolean truthValue = Boolean.parseBoolean(parts[1].trim());

					FeatureExpression fe = featureExpressionMapFromFeatureModel.get(featureName);
					if (fe != null) {
						fe.setTruthValue(truthValue);
					}
				}
			}
		}
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
//              System.out.println(featureName + " - " + (index));
				index++;
			}

		}
//      System.out.println("------------------------------");

		return featureExpressionList;
	}

	// Helper method to accurately calculate peak memory using MXBeans
	protected long getPeakHeapMemoryBytes() {
		long peakMemoryBytes = 0;
		for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
			//HEAP space
			if (pool.getType() == java.lang.management.MemoryType.HEAP) {
				MemoryUsage peakUsage = pool.getPeakUsage();
				if (peakUsage != null) {
					peakMemoryBytes += peakUsage.getUsed();
				}
			}
		}
		return peakMemoryBytes;
	}
	
	// Helper method to reset peak memory counters before a new measurement phase
	protected void resetPeakMemoryCounters() {
		for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
			pool.resetPeakUsage();
		}
	}

	protected void printFeatureExpressionList(List<FeatureExpression> featureExpressionList) {

		System.out.println("Feature Expression List");
		Iterator<FeatureExpression> featureExpressionListIterator = featureExpressionList.iterator();

		while (featureExpressionListIterator.hasNext()) {
			FeatureExpression featureExpression = featureExpressionListIterator.next();
			int index = featureExpressionList.indexOf(featureExpression);
			System.out.println(featureExpression.getFeature().getName() + " - " + (index + 1));

		}
		System.out.println("-----------------------------");

	}

	protected void printFeatureExpressionMapFromFeatureModel(
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {
		System.out.println("Feature Expression Map From Feature Model");
		for (Map.Entry<String, FeatureExpression> entry : featureExpressionMapFromFeatureModel.entrySet()) {
			String featureName = entry.getKey();
			FeatureExpression featureExpression = entry.getValue();
			System.out.print(featureName + " - " + featureExpression + ":" + featureExpression.evaluate() + "\n");
		}
		System.out.println("-----------------------------");

	}
}