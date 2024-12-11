package tr.edu.iyte.esgfx.mutationtesting.resultutils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FaultDetectionResultRecorder {

	private static String[] detailedFaultDetectionResults = { "Product ID", "Features", "Mutation Operator", "Mutation Element", "Mutant ID",
			"Is Mutant Valid?", "Is Fault Detected?" };
	
	private static String[] newColumns = { "Mutation Operator", "Product ID", "Number of Valid Mutants", "Number of Invalid Mutants", "Number of Detected Valid Mutants", "Number of Detected Invalid Mutants", "Total Number of Mutants", "Total Number of Detected Faults" };


	public static void writeDetailedFaultDetectionResult(String fileName, int productId, String features, String mutationOperator,
			String mutationElement, int mutantId, boolean isMutantValid, boolean isFaultDetected) {
		File csvFile = new File(fileName + ".csv");
		boolean fileJustCreated = false;

		if (!csvFile.exists()) {
			try {
				csvFile.createNewFile();
				fileJustCreated = true;
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}

		try (FileWriter writer = new FileWriter(csvFile, true)) {
			if (fileJustCreated) {
				// Write the header
				writer.append(String.join(",", detailedFaultDetectionResults));
				writer.append("\n");
			}

			// Write the data
			String row = String.join(",", String.valueOf(productId), features, mutationOperator, mutationElement,
					String.valueOf(mutantId), String.valueOf(isMutantValid), String.valueOf(isFaultDetected));
			writer.append(row);
			writer.append("\n");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    public static void writeFaultDetectionResultsForSPL(String fileName, String mutationOperator, int productId, int numValidMutants, int numInvalidMutants, int numDetectedValidMutants, int numDetectedInvalidMutants, int totalNumMutants, int totalNumDetectedFaults) {
        File csvFile = new File(fileName);
        boolean fileJustCreated = false;

        if (!csvFile.exists()) {
            try {
                csvFile.createNewFile();
                fileJustCreated = true;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        try (FileWriter writer = new FileWriter(csvFile, true)) {
            if (fileJustCreated) {
                // Write the header
                writer.append(String.join(",", newColumns));
                writer.append("\n");
            }

            // Write the data
            String row = String.join(",", mutationOperator, String.valueOf(productId), String.valueOf(numValidMutants), String.valueOf(numInvalidMutants), String.valueOf(numDetectedValidMutants), String.valueOf(numDetectedInvalidMutants), String.valueOf(totalNumMutants), String.valueOf(totalNumDetectedFaults));
            writer.append(row);
            writer.append("\n");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	
}
