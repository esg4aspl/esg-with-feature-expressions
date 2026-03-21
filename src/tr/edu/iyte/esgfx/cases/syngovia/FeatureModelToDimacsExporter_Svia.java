package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.productconfigurationgeneration.dimacs.FeatureModelToDimacsExporter;

public class FeatureModelToDimacsExporter_Svia extends CaseStudyUtilities_Svia {
	
	public static void main(String[] args) {
		
		CaseStudyUtilities_Svia.initializeFilePaths();
		FeatureModelToDimacsExporter exporter = new FeatureModelToDimacsExporter();
		
		try {
			exporter.exportToDIMACS();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
