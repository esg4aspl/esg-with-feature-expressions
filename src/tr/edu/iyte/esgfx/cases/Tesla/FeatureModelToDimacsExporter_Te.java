package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.productconfigurationgeneration.dimacs.FeatureModelToDimacsExporter;

public class FeatureModelToDimacsExporter_Te extends CaseStudyUtilities_Te {
	
	public static void main(String[] args) {
		
		CaseStudyUtilities_Te.initializeFilePaths();
		FeatureModelToDimacsExporter exporter = new FeatureModelToDimacsExporter();
		
		try {
			exporter.exportToDIMACS();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
