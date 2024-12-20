package tr.edu.iyte.esgfx.cases.resultrecordingutilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

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
	
	public static void writeTotalTimeMeasurementForSPL(double time, String folderName, String SPLName) {
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(2);
		
		String timeMeasurementFile = folderName + SPLName + ".csv";

		BufferedWriter writer = null;
		try {
			File file = new File(timeMeasurementFile);
			writer = new BufferedWriter(new FileWriter(file, true));
			if (file.length() > 0) {
				writer.append(df.format(time) + "\n");
			} else {
				writer.write(SPLName + "\n");
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

}
