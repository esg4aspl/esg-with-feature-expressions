package tr.edu.iyte.esgfx.cases;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esgfx.cases.resultrecordingutilities.TestPipelineMeasurementWriter_ComparativeEfficiency;
import tr.edu.iyte.esgfx.conversion.dot.DOTFileToESGFxConverter;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.testgeneration.TestSuiteFileWriter;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EdgeCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.eventcoverage.EventCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.randomwalktesting.RandomWalkTestGenerator;
import tr.edu.iyte.esgfx.testexecution.TestExecutor;

public class RQ1_ComparativeEfficiency_RandomWalk extends CaseStudyUtilities {

    public void measurePipeLineForRandomWalk() throws Exception {

        System.out.println("Total Time Measurement L=0 (Random Walk Pipeline) " + SPLName + " STARTED");

        int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
        int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));
        int runID = Integer.parseInt(System.getenv().getOrDefault("runID", "1"));
        coverageLength = 0;
        setCoverageType();

        long globalTotalTestGenTimeNanos = 0;
        long globalTotalTestExecTimeNanos = 0;
        long globalPeakMemoryGenBytes = 0;
        long globalPeakMemoryExecBytes = 0;
        long totalESGFxModelLoadTimeNanos = 0;

        int globalTotalVertices = 0;
        int globalTotalEdges = 0;
        int globalTotalTestCases = 0;
        int globalTotalTestEvents = 0;
        long globalTotalAbortedSequences = 0;

        double globalTotalEventCoverage = 0.0;
        long globalTotalEventCoverageTimeNanos = 0;
        double globalTotalEdgeCoverage = 0.0;
        long globalTotalEdgeCoverageTimeNanos = 0;
        long globalTotalTestCaseRecordingTimeNanos = 0;

        int handledProducts = 0;
        int failedProducts = 0;

        int safetyLimitHitCount = 0;
        long totalTimeSpentOnSafetyLimitsMs = 0;
        long totalStepsSpentOnSafetyLimits = 0;
        double totalCoverageOnSafetyLimits = 0.0;

        double dampingFactor = 0.85;
        double targetCoverage = 100.0;

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#.##", symbols);

        String RWPerProductLog = testsequencesFolder + coverageType + "/" + SPLName + "_RandomWalkPerProductLog.csv";
        if (N_SHARDS > 1) {
            RWPerProductLog = testsequencesFolder + coverageType + "/"
                    + String.format(SPLName + "_RandomWalkPerProductLog_shard%02d.csv", CURRENT_SHARD);
        }

        File logFile = new File(RWPerProductLog);
        boolean writeHeader = !logFile.exists() || logFile.length() == 0;
        if (logFile.getParentFile() != null)
            logFile.getParentFile().mkdirs();

        featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFile,
                ESGFxFile);

        String dotDirectoryPath = DOTFolder + "L2/";
        File dotDir = new File(dotDirectoryPath);

        if (!dotDir.exists() || !dotDir.isDirectory())
            throw new RuntimeException("CRITICAL ERROR: DOT directory not found: " + dotDirectoryPath);

        File[] dotFiles = dotDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".dot"));
        if (dotFiles == null || dotFiles.length == 0)
            return;

        Arrays.sort(dotFiles, (f1, f2) -> f1.getName().compareTo(f2.getName()));

        try (PrintWriter RWPerProductLogWriter = new PrintWriter(new FileWriter(logFile, true))) {

            if (writeHeader) {
                RWPerProductLogWriter.println("RunID;ProductID;ModelLoadTime(ms);TestGenTime(ms);TestGenPeakMemory(MB);"
                        + "Vertices;Edges;TestCases;TestEvents;AbortedSequences;TestCaseRecordingTime(ms);"
                        + "EventCoverage(%);EventCoverageTime(ms);EdgeCoverage(%);EdgeCoverageTime(ms);"
                        + "TestExecTime(ms);TestExecPeakMemory(MB);SafetyLimitHit;AchievedCoverageIfHit;Status;ErrorReason");
            }

            int productID = 0;
            for (File dotFile : dotFiles) {
                productID++;
                if (((productID - 1) % N_SHARDS) != CURRENT_SHARD)
                    continue;
                handledProducts++;

                String productName = dotFile.getName().replaceAll("(?i)\\.dot", "");
                String productTestSequencesPath = testsequencesFolder + coverageType + "/" + productName
                        + "_RandomWalk.txt";

                String configFilePath = productConfigurationFolder + productName + ".config";

                double currentModelLoadTimeMs = 0.0, currentGenTimeMs = 0.0, currentPeakGenMemMB = 0.0;
                int currentVertices = 0, currentEdges = 0, currentTestCases = 0, currentTestEvents = 0;
                long currentAbortedSequences = 0;
                double currentEventCoverage = 0.0, currentEventCovTimeMs = 0.0;
                double currentEdgeCoverage = 0.0, currentEdgeCovTimeMs = 0.0;
                double currentRecordingTimeMs = 0.0, currentExecTimeMs = 0.0, currentPeakExecMemMB = 0.0;
                boolean hitSafetyLimit = false;
                double limitAchievedCoverage = 0.0;
                String status = "SUCCESS", errorReason = "None";

                ESG productESGFx = null;
                Set<EventSequence> testSequences = null;
                RandomWalkTestGenerator rwGenerator = null;
                TestExecutor testExecutor = null;
                EventCoverageAnalyser evAnalyser = null;
                EdgeCoverageAnalyser edAnalyser = null;

                try {
                    updateFeatureExpressionMapFromConfigFile(configFilePath);

                    long loadStart = System.nanoTime();
                    productESGFx = DOTFileToESGFxConverter.parseDOTFileForESGFxCreation(dotFile.getAbsolutePath(),
                            featureExpressionMapFromFeatureModel);
                    long loadEnd = System.nanoTime();
                    currentModelLoadTimeMs = (loadEnd - loadStart) / 1_000_000.0;
                    totalESGFxModelLoadTimeNanos += (loadEnd - loadStart);

                    if (productESGFx != null) {
                        currentVertices = productESGFx.getVertexList().size();
                        currentEdges = productESGFx.getEdgeList().size();
                        globalTotalVertices += currentVertices;
                        globalTotalEdges += currentEdges;
                    }

                    resetPeakMemoryCounters();

                    long testGenStart = System.nanoTime();
                    int safetyLimit = Math.min(
                    	    5 * currentVertices * currentVertices * currentVertices,
                    	    2_000_000
                    	);
                    long seed = 42L + productID;
                    rwGenerator = new RandomWalkTestGenerator((ESGFx) productESGFx, dampingFactor, seed);
                    testSequences = rwGenerator.generateWalkUntilEdgeCoverage(targetCoverage, safetyLimit);
                    long testGenEnd = System.nanoTime();

                    currentGenTimeMs = (testGenEnd - testGenStart) / 1_000_000.0;
                    globalTotalTestGenTimeNanos += (testGenEnd - testGenStart);
                    
                    if (rwGenerator.isSafetyLimitHit()) {
                        hitSafetyLimit = true;
                        safetyLimitHitCount++;
                        totalTimeSpentOnSafetyLimitsMs += rwGenerator.getExecutionTimeMs();
                        totalStepsSpentOnSafetyLimits += rwGenerator.getStepsTaken();
                        limitAchievedCoverage = rwGenerator.getAchievedCoverage();
                        totalCoverageOnSafetyLimits += limitAchievedCoverage;
                    }

                    currentAbortedSequences = rwGenerator.getAbortedSequenceCount();
                    globalTotalAbortedSequences += currentAbortedSequences;

                    if (testSequences != null) {
                        currentTestCases = testSequences.size();
                        for (EventSequence seq : testSequences)
                            currentTestEvents += seq.length();
                        globalTotalTestCases += currentTestCases;
                        globalTotalTestEvents += currentTestEvents;
                    }

                    long currentPeakGenBytes = getPeakHeapMemoryBytes();
                    currentPeakGenMemMB = currentPeakGenBytes / (1024.0 * 1024.0);
                    if (currentPeakGenBytes > globalPeakMemoryGenBytes)
                        globalPeakMemoryGenBytes = currentPeakGenBytes;
                    
                    System.gc();
                    
                    resetPeakMemoryCounters();

                    long execStart = System.nanoTime();
                    if (testSequences != null && !testSequences.isEmpty()) {
                        testExecutor = new TestExecutor(testSequences);
                        testExecutor.executeAllTests(productESGFx);
                    }
                    long execEnd = System.nanoTime();
                    currentExecTimeMs = (execEnd - execStart) / 1_000_000.0;
                    globalTotalTestExecTimeNanos += (execEnd - execStart);
                    
                    long currentPeakExecBytes = getPeakHeapMemoryBytes();
                    currentPeakExecMemMB = currentPeakExecBytes / (1024.0 * 1024.0);
                    if (currentPeakExecBytes > globalPeakMemoryExecBytes)
                        globalPeakMemoryExecBytes = currentPeakExecBytes;
                    
                    long evCovStart = System.nanoTime();
                    evAnalyser = new EventCoverageAnalyser();
                    currentEventCoverage = evAnalyser.analyseEventCoverage(productESGFx, testSequences,
                            featureExpressionMapFromFeatureModel);
                    long evCovEnd = System.nanoTime();
                    currentEventCovTimeMs = (evCovEnd - evCovStart) / 1_000_000.0;
                    globalTotalEventCoverageTimeNanos += (evCovEnd - evCovStart);
                    globalTotalEventCoverage += currentEventCoverage;

                    long edCovStart = System.nanoTime();
                    edAnalyser = new EdgeCoverageAnalyser();
                    currentEdgeCoverage = edAnalyser.analyseEdgeCoverage(productESGFx, testSequences,
                            featureExpressionMapFromFeatureModel);
                    long edCovEnd = System.nanoTime();
                    currentEdgeCovTimeMs = (edCovEnd - edCovStart) / 1_000_000.0;
                    globalTotalEdgeCoverageTimeNanos += (edCovEnd - edCovStart);
                    globalTotalEdgeCoverage += currentEdgeCoverage;

                    long recStart = System.nanoTime();
                    TestSuiteFileWriter.writeEventSequenceSetAndCoverageAnalysisToFile(productTestSequencesPath,
                            testSequences, "Event Coverage", currentEventCoverage);
                    TestSuiteFileWriter.writeCoverageAnalysisToFile(productTestSequencesPath, "Edge Coverage", currentEdgeCoverage);
                    long recEnd = System.nanoTime();
                    currentRecordingTimeMs = (recEnd - recStart) / 1_000_000.0;
                    globalTotalTestCaseRecordingTimeNanos += (recEnd - recStart);

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
                    RWPerProductLogWriter.println(runID + ";" + productName + ";" + df.format(currentModelLoadTimeMs)
                            + ";" + df.format(currentGenTimeMs) + ";" + df.format(currentPeakGenMemMB) + ";"
                            + currentVertices + ";" + currentEdges + ";" + currentTestCases + ";" + currentTestEvents
                            + ";" + currentAbortedSequences + ";" + df.format(currentRecordingTimeMs) + ";"
                            + df.format(currentEventCoverage) + ";" + df.format(currentEventCovTimeMs) + ";"
                            + df.format(currentEdgeCoverage) + ";" + df.format(currentEdgeCovTimeMs) + ";"
                            + df.format(currentExecTimeMs) + ";" + df.format(currentPeakExecMemMB) + ";"
                            + hitSafetyLimit + ";" + df.format(limitAchievedCoverage) + ";" + status + ";"
                            + errorReason);
                    RWPerProductLogWriter.flush();

                    productESGFx = null;
                    testSequences = null;
                    rwGenerator = null;
                    testExecutor = null;
                    evAnalyser = null;
                    edAnalyser = null;

                    if (handledProducts % 50 == 0) {
                        System.out.println("Handled products: " + handledProducts);
                    }
                }
            }
        }

        double testGenTimeMs = globalTotalTestGenTimeNanos / 1_000_000.0;
        double evCovTimeMs = globalTotalEventCoverageTimeNanos / 1_000_000.0;
        double edCovTimeMs = globalTotalEdgeCoverageTimeNanos / 1_000_000.0;
        double testCaseRecordingTimeMs = globalTotalTestCaseRecordingTimeNanos / 1_000_000.0;
        double testExecTimeMs = globalTotalTestExecTimeNanos / 1_000_000.0;
        double esgfxModelLoadTimeMs = totalESGFxModelLoadTimeNanos / 1_000_000.0;

        double timeElapsedTotalMs = testGenTimeMs + evCovTimeMs + edCovTimeMs + testCaseRecordingTimeMs + testExecTimeMs + esgfxModelLoadTimeMs;

        double peakGenMemMB = globalPeakMemoryGenBytes / (1024.0 * 1024.0);
        double peakExecMemMB = globalPeakMemoryExecBytes / (1024.0 * 1024.0);

        double avgEventCoverage = handledProducts > 0 ? globalTotalEventCoverage / handledProducts : 0.0;
        double avgEdgeCoverage = handledProducts > 0 ? globalTotalEdgeCoverage / handledProducts : 0.0;

        double avgTimeOnSafetyLimitMs = 0.0;
        double avgStepsOnSafetyLimit = 0.0;
        double avgCoverageOnSafetyLimit = 0.0;

        if (safetyLimitHitCount > 0) {
            avgTimeOnSafetyLimitMs = (double) totalTimeSpentOnSafetyLimitsMs / safetyLimitHitCount;
            avgStepsOnSafetyLimit = (double)totalStepsSpentOnSafetyLimits / safetyLimitHitCount;
            avgCoverageOnSafetyLimit = (double) totalCoverageOnSafetyLimits / safetyLimitHitCount;
        }

        System.out.println("=====================================================");
        System.out.println(" RANDOM WALK SAFETY LIMIT REPORT");
        System.out.println("Products hit limit     : " + safetyLimitHitCount + " / " + handledProducts);
        if (safetyLimitHitCount > 0) {
            System.out.println("Avg limit hit MS       : " + df.format(avgTimeOnSafetyLimitMs) + " ms");
            System.out.println("Avg limit hit steps    : " + avgStepsOnSafetyLimit);
            System.out.println("Avg Cov when hit (%)   : " + df.format(avgCoverageOnSafetyLimit));
        }
        System.out.println("Aborted sequences   : " + globalTotalAbortedSequences);
        System.out.println("=====================================================");

        String summaryResultPath = (N_SHARDS > 1)
                ? String.format("%sRandomWalk/%s/%s_RandomWalk_L%d_shard%02d.csv", comparativeEfficiencyTestPipelineMeasurementFolder, coverageType, SPLName,  coverageLength, CURRENT_SHARD)
                : String.format("%sRandomWalk/%s/%s_RandomWalk_L%d.csv", comparativeEfficiencyTestPipelineMeasurementFolder, coverageType, SPLName, coverageLength);

        TestPipelineMeasurementWriter_ComparativeEfficiency.writeDetailedPipelineMeasurementForRandomWalk(runID,
                timeElapsedTotalMs, esgfxModelLoadTimeMs, testGenTimeMs, peakGenMemMB, globalTotalVertices,
                globalTotalEdges, globalTotalTestCases, globalTotalTestEvents, globalTotalAbortedSequences,
                testCaseRecordingTimeMs, avgEventCoverage, evCovTimeMs, avgEdgeCoverage, edCovTimeMs, testExecTimeMs,
                peakExecMemMB, safetyLimitHitCount, avgTimeOnSafetyLimitMs, avgStepsOnSafetyLimit,
                avgCoverageOnSafetyLimit, handledProducts, failedProducts, summaryResultPath, SPLName, coverageType);

        System.out.println("Total Time Measurement L=0 " + SPLName + " FINISHED");
    }
}