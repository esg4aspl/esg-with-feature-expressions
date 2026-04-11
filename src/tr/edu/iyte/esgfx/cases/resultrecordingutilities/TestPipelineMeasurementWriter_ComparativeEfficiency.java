package tr.edu.iyte.esgfx.cases.resultrecordingutilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Locale;
import java.text.DecimalFormatSymbols;

public class TestPipelineMeasurementWriter_ComparativeEfficiency {
	
	/**
	 * 
	 * @param runID
	 * @param totalElapsedTimeMs
	 * @param testGenTimeMs
	 * @param testGenPeakMemoryMB
	 * @param numberOfESGFxVertices
	 * @param numberOfESGFxEdges
	 * @param numberOfESGFxTestCases
	 * @param numberOfESGFxTestEvents
	 * @param testCaseRecordingTimeMs
	 * @param eventCoveragePercent
	 * @param eventCoverageAnalysisTimeMs
	 * @param testExecTimeMs
	 * @param testExecPeakMemoryMB
	 * @param ESGFxModelLoadTimeMs
	 * @param processedProductCount
	 * @param failedProductCount
	 * @param folderName
	 * @param SPLName
	 * @param coverageType L1-> event coverage
	 * 
	 */

    public static void writeDetailedPipelineMeasurementForESGFx_L1(int runID, double totalElapsedTimeMs, double testGenTimeMs,
            double testGenPeakMemoryMB, int numberOfESGFxVertices, int numberOfESGFxEdges, int numberOfESGFxTestCases,
            int numberOfESGFxTestEvents, double testCaseRecordingTimeMs, double eventCoveragePercent,
            double eventCoverageAnalysisTimeMs, double testExecTimeMs, double testExecPeakMemoryMB,
            double ESGFxModelLoadTimeMs, int processedProductCount,  int failedProductCount, String folderName, String SPLName,
            String coverageType) {

        // Set decimal separator to comma
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator(',');

        DecimalFormat df = new DecimalFormat("#.##", symbols);

        BufferedWriter writer = null;
        try {
            File file = new File(folderName);

            // Ensure parent directory exists (Safety check)
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            writer = new BufferedWriter(new FileWriter(file, true));

            String dataRow = runID + ";" +
            		SPLName + ";" 
                    + coverageType + ";" 
                    + df.format(totalElapsedTimeMs) + ";"
                    + df.format(testGenTimeMs) + ";" 
                    + df.format(testGenPeakMemoryMB) + ";" 
                    + numberOfESGFxVertices + ";"
                    + numberOfESGFxEdges + ";" 
                    + numberOfESGFxTestCases + ";" 
                    + numberOfESGFxTestEvents + ";"
                    + df.format(testCaseRecordingTimeMs) + ";" 
                    + df.format(eventCoveragePercent) + ";"
                    + df.format(eventCoverageAnalysisTimeMs) + ";" 
                    + df.format(testExecTimeMs) + ";"
                    + df.format(testExecPeakMemoryMB) + ";" 
                    + df.format(ESGFxModelLoadTimeMs) + ";"
                    + processedProductCount + ";"
                    + failedProductCount
                    + "\n";

            if (file.length() > 0) {
                // Append data
                writer.append(dataRow);
            } else {
                // Write Header first
                writer.write("Run ID;"+ "SPL Name;" + "Coverage Type;" + "Total Elapsed Time(ms);" + "Test Generation Time(ms);"
                        + "Test Generation Peak Memory(MB);" + "Number of ESGFx Vertices;" + "Number of ESGFx Edges;"
                        + "Number of ESGFx Test Cases;" + "Number of ESGFx Test Events;"
                        + "Test Case Recording Time(ms);" +  "Event Coverage(%);"
                        + "Event Coverage Analysis Time(ms);" + "Test Execution Time(ms);" + "Test Execution Peak Memory(MB);"
                        + "ESGFx Model Load Time(ms);" + "Processed Products; Failed Products\n");
                // Write Data
                writer.write(dataRow);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * 
     * @param runID
     * @param totalElapsedTimeMs
     * @param testGenTimeMs
     * @param transformationTimeMs
     * @param testGenPeakMemoryMB
     * @param numberOfESGFxVertices
     * @param numberOfESGFxEdges
     * @param numberOfESGFxTestCases
     * @param numberOfESGFxTestEvents
     * @param testCaseRecordingTimeMs
     * @param eventCoveragePercent
     * @param eventCoverageAnalysisTimeMs
     * @param testExecTimeMs
     * @param testExecPeakMemoryMB
     * @param ESGFxModelLoadTimeMs
     * @param processedProductCount
     * @param failedProductCount
     * @param folderName
     * @param SPLName
     * @param coverageType edge coverage
     */
    public static void writeDetailedPipelineMeasurementForESGFx_L234(int runID, double totalElapsedTimeMs, double testGenTimeMs,
            double transformationTimeMs, double testGenPeakMemoryMB, int numberOfESGFxVertices, int numberOfESGFxEdges, 
            int numberOfESGFxTestCases, int numberOfESGFxTestEvents, double testCaseRecordingTimeMs, double eventCoveragePercent,
            double eventCoverageAnalysisTimeMs, double testExecTimeMs, double testExecPeakMemoryMB,
            double ESGFxModelLoadTimeMs, int processedProductCount, int failedProductCount, String folderName, String SPLName,
            String coverageType) {

        // Set decimal separator to comma
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator(',');

        DecimalFormat df = new DecimalFormat("#.##", symbols);

        BufferedWriter writer = null;
        try {
            File file = new File(folderName);

            // Ensure parent directory exists (Safety check)
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            writer = new BufferedWriter(new FileWriter(file, true));

            // Fixed missing semicolon between processedProductCount and failedProductCount
            String dataRow = runID + ";" +
            		SPLName + ";" 
                    + coverageType + ";" 
                    + df.format(totalElapsedTimeMs) + ";"
                    + df.format(testGenTimeMs) + ";" 
                    + df.format(transformationTimeMs) + ";" 
                    + df.format(testGenPeakMemoryMB) + ";" 
                    + numberOfESGFxVertices + ";"
                    + numberOfESGFxEdges + ";" 
                    + numberOfESGFxTestCases + ";" 
                    + numberOfESGFxTestEvents + ";"
                    + df.format(testCaseRecordingTimeMs) + ";" 
                    + df.format(eventCoveragePercent) + ";"
                    + df.format(eventCoverageAnalysisTimeMs) + ";" 
                    + df.format(testExecTimeMs) + ";"
                    + df.format(testExecPeakMemoryMB) + ";" 
                    + df.format(ESGFxModelLoadTimeMs) + ";"
                    + processedProductCount + ";"
                    + failedProductCount
                    + "\n";

            if (file.length() > 0) {
                // Append data
                writer.append(dataRow);
            } else {
                // Write Header first
                writer.write("Run ID;" + "SPL Name;" + "Coverage Type;" + "Total Elapsed Time(ms);" + "Test Generation Time(ms);"
                        + "Transformation Time(ms);" + "Test Generation Peak Memory(MB);" + "Number of ESGFx Vertices;" 
                        + "Number of ESGFx Edges;" + "Number of ESGFx Test Cases;" + "Number of ESGFx Test Events;"
                        + "Test Case Recording Time(ms);" + "Edge Coverage(%);" 
                        + "Coverage Analysis Time(ms);" + "Test Execution Time(ms);" + "Test Execution Peak Memory(MB);"
                        + "ESGFx Model Load Time(ms);" + "Processed Products;" + "Failed Products\n");
                // Write Data
                writer.write(dataRow);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void writeDetailedPipelineMeasurementForRandomWalk(int runID, double timeElapsedTotalMs, double ESGFxModelLoadTimeMs,
            double testGenTimeMs, double testGenPeakMemoryMB,
            int numberOfVertices, int numberOfEdges, int numberOfTestCases, int numberOfTestEvents,long numberOfAbortedSequences,
            double testCaseRecordingTimeMs, double eventCoveragePercent, double eventCoverageAnalysisTimeMs,
            double edgeCoveragePercent, double edgeCoverageAnalysisTimeMs,
            double testExecTimeMs, double testExecPeakMemoryMB,
            int safetyLimitHitCount, double avgTimeOnSafetyLimitMs, double avgStepsOnSafetyLimit, double avgCoverageOnSafetyLimit,
            int processedProductCount, int failedProductCount, String folderName, String SPLName, String coverageType) {

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator(',');

        DecimalFormat df = new DecimalFormat("#.##", symbols);

        BufferedWriter writer = null;
        try {
            File file = new File(folderName);

            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            writer = new BufferedWriter(new FileWriter(file, true));

            String dataRow = runID + ";" +
            		SPLName + ";" 
                    + coverageType + ";" 
                    + df.format(timeElapsedTotalMs) + ";"
                    + df.format(ESGFxModelLoadTimeMs) + ";" 
                    + df.format(testGenTimeMs) + ";"
                    + df.format(testGenPeakMemoryMB) + ";" 
                    + numberOfVertices + ";" 
                    + numberOfEdges + ";" 
                    + numberOfTestCases + ";" 
                    + numberOfTestEvents + ";" 
                    + numberOfAbortedSequences + ";"
                    + df.format(testCaseRecordingTimeMs) + ";" 
                    + df.format(eventCoveragePercent) + ";" 
                    + df.format(eventCoverageAnalysisTimeMs) + ";" 
                    + df.format(edgeCoveragePercent) + ";" 
                    + df.format(edgeCoverageAnalysisTimeMs) + ";" 
                    + df.format(testExecTimeMs) + ";" 
                    + df.format(testExecPeakMemoryMB) + ";" 
                    + safetyLimitHitCount + ";"
                    + df.format(avgTimeOnSafetyLimitMs) + ";" 
                    + df.format(avgStepsOnSafetyLimit) + ";"
                    + df.format(avgCoverageOnSafetyLimit) + ";"
                    + processedProductCount + ";" 
                    + failedProductCount + "\n";

            if (file.length() > 0) {
                writer.append(dataRow);
            } else {
                writer.write("Run ID;" + "SPL Name;" + "Coverage Type;" + "Total Elapsed Time(ms);" + "Model Load Time(ms);"
                        + "Test Gen Time(ms);" + "Test Gen Peak Memory(MB);"
                        + "Total Vertices;" + "Total Edges;" + "Total Test Cases;" + "Total Test Events;" + "Aborted Sequences;" 
                        + "TestCase Recording Time(ms);" + "Event Coverage(%);" + "Event Coverage Analysis  Time(ms);"
                        + "Edge Coverage(%);" + "Edge Coverage Analysis  Time(ms);" + "Test Exec Time(ms);"
                        + "Test Exec Peak Memory(MB);" + "Safety Limit Hit Count;" + "Avg Time on Safety Limit(ms);"
                        + "Avg Steps on Safety Limit;" + "Avg Edge Coverage on Safety Limit(%);" 
                        + "Processed Products;" + "Failed Products\n");
                writer.write(dataRow);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void writeTotalPipelineMeasurementForEFG(int runID,double totalElapsedTimeMs, double totalTestGenTimeMs, double totalParseTimeMs,
            double totalTestGenPeakMemoryMB, int numberOfEFGVertices, int numberOfEFGEdges, int numberOfEFGTestCases,
            int numberOfEFGTestEvents, double eventCoveragePercent, double eventCoverageAnalysisTimeMs, double edgeCoveragePercent, double edgeCoverageAnalysisTimeMs,
            double totalTestExecTimeMs, double totalTestExecPeakMemoryMB, int ESGFxVertices, int ESGFxEdges,
            double ESGFxModelLoadTimeMs, int processedProductCount, int failedProductCount, String folderName,
            String SPLName, String coverageType) {

        // Set decimal separator to comma
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator(',');

        DecimalFormat df = new DecimalFormat("#.##", symbols);

        BufferedWriter writer = null;
        try {
            File file = new File(folderName);

            // Ensure parent directory exists (Safety check)
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            writer = new BufferedWriter(new FileWriter(file, true));

            String dataRow = runID + ";" 
            		+ SPLName + ";" 
                    + coverageType + ";" 
                    + df.format(totalElapsedTimeMs) + ";"
                    + df.format(totalTestGenTimeMs) + ";" 
                    + df.format(totalParseTimeMs) + ";"
                    + df.format(totalTestGenPeakMemoryMB) + ";"
                    + numberOfEFGVertices + ";" 
                    + numberOfEFGEdges + ";" 
                    + numberOfEFGTestCases + ";"
                    + numberOfEFGTestEvents + ";" 
                    + df.format(eventCoveragePercent) + ";"
                    + df.format(eventCoverageAnalysisTimeMs) + ";"
                    + df.format(edgeCoveragePercent) + ";"
                    + df.format(edgeCoverageAnalysisTimeMs) + ";" 
                    + df.format(totalTestExecTimeMs) + ";"
                    + df.format(totalTestExecPeakMemoryMB) + ";" 
                    + ESGFxVertices + ";" 
                    + ESGFxEdges + ";"
                    + df.format(ESGFxModelLoadTimeMs) + ";" 
                    + processedProductCount + ";" 
                    + failedProductCount + "\n";

            if (file.length() > 0) {
                // Append data
                writer.append(dataRow);
            } else {
                // Write Header first
                writer.write("Run ID;" + "SPL Name;" + "Coverage Type;" + "Total Elapsed Time(ms);" + "Total TestGenTime(ms);"
                		+ "Total Parse Time (ms);"
                        + "Total TestGenPeakMemory(MB);" + "Total NumberOfEFGVertices;" + "Total NumberOfEFGEdges;"
                        + "Total NumberOfEFGTestCases;" + "Total NumberOfEFGTestEvents;" + "Event Coverage(%);"
                        + "Event Coverage Analysis Time(ms);" + "Edge Coverage(%);"
                        + "Edge Coverage Analysis Time(ms);" + "Total TestExecTime(ms);" + "Total TestExecPeakMemory(MB);"
                        + "Total ESGFx_Vertices;" + "Total ESGFx_Edges;" + "Total ESGFxModelLoadTimeMs;"
                        + "Processed Products;" + "Failed Products\n");
                // Write Data
                writer.write(dataRow);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}