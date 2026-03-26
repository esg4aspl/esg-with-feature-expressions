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
import tr.edu.iyte.esgfx.testexecution.TestExecutor;
import tr.edu.iyte.esgfx.testgeneration.guitar.GuitarExecutionWrapper;
import tr.edu.iyte.esgfx.testgeneration.guitar.GuitarOutputToEventSequenceParser;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EdgeCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.eventcoverage.EventCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.guitar.EFGMetricsExtractor;
import tr.edu.iyte.esgfx.testgeneration.guitar.EFGMetricsExtractor.EFGMetrics;

public class RQ1_ComparativeEfficiency_EFG_L234 extends CaseStudyUtilities {

    public void measureTotalTimeForEFGPipeline() throws Exception {

        int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
        int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));
        int L_LEVEL = Integer.parseInt(System.getenv().getOrDefault("L_LEVEL", "3"));
        int runID = Integer.parseInt(System.getenv().getOrDefault("runID", "1"));
        String EFGCoverageType = "L" + L_LEVEL;
        coverageLength = L_LEVEL;
        setCoverageType();

        System.out.println("Pipeline for EFG " + EFGCoverageType + " - SPL: " + SPLName + " STARTED");

        File dotDir = new File(DOTFolder + "L2/");
        File[] dotFiles = dotDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".dot"));
        if (dotFiles == null || dotFiles.length == 0)
            return;

        Arrays.sort(dotFiles, (f1, f2) -> f1.getName().compareTo(f2.getName()));
        
        efg_resultsFolder = efg_resultsFolder + EFGCoverageType + "/";
        String EFGPerProductLog = efg_resultsFolder + SPLName + "_EFGPerProductLog_" + EFGCoverageType + ".csv";
        if (N_SHARDS > 1) {
            EFGPerProductLog = efg_resultsFolder
                    + String.format(SPLName + "_EFGPerProductLog_shard%02d_%s.csv", CURRENT_SHARD, EFGCoverageType);
        }

        long globalTotalGuitarGenTimeNanos = 0;
        long globalTotalParseTimeNanos = 0;
        long globalPeakMemoryGenBytes = 0;
        
        int globalTotalEFGVertices = 0;
        int globalTotalEFGEdges = 0;

        int globalTotalEFGTestCases = 0;
        int globalTotalEFGTestEvents = 0;
        
        double globalTotalEventCoverage = 0.0;
        long globalTotalEventCoverageAnalysisTimeNanos = 0;

        double globalTotalEdgeCoverage = 0.0;
        long globalTotalEdgeCoverageAnalysisTimeNanos = 0;

        long globalTotalTestExecTimeNanos = 0;
        long globalPeakMemoryExecBytes = 0;

        int globalTotalESGFxVertices = 0;
        int globalTotalESGFxEdges = 0;
        long totalESGFxModelLoadTimeNanos = 0;

        int handledProducts = 0;
        int failedProducts = 0;

        int fileCounter = 0;

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#.##", symbols);

        File logFile = new File(EFGPerProductLog);
        boolean writeHeader = !logFile.exists() || logFile.length() == 0;

        if (logFile.getParentFile() != null) {
            logFile.getParentFile().mkdirs();
        }

        featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFile,
                ESGFxFile);

        try (PrintWriter EFGPerProductLogWriter = new PrintWriter(new FileWriter(logFile, true))) {

            if (writeHeader) {
                EFGPerProductLogWriter.println("RunID;ProductID;" + "GuitarGenTime(ms);ParsingTimeMs;ParentPeakMemory(MB);"
                        + "NumberOfEFGVertices;NumberOfEFGEdges;" + "NumberOfEFGTestCases;NumberOfEFGTestEvents;"
                        +   "EventCoverage;EventCoverageAnalysisTimeMs;EdgeCoverage;EdgeCoverageAnalysisTimeMs;TestExecTimeMs;TestExecPeakMemoryMB;"
                        + "ESGFx_Vertices;ESGFx_Edges;ESGFxModelLoadTimeMs;" + "Status;ErrorReason");
            }

            for (File dotFile : dotFiles) {
                fileCounter++;
                if (((fileCounter - 1) % N_SHARDS) != CURRENT_SHARD)
                    continue;

                handledProducts++;

                String productName = dotFile.getName().replaceAll("(?i)\\.dot", "");
                String efgFilePath = EFGFolder + productName + ".EFG";
                String EFGTestSequencesFolderPerProduct = efg_testsequencesFolder + productName + "/" + EFGCoverageType
                        + "/";

                String configFilePath = productConfigurationFolder + productName + ".config";

                double guitarGenTimeMs = 0.0;
                double parseTimeMs = 0.0;
                double currentPeakGenMemMB = 0.0;
                int efgVertices = 0, efgEdges = 0;
                int currentTestCases = 0, currentTestEvents = 0;
                double currentEventCoverage = 0.0, currentEventCovTimeMs = 0.0;
                double currentEdgeCoverage = 0.0;
                double currentEdgeCovTimeMs = 0.0;
                double execTimeMs = 0.0;
                double currentPeakExecMemMB = 0.0;
                int esgfxVertices = 0, esgfxEdges = 0;
                double esgfxModelLoadTimeMs = 0.0;
                String status = "SUCCESS";
                String errorReason = "None";

                ESG productESGFx = null;
                Set<EventSequence> testSequences = null;
                EdgeCoverageAnalyser edgeCoverageAnalyser = null;
                EventCoverageAnalyser evAnalyser = null;
                TestExecutor testExecutor = null;

                try {
                    updateFeatureExpressionMapFromConfigFile(configFilePath);
                    
                    long loadStart = System.nanoTime();
                    productESGFx = DOTFileToESGFxConverter.parseDOTFileForESGFxCreation(dotFile.getAbsolutePath(),
                            featureExpressionMapFromFeatureModel);
                    long loadEnd = System.nanoTime();

//                    System.out.println(productESGFx);
                    esgfxModelLoadTimeMs = (loadEnd - loadStart) / 1_000_000.0;
                    totalESGFxModelLoadTimeNanos += (loadEnd - loadStart);

                    if (productESGFx != null) {
                        esgfxVertices = productESGFx.getVertexList().size();
                        esgfxEdges = productESGFx.getEdgeList().size();
                        globalTotalESGFxVertices += esgfxVertices;
                        globalTotalESGFxEdges += esgfxEdges;
                    }

                    EFGMetrics efgMetrics = EFGMetricsExtractor.getMetrics(efgFilePath);
                    efgVertices = efgMetrics.vertices;
                    efgEdges = efgMetrics.edges;
                    globalTotalEFGVertices += efgVertices;
                    globalTotalEFGEdges += efgEdges;

                    resetPeakMemoryCounters();
                    
                    long genStart = System.nanoTime();
                    GuitarExecutionWrapper.generateTestsFromEFG(efgFilePath, EFGTestSequencesFolderPerProduct, L_LEVEL);
                    long genEnd = System.nanoTime();
                    
                    guitarGenTimeMs = (genEnd - genStart) / 1_000_000.0;
                    globalTotalGuitarGenTimeNanos += (genEnd - genStart);

                    long parseStart = System.nanoTime();
                    testSequences = GuitarOutputToEventSequenceParser.parseGuitarTests(EFGTestSequencesFolderPerProduct,
                            efgFilePath, (ESGFx) productESGFx);
                    long parseEnd = System.nanoTime();
                    
                    parseTimeMs = (parseEnd - parseStart) / 1_000_000.0;
                    globalTotalParseTimeNanos += (parseEnd - parseStart);
                    
                    if (testSequences != null) {
                        currentTestCases = testSequences.size();
                        for (EventSequence seq : testSequences) {
                            currentTestEvents += seq.length();
                        }
                        globalTotalEFGTestCases += currentTestCases;
                        globalTotalEFGTestEvents += currentTestEvents;
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

                    execTimeMs = (execEnd - execStart) / 1_000_000.0;
                    globalTotalTestExecTimeNanos += (execEnd - execStart);
                    
                    long currentPeakExecBytes = getPeakHeapMemoryBytes();
                    currentPeakExecMemMB = currentPeakExecBytes / (1024.0 * 1024.0);
                    if (currentPeakExecBytes > globalPeakMemoryExecBytes)
                        globalPeakMemoryExecBytes = currentPeakExecBytes;
                    
//            		for (EventSequence es : testSequences) {
//            			System.out.println(/* es.length() + " - " + */es);
//            		}
//            		System.out.println();

                    long evCovStart = System.nanoTime();
                    evAnalyser = new EventCoverageAnalyser();
                    currentEventCoverage = evAnalyser.analyseEventCoverage(productESGFx, testSequences,
                            featureExpressionMapFromFeatureModel);
                    long evCovEnd = System.nanoTime();
                    currentEventCovTimeMs = (evCovEnd - evCovStart) / 1_000_000.0;
                    globalTotalEventCoverageAnalysisTimeNanos += (evCovEnd - evCovStart);
                    globalTotalEventCoverage += currentEventCoverage;

                    long coverageAnalysisStart = System.nanoTime();
                    edgeCoverageAnalyser = new EdgeCoverageAnalyser();
                    currentEdgeCoverage = edgeCoverageAnalyser.analyseEdgeCoverage(productESGFx, testSequences,
                            featureExpressionMapFromFeatureModel);
                    long coverageAnalysisEnd = System.nanoTime();
                    currentEdgeCovTimeMs = (coverageAnalysisEnd - coverageAnalysisStart) / 1_000_000.0;
                    globalTotalEdgeCoverageAnalysisTimeNanos += (coverageAnalysisEnd - coverageAnalysisStart);
                    globalTotalEdgeCoverage += currentEdgeCoverage;

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

                    EFGPerProductLogWriter.println(
                    		runID + ";" + 
                    		productName + ";" + 
                    		df.format(guitarGenTimeMs) + ";" + 
                    		df.format(parseTimeMs) + ";" + 
                    		df.format(currentPeakGenMemMB) + ";" + 
                    		efgVertices + ";" + 
                    		efgEdges + ";" + 
                    		currentTestCases + ";" + 
                    		currentTestEvents + ";" + 
                    		df.format(currentEventCoverage) + ";"+ 
                    		df.format(currentEventCovTimeMs)+ ";"+ 
                    		df.format(currentEdgeCoverage) + ";"+ 
                    		df.format(currentEdgeCovTimeMs) + ";" +
                            df.format(execTimeMs) + ";" +
                            df.format(currentPeakExecMemMB) + ";" + 
                            esgfxVertices + ";" + 
                            esgfxEdges + ";" +
                            df.format(esgfxModelLoadTimeMs) + ";" + 
                            status + ";" + 
                            errorReason);

                    EFGPerProductLogWriter.flush();

                    productESGFx = null;
                    testSequences = null;
                    edgeCoverageAnalyser = null;
                    testExecutor = null;

                    if (handledProducts % 50 == 0) {
                        System.out.println("Handled products: " + handledProducts);
                    }
                }
            }
        }

        double totalGuitarGenTimeMs = globalTotalGuitarGenTimeNanos / 1_000_000.0;
        double totalParseTimeMs = globalTotalParseTimeNanos / 1_000_000.0;
        double totalExecTimeMs = globalTotalTestExecTimeNanos / 1_000_000.0;
        double totalESGFxModelLoadTimeMs = totalESGFxModelLoadTimeNanos / 1_000_000.0;
        double totalEventCoverageAnalysisTimeMs = globalTotalEventCoverageAnalysisTimeNanos / 1_000_000.0;
        double totalEdgeCoverageAnalysisTimeMs = globalTotalEdgeCoverageAnalysisTimeNanos / 1_000_000.0;

        
        double totalWallClockMs = totalGuitarGenTimeMs + totalParseTimeMs + totalExecTimeMs + totalESGFxModelLoadTimeMs + totalEdgeCoverageAnalysisTimeMs;
        
        double totalEdgeCoverage = handledProducts > 0 ? globalTotalEdgeCoverage / handledProducts : 0.0;
        double totalEventCoverage = handledProducts > 0 ? globalTotalEventCoverage / handledProducts : 0.0;

        double globalPeakMemoryGenMB = globalPeakMemoryGenBytes / (1024.0 * 1024.0);
        double globalPeakMemoryExecMB = globalPeakMemoryExecBytes / (1024.0 * 1024.0);

        String summaryResultPath = (N_SHARDS > 1)
                ? String.format("%sEFG/%s/%s_EFG_L%d_shard%02d.csv", comparativeEfficiencyTestPipelineMeasurementFolder, coverageType, SPLName, coverageLength, CURRENT_SHARD)
                : String.format("%sEFG/%s/%s_EFG_L%d.csv", comparativeEfficiencyTestPipelineMeasurementFolder, coverageType, SPLName, coverageLength);

        TestPipelineMeasurementWriter_ComparativeEfficiency.writeTotalPipelineMeasurementForEFG(runID,totalWallClockMs,
                totalGuitarGenTimeMs, totalParseTimeMs, globalPeakMemoryGenMB, globalTotalEFGVertices, globalTotalEFGEdges,
                globalTotalEFGTestCases, globalTotalEFGTestEvents,totalEventCoverage, totalEventCoverageAnalysisTimeMs,  totalEdgeCoverage, totalEdgeCoverageAnalysisTimeMs,
                totalExecTimeMs, globalPeakMemoryExecMB, globalTotalESGFxVertices, globalTotalESGFxEdges,
                totalESGFxModelLoadTimeMs, handledProducts, failedProducts, summaryResultPath, SPLName,
                EFGCoverageType);

        System.out.println("Pipeline " + EFGCoverageType + " FINISHED.");
    }
}