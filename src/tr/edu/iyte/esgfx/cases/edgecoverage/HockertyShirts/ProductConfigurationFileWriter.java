package tr.edu.iyte.esgfx.cases.edgecoverage.HockertyShirts;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class ProductConfigurationFileWriter {
	
	
	public static void printProductConfiragutionToFile(String filePath, String productConfiguration) {
		 try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
		        writer.write(productConfiguration);
		        writer.newLine();
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		
	}

}
