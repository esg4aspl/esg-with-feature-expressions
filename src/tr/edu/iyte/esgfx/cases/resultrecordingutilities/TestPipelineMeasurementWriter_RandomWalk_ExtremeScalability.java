package tr.edu.iyte.esgfx.cases.resultrecordingutilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Locale;
import java.text.DecimalFormatSymbols;

/**
 * Writer for RQ2 Extreme Scalability - Random Walk Baseline
 * Records time complexity explosion (O(|V|³)) and safety limit hits
 */
public class TestPipelineMeasurementWriter_RandomWalk_ExtremeScalability {

    /**
     * Write Random Walk measurements with safety limit tracking
     * CRITICAL: Captures time complexity explosion on large SPLs
     */
    public static void writeDetailedPipelineMeasurementForRandomWalk(
            int runID,
            double totalElapsedTimeMs,
            double satTimeMs,
            double prodGenTimeMs,
            double testGenTimeMs,            // Random Walk generation time
            double testGenPeakMemoryMB,
            int numberOfVertices,
            int numberOfEdges,
            long numberOfTestCases,
            long numberOfTestEvents,
            long numberOfAbortedSequences,   // Safety limit hits
            double eventCoveragePercent,
            double eventCoverageAnalysisTimeMs,
            double edgeCoveragePercent,
            double edgeCoverageAnalysisTimeMs,
            double testExecTimeMs,
            double testExecPeakMemoryMB,
            int safetyLimitHitCount,         // How many products hit 5|V|³ limit
            double avgTimeOnSafetyLimitMs,   // Avg time spent at limit
            long avgStepsOnSafetyLimit,      // Avg steps when limit hit
            double avgCoverageAtSafetyLimit, // Coverage when aborted
            int processedProductCount,
            int failedProductCount,
            String folderName,
            String SPLName) {

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
                    SPLName + ";L0;" 
                    + df.format(totalElapsedTimeMs) + ";"
                    + df.format(satTimeMs) + ";"
                    + df.format(prodGenTimeMs) + ";"
                    + df.format(testGenTimeMs) + ";"
                    + df.format(testGenPeakMemoryMB) + ";"
                    + numberOfVertices + ";"
                    + numberOfEdges + ";"
                    + numberOfTestCases + ";"
                    + numberOfTestEvents + ";"
                    + numberOfAbortedSequences + ";"
                    + df.format(eventCoveragePercent) + ";"
                    + df.format(eventCoverageAnalysisTimeMs) + ";"
                    + df.format(edgeCoveragePercent) + ";"
                    + df.format(edgeCoverageAnalysisTimeMs) + ";"
                    + df.format(testExecTimeMs) + ";"
                    + df.format(testExecPeakMemoryMB) + ";"
                    + safetyLimitHitCount + ";"
                    + df.format(avgTimeOnSafetyLimitMs) + ";"
                    + avgStepsOnSafetyLimit + ";"
                    + df.format(avgCoverageAtSafetyLimit) + ";"
                    + processedProductCount + ";"
                    + failedProductCount
                    + "\n";

            if (file.length() == 0) {
                writer.write("RunID;SPL Name;Coverage Type;Total Elapsed Time(ms);SAT Time(ms);"
                        + "Product Gen Time(ms);Test Generation Time(ms);"
                        + "Test Generation Peak Memory(MB);Number of Vertices;Number of Edges;"
                        + "Number of Test Cases;Number of Test Events;Aborted Sequences;"
                        + "Event Coverage(%);Event Coverage Analysis Time(ms);"
                        + "Edge Coverage(%);Edge Coverage Analysis Time(ms);"
                        + "Test Execution Time(ms);Test Execution Peak Memory(MB);"
                        + "Safety Limit Hit Count;Avg Time on Safety Limit(ms);"
                        + "Avg Steps on Safety Limit;Avg Coverage at Safety Limit(%);"
                        + "Processed Products;Failed Products\n");
            }
            writer.write(dataRow);

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