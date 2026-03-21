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
import tr.edu.iyte.esgfx.testgeneration.eventcoverage.EulerCycleGeneratorForEventCoverage;
import tr.edu.iyte.esgfx.testgeneration.eventcoverage.EventCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;
import tr.edu.iyte.esgfx.testexecution.TestExecutor;

public class RQ1_ComparativeEfficiency_ESGFx_L1 extends CaseStudyUtilities {

    public void measurePipelineForEventCoverage() throws Exception {
        System.out.println("Test Generation And Execution for ESG-Fx Pipeline L=1 " + SPLName + " STARTED");

        int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
        int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));
        int runID = Integer.parseInt(System.getenv().getOrDefault("runID", "1"));
        coverageLength = 1;
        setCoverageType();

        File dotDir = new File(DOTFolder + "L2/");
        File[] dotFiles = dotDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".dot"));
        if (dotFiles == null || dotFiles.length == 0)
            return;
            
        Arrays.sort(dotFiles, (f1, f2) -> f1.getName().compareTo(f2.getName()));

        String ESGFxPerProductLog = testsequencesFolder + coverageType + "/" + SPLName + "_ESG-FxPerProductLog_L"
                + coverageLength + ".csv";

        if (N_SHARDS > 1) {
            ESGFxPerProductLog = testsequencesFolder + coverageType + "/" + String
                    .format(SPLName + "_ESG-FxPerProductLog_shard%02d_%s.csv", CURRENT_SHARD, "L" + coverageLength);
        }

        long globalTotalTestGenTimeNanos = 0;
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
                ESGFxPerProductLogWriter.println("RunID;ProductID;" 
                        + "TestGenTime(ms);TestGenPeakMemory(MB);"
                        + "NumberOfESGFxVertices;NumberOfESGFxEdges;"
                        + "NumberOfESGFxTestCases;NumberOfESGFxTestEvents;"
                        + "TestCaseRecordingTime(ms);"
                        + "EventCoverage(%);EventCoverageAnalysisTime(ms);"
                        + "TestExecTimeMs;TestExecPeakMemoryMB;ESGFxModelLoadTimeMs;"
                        + "Status;ErrorReason");
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
                Set<EventSequence> testSequences = null;
                EulerCycleGeneratorForEventCoverage eulerCycleGeneratorForEventCoverage = null;
                EulerCycleToTestSequenceGenerator eulerCycleToTestSequenceGenerator = null;
                ESG stronglyConnectedBalancedESGFx = null;
                List<Vertex> eulerCycle = null;
                EventCoverageAnalyser eventCoverageAnalyser = null;
                TestExecutor testExecutor = null;

                try {
                    updateFeatureExpressionMapFromConfigFile(configFilePath);
                    
                    long loadStart = System.nanoTime();
                    productESGFx = DOTFileToESGFxConverter.parseDOTFileForESGFxCreation(dotFile.getAbsolutePath(), featureExpressionMapFromFeatureModel);
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
                    eulerCycleGeneratorForEventCoverage = new EulerCycleGeneratorForEventCoverage(
                            featureExpressionMapFromFeatureModel);
                    eulerCycleToTestSequenceGenerator = new EulerCycleToTestSequenceGenerator();
                    stronglyConnectedBalancedESGFx = StronglyConnectedBalancedESGFxGeneration
                            .getStronglyConnectedBalancedESGFxGeneration(productESGFx);
                    
                    eulerCycleGeneratorForEventCoverage.generateEulerCycle(stronglyConnectedBalancedESGFx);
                    eulerCycle = eulerCycleGeneratorForEventCoverage.getEulerCycle();

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
                    
                    // ISOLATION BARRIER
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
                    eventCoverageAnalyser = new EventCoverageAnalyser();
                    currentCoverage = eventCoverageAnalyser.analyseEventCoverage(productESGFx, testSequences,
                            featureExpressionMapFromFeatureModel);
                    long coverageAnalysisEnd = System.nanoTime();
                    
                    currentCoverageAnalysisTimeMs = (coverageAnalysisEnd - coverageAnalysisStart) / 1_000_000.0;
                    globalTotalCoverageAnalysisTimeNanos += (coverageAnalysisEnd - coverageAnalysisStart);
                    globalTotalCoverage += currentCoverage;

                    long testCaseRecordingStart = System.nanoTime();
                    TestSuiteFileWriter.writeEventSequenceSetAndCoverageAnalysisToFile(productESGFxTestSequences,
                            testSequences,coverageType, currentCoverage);
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
                    e.printStackTrace(); 
                } finally {
                    
                    ESGFxPerProductLogWriter.println(runID + ";" + productName + ";" + df.format(genTimeMs) + ";"
                            + df.format(currentPeakGenMemMB) + ";" + esgfxVertices + ";" + esgfxEdges + ";" + currentTestCases
                            + ";" + currentTestEvents + ";" + df.format(testCaseRecordingTimeMs) + ";" + df.format(currentCoverage) + ";" 
                            + df.format(currentCoverageAnalysisTimeMs) + ";" + df.format(execTimeMs) + ";" + df.format(currentPeakExecMemMB)
                            + ";" + df.format(esgfxModelLoadTimeMs) + ";" + status + ";" + errorReason);

                    ESGFxPerProductLogWriter.flush();

                    if (eulerCycleGeneratorForEventCoverage != null) {
                        eulerCycleGeneratorForEventCoverage.reset();
                    }
                    if (eulerCycleToTestSequenceGenerator != null) {
                        eulerCycleToTestSequenceGenerator.reset();
                    }

                    productESGFx = null;
                    testSequences = null;
                    eulerCycleGeneratorForEventCoverage = null;
                    eulerCycleToTestSequenceGenerator = null;
                    stronglyConnectedBalancedESGFx = null;
                    eulerCycle = null;
                    eventCoverageAnalyser = null;
                    testExecutor = null;

                    if (handledProducts % 100 == 0) {
                        System.out.println("Handled products: " + handledProducts);
                    }
                }
            }
        }

        double testGenTimeMs = globalTotalTestGenTimeNanos / 1_000_000.0;
        double coverageAnalysisTimeMs = globalTotalCoverageAnalysisTimeNanos / 1_000_000.0;
        double testCaseRecordingTimeMs = globalTotalTestCaseRecordingTimeNanos / 1_000_000.0;
        double testExecTimeMs = globalTotalTestExecTimeNanos / 1_000_000.0;
        double esgfxModelLoadTimeMs = totalESGFxModelLoadTimeNanos / 1_000_000.0;
        
        double timeElapsedTotalMs = testGenTimeMs + coverageAnalysisTimeMs + testCaseRecordingTimeMs + testExecTimeMs + esgfxModelLoadTimeMs;
        
        double totalCoverage = handledProducts > 0 ? globalTotalCoverage / handledProducts : 0.0;

        double globalPeakMemoryGenMB = globalPeakMemoryGenBytes / (1024.0 * 1024.0);
        double globalPeakMemoryExecMB = globalPeakMemoryExecBytes / (1024.0 * 1024.0);

        String summaryResultPath = (N_SHARDS > 1)
                ? String.format("%sESG-Fx/%s/%s_ESG-Fx_L%d_shard%02d.csv", comparativeEfficiencyTestPipelineMeasurementFolder, coverageType, SPLName, coverageLength, CURRENT_SHARD)
                : String.format("%sESG-Fx/%s/%s_ESG-Fx_L%d.csv", comparativeEfficiencyTestPipelineMeasurementFolder, coverageType, SPLName, coverageLength);

        TestPipelineMeasurementWriter_ComparativeEfficiency.writeDetailedPipelineMeasurementForESGFx_L1(runID,
                timeElapsedTotalMs, testGenTimeMs, globalPeakMemoryGenMB, globalTotalESGFxVertices, globalTotalESGFxEdges,
                globalTotalESGFxTestCases, globalTotalESGFxTestEvents, testCaseRecordingTimeMs, totalCoverage,
                coverageAnalysisTimeMs, testExecTimeMs, globalPeakMemoryExecMB, esgfxModelLoadTimeMs, handledProducts, failedProducts,
                summaryResultPath, SPLName, "L" + coverageLength);

        System.out.println("Total Time Measurement L=1 " + SPLName + " FINISHED.");
    }
}