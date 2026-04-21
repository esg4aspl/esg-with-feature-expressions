package tr.edu.iyte.esgfx.cases;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.cases.resultrecordingutilities.TestPipelineMeasurementWriter_ComparativeEfficiency;
import tr.edu.iyte.esgfx.conversion.dot.DOTFileToESGFxConverter;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;
import tr.edu.iyte.esgfx.testgeneration.TestSuiteFileWriter;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EdgeCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EulerCycleGeneratorForEdgeCoverage;
import tr.edu.iyte.esgfx.testgeneration.eventtriplecoverage.TransformedESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;
import tr.edu.iyte.esgfx.testexecution.TestExecutor;

public class RQ1_ComparativeEfficiency_ESGFx_L234 extends CaseStudyUtilities {

	public void measurePipelineForEdgeCoverage() throws Exception {
		int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
		int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));
		int L_LEVEL = Integer.parseInt(System.getenv().getOrDefault("L_LEVEL", "4"));
		int runID = Integer.parseInt(System.getenv().getOrDefault("runID", "1"));

		coverageLength = L_LEVEL;
		setCoverageType();

		System.out.println(
				"Test Generation And Execution Pipeline for ESG-Fx L=" + coverageLength + " " + SPLName + " STARTED");

		String dotDirectoryPath = DOTFolder + "L2/";
		File dotDirectory = new File(dotDirectoryPath);

		if (!dotDirectory.exists() || !dotDirectory.isDirectory()) {
			throw new Exception("DOT directory does not exist: " + dotDirectoryPath);
		}

		File[] dotFiles = dotDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".dot"));
		if (dotFiles == null || dotFiles.length == 0) {
			System.out.println("No DOT files found in directory.");
			return;
		}

		Arrays.sort(dotFiles, (f1, f2) -> f1.getName().compareTo(f2.getName()));

		String ESGFxPerProductLog = testsequencesFolder + coverageType + "/" + SPLName + "_ESG-FxPerProductLog_L"
				+ coverageLength + ".csv";

		if (N_SHARDS > 1) {
			ESGFxPerProductLog = testsequencesFolder + coverageType + "/" + String
					.format(SPLName + "_ESG-FxPerProductLog_shard%02d_%s.csv", CURRENT_SHARD, "L" + coverageLength);
		}

		long globalTotalTestGenTimeNanos = 0;
		long globalTotalTransformationTimeNanos = 0;
		long globalPeakMemoryGenBytes = 0;

		int globalTotalESGFxVertices = 0;
		int globalTotalESGFxEdges = 0;

		int globalTotalESGFxTestCases = 0;
		int globalTotalESGFxTestEvents = 0;
		double globalTotalTestCaseRecordingTimeNanos = 0;

		double globalTotalCoverage = 0.0;
		double globalTotalCoverageAnalysisTimeNanos = 0;

		long globalTotalTestExecTimeNanos = 0;
		long globalPeakMemoryExecBytes = 0;
		long totalESGFxModelLoadTimeNanos = 0;

		int handledProducts = 0;
		int failedProducts = 0;

		DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
		symbols.setDecimalSeparator(',');
		DecimalFormat df = new DecimalFormat("#.##", symbols);

		File logFile = new File(ESGFxPerProductLog);
		boolean writeHeader = !logFile.exists() || logFile.length() == 0;

		if (logFile.getParentFile() != null) {
			logFile.getParentFile().mkdirs();
		}

		featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFile,
				ESGFxFile);

		try (PrintWriter ESGFxPerProductLogWriter = new PrintWriter(new FileWriter(logFile, true))) {

			if (writeHeader) {
				ESGFxPerProductLogWriter.println(
						"RunID;ProductID;" + "TotalTestGenTime(ms);TransformationTime(ms);TestGenPeakMemory(MB);"
								+ "NumberOfESGFxVertices;NumberOfESGFxEdges;"
								+ "NumberOfESGFxTestCases;NumberOfESGFxTestEvents;" + "TestCaseRecordingTime(ms);"
								+ "EdgeCoverage(%);EdgeCoverageAnalysisTime(ms);"
								+ "TestExecTimeMs;TestExecPeakMemoryMB;ESGFxModelLoadTimeMs;" + "Status;ErrorReason");
			}

			for (int i = 0; i < dotFiles.length; i++) {

				if ((i % N_SHARDS) != CURRENT_SHARD) {
					continue;
				}

				handledProducts++;
				File dotFile = dotFiles[i];
				String productName = dotFile.getName().replaceAll("(?i)\\.dot", "");
				String productESGFxTestSequences = testsequencesFolder + coverageType + "/" + productName + "_L"
						+ coverageLength + ".txt";

				String configFilePath = productConfigurationFolder + productName + ".config";

				double genTimeMs = 0.0;
				double transformationTimeMs = 0.0;
				double currentPeakGenMemMB = 0.0;
				int esgfxVertices = 0, esgfxEdges = 0;
				int currentTestCases = 0, currentTestEvents = 0;
				double testCaseRecordingTimeMs = 0.0;
				double currentCoverage = 0.0;
				double currentCoverageAnalysisTimeMs = 0.0;
				double execTimeMs = 0.0;
				double currentPeakExecMemMB = 0.0;
				double esgfxModelLoadTimeMs = 0.0;
				String status = "SUCCESS";
				String errorReason = "None";

				ESG productESGFx = null;
				ESG transformedProductESGFx = null;
				ESG stronglyConnectedBalancedESGFx = null;
				Set<EventSequence> testSequences = null;
				List<Vertex> eulerCycle = null;

				TransformedESGFxGenerator transformedESGFxGenerator = null;
				EulerCycleGeneratorForEdgeCoverage eulerCycleGeneratorForEdgeCoverage = null;
				EulerCycleToTestSequenceGenerator eulerCycleToTestSequenceGenerator = null;
				EdgeCoverageAnalyser edgeCoverageAnalyser = null;
				TestExecutor testExecutor = null;

				try {
					updateFeatureExpressionMapFromConfigFile(configFilePath);

					long loadStart = System.nanoTime();
					productESGFx = DOTFileToESGFxConverter.parseDOTFileForESGFxCreation(dotFile.getAbsolutePath(),
							featureExpressionMapFromFeatureModel);
					long loadEnd = System.nanoTime();

					esgfxModelLoadTimeMs = (loadEnd - loadStart) / 1_000_000.0;
					totalESGFxModelLoadTimeNanos += (loadEnd - loadStart);

					if (productESGFx != null) {
						esgfxVertices = productESGFx.getVertexList().size();
						esgfxEdges = productESGFx.getEdgeList().size();
						globalTotalESGFxVertices += esgfxVertices;
						globalTotalESGFxEdges += esgfxEdges;
					}

					resetPeakMemoryCounters();

					long testGenStart = System.nanoTime();
					transformedESGFxGenerator = new TransformedESGFxGenerator();
					eulerCycleGeneratorForEdgeCoverage = new EulerCycleGeneratorForEdgeCoverage();
					eulerCycleToTestSequenceGenerator = new EulerCycleToTestSequenceGenerator();

					long transformationStart = System.nanoTime();
					transformedProductESGFx = transformedESGFxGenerator.generateTransformedESGFx(coverageLength,
							productESGFx);
					long transformationEnd = System.nanoTime();

					transformationTimeMs = (transformationEnd - transformationStart) / 1_000_000.0;
					globalTotalTransformationTimeNanos += (transformationEnd - transformationStart);

					stronglyConnectedBalancedESGFx = StronglyConnectedBalancedESGFxGeneration
							.getStronglyConnectedBalancedESGFxGeneration(transformedProductESGFx);

					transformedProductESGFx = null;

					eulerCycleGeneratorForEdgeCoverage.generateEulerCycle(stronglyConnectedBalancedESGFx);
					stronglyConnectedBalancedESGFx = null;

					eulerCycle = eulerCycleGeneratorForEdgeCoverage.getEulerCycle();

					testSequences = eulerCycleToTestSequenceGenerator.CESgenerator(eulerCycle);
					long testGenEnd = System.nanoTime();

					genTimeMs = (testGenEnd - testGenStart) / 1_000_000.0;
					globalTotalTestGenTimeNanos += (testGenEnd - testGenStart);

					if (testSequences != null) {
						currentTestCases = testSequences.size();
						for (EventSequence seq : testSequences) {
							currentTestEvents += seq.length();
						}
						globalTotalESGFxTestCases += currentTestCases;
						globalTotalESGFxTestEvents += currentTestEvents;
					}

					long currentPeakGenBytes = getPeakHeapMemoryBytes();
					currentPeakGenMemMB = currentPeakGenBytes / (1024.0 * 1024.0);
					if (currentPeakGenBytes > globalPeakMemoryGenBytes)
						globalPeakMemoryGenBytes = currentPeakGenBytes;

					System.gc();

					resetPeakMemoryCounters();

					long testExecStart = System.nanoTime();
					if (testSequences != null && !testSequences.isEmpty()) {
						testExecutor = new TestExecutor(testSequences);
						testExecutor.executeAllTests(productESGFx);
					}
					long testExecEnd = System.nanoTime();

					execTimeMs = (testExecEnd - testExecStart) / 1_000_000.0;
					globalTotalTestExecTimeNanos += (testExecEnd - testExecStart);

					long currentPeakExecBytes = getPeakHeapMemoryBytes();
					currentPeakExecMemMB = currentPeakExecBytes / (1024.0 * 1024.0);
					if (currentPeakExecBytes > globalPeakMemoryExecBytes)
						globalPeakMemoryExecBytes = currentPeakExecBytes;

					long coverageAnalysisStart = System.nanoTime();
					edgeCoverageAnalyser = new EdgeCoverageAnalyser();
					currentCoverage = edgeCoverageAnalyser.analyseEdgeCoverage(productESGFx, testSequences,
							featureExpressionMapFromFeatureModel);
					long coverageAnalysisEnd = System.nanoTime();

					currentCoverageAnalysisTimeMs = (coverageAnalysisEnd - coverageAnalysisStart) / 1_000_000.0;
					globalTotalCoverageAnalysisTimeNanos += (coverageAnalysisEnd - coverageAnalysisStart);
					globalTotalCoverage += currentCoverage;

					long testCaseRecordingStart = System.nanoTime();
					TestSuiteFileWriter.writeEventSequenceSetAndCoverageAnalysisToFile(productESGFxTestSequences,
							testSequences, coverageType, currentCoverage);
					long testCaseRecordingEnd = System.nanoTime();

					testCaseRecordingTimeMs = (testCaseRecordingEnd - testCaseRecordingStart) / 1_000_000.0;
					globalTotalTestCaseRecordingTimeNanos += (testCaseRecordingEnd - testCaseRecordingStart);

				} catch (OutOfMemoryError oom) {
					status = "FAILED";
					errorReason = "OutOfMemory";
					failedProducts++;
					System.gc();
				} catch (Exception e) {
					status = "FAILED";
					errorReason = "Exception: " + e.getClass().getSimpleName();
					failedProducts++;
				} finally {

					ESGFxPerProductLogWriter.println(runID + ";" + productName + ";" + df.format(genTimeMs) + ";"
							+ df.format(transformationTimeMs) + ";" + df.format(currentPeakGenMemMB) + ";"
							+ esgfxVertices + ";" + esgfxEdges + ";" + currentTestCases + ";" + currentTestEvents + ";"
							+ df.format(testCaseRecordingTimeMs) + ";" + df.format(currentCoverage) + ";"
							+ df.format(currentCoverageAnalysisTimeMs) + ";" + df.format(execTimeMs) + ";"
							+ df.format(currentPeakExecMemMB) + ";" + df.format(esgfxModelLoadTimeMs) + ";" + status
							+ ";" + errorReason);

					ESGFxPerProductLogWriter.flush();

					if (eulerCycleGeneratorForEdgeCoverage != null) {
						eulerCycleGeneratorForEdgeCoverage.reset();
					}
					if (eulerCycleToTestSequenceGenerator != null) {
						eulerCycleToTestSequenceGenerator.reset();
					}

					productESGFx = null;
					transformedProductESGFx = null;
					stronglyConnectedBalancedESGFx = null;
					eulerCycle = null;
					testSequences = null;
					transformedESGFxGenerator = null;
					eulerCycleGeneratorForEdgeCoverage = null;
					eulerCycleToTestSequenceGenerator = null;
					edgeCoverageAnalyser = null;
					testExecutor = null;

					if (handledProducts % 50 == 0) {
						System.out.println("Handled products: " + handledProducts);
					}
				}
			}
		}

		double testGenTimeMs = globalTotalTestGenTimeNanos / 1_000_000.0;
		double transformationTimeMs = globalTotalTransformationTimeNanos / 1_000_000.0;
		double coverageAnalysisTimeMs = globalTotalCoverageAnalysisTimeNanos / 1_000_000.0;
		double testCaseRecordingTimeMs = globalTotalTestCaseRecordingTimeNanos / 1_000_000.0;
		double testExecTimeMs = globalTotalTestExecTimeNanos / 1_000_000.0;
		double esgfxModelLoadTimeMs = totalESGFxModelLoadTimeNanos / 1_000_000.0;

		double timeElapsedTotalMs = testGenTimeMs + transformationTimeMs + coverageAnalysisTimeMs
				+ testCaseRecordingTimeMs + testExecTimeMs + esgfxModelLoadTimeMs;

		double totalCoverage = handledProducts > 0 ? globalTotalCoverage / handledProducts : 0.0;

		double globalPeakMemoryGenMB = globalPeakMemoryGenBytes / (1024.0 * 1024.0);
		double globalPeakMemoryExecMB = globalPeakMemoryExecBytes / (1024.0 * 1024.0);

		String summaryResultPath = (N_SHARDS > 1)
				? String.format("%sESG-Fx/%s/%s_ESG-Fx_L%d_shard%02d.csv",
						comparativeEfficiencyTestPipelineMeasurementFolder, coverageType, SPLName, coverageLength,
						CURRENT_SHARD)
				: String.format("%sESG-Fx/%s/%s_ESG-Fx_L%d.csv", comparativeEfficiencyTestPipelineMeasurementFolder,
						coverageType, SPLName, coverageLength);

		TestPipelineMeasurementWriter_ComparativeEfficiency.writeDetailedPipelineMeasurementForESGFx_L234(runID,
				timeElapsedTotalMs, testGenTimeMs, transformationTimeMs, globalPeakMemoryGenMB,
				globalTotalESGFxVertices, globalTotalESGFxEdges, globalTotalESGFxTestCases, globalTotalESGFxTestEvents,
				testCaseRecordingTimeMs, totalCoverage, coverageAnalysisTimeMs, testExecTimeMs, globalPeakMemoryExecMB,
				esgfxModelLoadTimeMs, handledProducts, failedProducts, summaryResultPath, SPLName,
				"L" + coverageLength);

		System.out.println("Total Time Measurement L=" + coverageLength + " " + SPLName + " FINISHED.");
	}
}