package tr.edu.iyte.esgfx.cases.resultrecordingutilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Locale;
import java.text.DecimalFormatSymbols;

public class TestPipelineMeasurementWriter_ExtremeScalability {

    public static void writeDetailedPipelineMeasurementForESGFx_L1(int runID, double totalElapsedTimeMs, double satTimeMs,
            double prodGenTimeMs, double testGenTimeMs, double testGenPeakMemoryMB,
            int numberOfESGFxVertices, int numberOfESGFxEdges, long numberOfESGFxTestCases, long numberOfESGFxTestEvents, 
            double eventCoveragePercent, double eventCoverageAnalysisTimeMs, 
            double testExecTimeMs, double testExecPeakMemoryMB, int processedProductCount, int failedProductCount, 
            String folderName, String SPLName, String coverageType) {

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
                    + df.format(testGenTimeMs) + ";" 
                    + df.format(testGenPeakMemoryMB) + ";" 
                    + numberOfESGFxVertices + ";"
                    + numberOfESGFxEdges + ";" 
                    + numberOfESGFxTestCases + ";" 
                    + numberOfESGFxTestEvents + ";"
                    + df.format(eventCoveragePercent) + ";"
                    + df.format(eventCoverageAnalysisTimeMs) + ";" 
                    + df.format(testExecTimeMs) + ";"
                    + df.format(testExecPeakMemoryMB) + ";" 
                    + processedProductCount + ";"
                    + failedProductCount
                    + "\n";

            if (file.length() > 0) {
                writer.append(dataRow);
            } else {
                writer.write("Run ID;" + "SPL Name;" + "Coverage Type;" + "Total Elapsed Time(ms);" + "SAT Time(ms);"
                        + "Product Gen Time(ms);" + "Test Generation Time(ms);" 
                        + "Test Generation Peak Memory(MB);" + "Number of ESGFx Vertices;" 
                        + "Number of ESGFx Edges;" + "Number of ESGFx Test Cases;" + "Number of ESGFx Test Events;"
                        + coverageType + " Coverage(%);" + "Coverage Analysis Time(ms);" + "Test Execution Time(ms);" 
                        + "Test Execution Peak Memory(MB);" + "Processed Products;" + "Failed Products\n");
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
        
    public static void writeDetailedPipelineMeasurementForESGFx_L234(int runID,double totalElapsedTimeMs, double satTimeMs,
            double prodGenTimeMs, double transformationTimeMs, double testGenTimeMs, double testGenPeakMemoryMB,
            int numberOfESGFxVertices, int numberOfESGFxEdges, long numberOfESGFxTestCases, long numberOfESGFxTestEvents, 
            double edgeCoveragePercent, double edgeCoverageAnalysisTimeMs, 
            double testExecTimeMs, double testExecPeakMemoryMB, int processedProductCount, int failedProductCount, 
            String folderName, String SPLName, String coverageType) {

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
                    + df.format(transformationTimeMs) + ";" 
                    + df.format(testGenTimeMs) + ";" 
                    + df.format(testGenPeakMemoryMB) + ";" 
                    + numberOfESGFxVertices + ";"
                    + numberOfESGFxEdges + ";" 
                    + numberOfESGFxTestCases + ";" 
                    + numberOfESGFxTestEvents + ";"
                    + df.format(edgeCoveragePercent) + ";"
                    + df.format(edgeCoverageAnalysisTimeMs) + ";" 
                    + df.format(testExecTimeMs) + ";"
                    + df.format(testExecPeakMemoryMB) + ";" 
                    + processedProductCount + ";"
                    + failedProductCount
                    + "\n";

            if (file.length() > 0) {
                writer.append(dataRow);
            } else {
                writer.write("RunID;" + "SPL Name;" + "Coverage Type;" + "Total Elapsed Time(ms);" + "SAT Time(ms);"
                        + "Product Gen Time(ms);" + "Transformation Time(ms);" + "Test Generation Time(ms);" 
                        + "Test Generation Peak Memory(MB);" + "Number of ESGFx Vertices;" 
                        + "Number of ESGFx Edges;" + "Number of ESGFx Test Cases;" + "Number of ESGFx Test Events;"
                        + coverageType + " Coverage(%);" + "Coverage Analysis Time(ms);" + "Test Execution Time(ms);" 
                        + "Test Execution Peak Memory(MB);" + "Processed Products;" + "Failed Products\n");
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