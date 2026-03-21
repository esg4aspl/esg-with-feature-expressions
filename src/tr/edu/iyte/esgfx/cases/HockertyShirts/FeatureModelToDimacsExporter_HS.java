package tr.edu.iyte.esgfx.cases.HockertyShirts;

import tr.edu.iyte.esgfx.productconfigurationgeneration.dimacs.FeatureModelToDimacsExporter;

public class FeatureModelToDimacsExporter_HS extends CaseStudyUtilities_HS {
	
	public static void main(String[] args) {
		
		CaseStudyUtilities_HS.initializeFilePaths();
		FeatureModelToDimacsExporter exporter = new FeatureModelToDimacsExporter();
		
		try {
			exporter.exportToDIMACS();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
