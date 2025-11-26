package tr.edu.iyte.esgfx.cases.resultrecordingutilities;

import java.io.BufferedWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Locale;

import java.text.DecimalFormatSymbols;

public class TestSequenceGenerationTimeMeasurementWriter {

	public static void writeTimeMeasurement(double time, String folderName, String ESGFxName) {
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(2);

		String timeMeasurementFile = folderName + ESGFxName + ".csv";

		BufferedWriter writer = null;
		try {
			File file = new File(timeMeasurementFile);
			writer = new BufferedWriter(new FileWriter(file, true));
			if (file.length() > 0) {
				writer.append(df.format(time) + "\n");
			} else {
				writer.write(ESGFxName + "\n");
				writer.append(df.format(time) + "\n");

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void writeTotalTimeMeasurementForSPL(double time, int processedProductCount, String folderName,
			String SPLName, String coverageType) {

		// Set decimal separator to comma
		DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
		symbols.setDecimalSeparator(',');

		DecimalFormat df = new DecimalFormat("#.##", symbols);

//        String timeMeasurementFile = folderName + SPLName + "_" + coverageType + ".csv";

		BufferedWriter writer = null;
		try {
			File file = new File(folderName);

			// Ensure parent directory exists (Safety check)
			if (file.getParentFile() != null) {
				file.getParentFile().mkdirs();
			}

			writer = new BufferedWriter(new FileWriter(file, true));

			if (file.length() > 0) {
				// Append data: Time;Count
				writer.append(
						SPLName + ";" + coverageType + ";" + df.format(time) + ";" + processedProductCount + "\n");
			} else {
				// Write Header first
				writer.write("SPL Name;Coverage Type;Time(ms);Processed Products\n");
				// Write Data: Name;Type;Time;Count
				writer.write(SPLName + ";" + coverageType + ";" + df.format(time) + ";" + processedProductCount + "\n");
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

	public static void writeDetailedTimeMeasurement(double timeElapsedTotalMs, double satTimeMs, double prodGenTimeMs,
			double testGenTimeMs, int processedProductCount, String folderName, String SPLName, String coverageType) {

		// Set decimal separator to comma
		DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
		symbols.setDecimalSeparator(',');

		DecimalFormat df = new DecimalFormat("#.##", symbols);

//        String timeMeasurementFile = folderName + SPLName + "_" + coverageType + ".csv";

		BufferedWriter writer = null;
		try {
			File file = new File(folderName);

			// Ensure parent directory exists (Safety check)
			if (file.getParentFile() != null) {
				file.getParentFile().mkdirs();
			}

			writer = new BufferedWriter(new FileWriter(file, true));

			if (file.length() > 0) {
				// Append data: Time;Count
				writer.append(SPLName + ";" + coverageType + ";" + df.format(timeElapsedTotalMs) + ";"
						+ df.format(satTimeMs) + ";" + df.format(prodGenTimeMs) + ";" + df.format(testGenTimeMs) + ";"
						+ processedProductCount + "\n");
			} else {
				// Write Header first
				writer.write(
						"SPL Name;"
						+ "Coverage Type;"
						+ "Total Time(ms);"
						+ "SAT Time(ms);"
						+ "ProductESGGeneration Time(ms);"
						+ "Test Generation Time(ms);"
						+ "Processed Products\n");
				// Write Data: Name;Type;Time;Count
				writer.write(SPLName + ";" 
						+ coverageType + ";" 
						+ df.format(timeElapsedTotalMs) + ";"
						+ df.format(satTimeMs) + ";" 
						+ df.format(prodGenTimeMs) + ";" 
						+ df.format(testGenTimeMs) + ";"
						+ processedProductCount + "\n");
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
