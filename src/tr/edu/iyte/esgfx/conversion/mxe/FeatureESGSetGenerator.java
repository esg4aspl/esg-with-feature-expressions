package tr.edu.iyte.esgfx.conversion.mxe;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import tr.edu.iyte.esg.conversion.ESGConversionUtilities;
import tr.edu.iyte.esg.model.ESG;

public class FeatureESGSetGenerator {
	
	public static Set<ESG>  createFeatureESGSet(String folderPath) {
//		System.out.println("Folder Path: " + folderPath);
		Set<ESG> featureESGSet = new LinkedHashSet<ESG>();

		File folder = new File(folderPath);
		File[] listOfFiles = folder.listFiles();

		int featureID = 1;
		for (File file : listOfFiles) {
			if (file.isFile()) {
//		    	System.out.println("File " + file.getName());
				ESG featureESG = ESGConversionUtilities.readESGFromMXEFile(folderPath + "/" + file.getName(), featureID,
						file.getName().substring(0, file.getName().length() - 4));
				featureESGSet.add(featureESG);
//		        System.out.println("Feature " + featureESG.toString());
			}
			featureID++;
		}
		
		return featureESGSet;
	}

}
