package tr.edu.iyte.esgfx.cases.resultrecordingutilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Locale;
import java.text.DecimalFormatSymbols;

/**
 * Writer for RQ2 Extreme Scalability - EFG Baseline (UPDATED)
 * 
 * CHANGES FROM ORIGINAL:
 * 1. Added parseTimeMs parameter (GUITAR output → EventSequence parsing)
 * 2. Already had numberOfEFGVertices and numberOfEFGEdges (correct!)
 * 
 * Records throughput, failures, and coverage degradation on large SPLs
 */
public class TestPipelineMeasurementWriter_EFG_ExtremeScalability {

    /**
     * Write EFG L=2,3,4 measurements with transformation, parsing, and coverage tracking
     * Matches ESG-Fx L234 column structure for direct comparison
     */
    public static void writeDetailedPipelineMeasurementForEFG_L234(
            int runID, 
            double totalElapsedTimeMs, 
            double satTimeMs,
            double prodGenTimeMs, 
            double efgTransformationTimeMs,  // ESGFx → EFG XML writing
            double testGenTimeMs,             // GUITAR test generation  
            double parseTimeMs,               // GUITAR output parsing (ADDED)
            double testGenPeakMemoryMB,
            int numberOfEFGVertices,          // EFG vertex count (already there!)
            int numberOfEFGEdges,             // EFG edge count (already there!)
            long numberOfEFGTestCases, 
            long numberOfEFGTestEvents,
            double eventCoveragePercent,
            double eventCoverageAnalysisTimeMs,
            double edgeCoveragePercent,       // CRITICAL: Track coverage degradation
            double edgeCoverageAnalysisTimeMs,
            double testExecTimeMs, 
            double testExecPeakMemoryMB, 
            int processedProductCount, 
            int failedProductCount,           // OOM, timeout, GUITAR crashes
            String folderName, 
            String SPLName, 
            String coverageType) {

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
                    + df.format(totalElapsedTimeMs) + ";"
                    + df.format(satTimeMs) + ";" 
                    + df.format(prodGenTimeMs) + ";" 
                    + df.format(efgTransformationTimeMs) + ";"  // ESGFx → EFG
                    + df.format(testGenTimeMs) + ";"            // GUITAR generation
                    + df.format(parseTimeMs) + ";"              // GUITAR output parsing (ADDED)
                    + df.format(testGenPeakMemoryMB) + ";" 
                    + numberOfEFGVertices + ";"                 // EFG metrics
                    + numberOfEFGEdges + ";" 
                    + numberOfEFGTestCases + ";" 
                    + numberOfEFGTestEvents + ";"
                    + df.format(eventCoveragePercent) + ";"
                    + df.format(eventCoverageAnalysisTimeMs) + ";"
                    + df.format(edgeCoveragePercent) + ";"      // Coverage degradation metric
                    + df.format(edgeCoverageAnalysisTimeMs) + ";"
                    + df.format(testExecTimeMs) + ";"
                    + df.format(testExecPeakMemoryMB) + ";" 
                    + processedProductCount + ";"
                    + failedProductCount                        // Failure count
                    + "\n";

            if (file.length() == 0) {
                writer.write("RunID;SPL Name;Coverage Type;Total Elapsed Time(ms);SAT Time(ms);"
                        + "Product Gen Time(ms);EFG Transformation Time(ms);"
                        + "Test Generation Time(ms);Parse Time(ms);"  // ADDED Parse Time
                        + "Test Generation Peak Memory(MB);"
                        + "Number of EFG Vertices;Number of EFG Edges;"  // EFG metrics
                        + "Number of EFG Test Cases;Number of EFG Test Events;"
                        + "Event Coverage(%);Event Coverage Analysis Time(ms);"
                        + "Edge Coverage(%);Edge Coverage Analysis Time(ms);"
                        + "Test Execution Time(ms);Test Execution Peak Memory(MB);"
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