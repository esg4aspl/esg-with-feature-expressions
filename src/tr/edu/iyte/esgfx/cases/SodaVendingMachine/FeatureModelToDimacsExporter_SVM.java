package tr.edu.iyte.esgfx.cases.SodaVendingMachine;

import tr.edu.iyte.esgfx.productconfigurationgeneration.dimacs.FeatureModelToDimacsExporter;

public class FeatureModelToDimacsExporter_SVM extends CaseStudyUtilities_SVM {
	
	public static void main(String[] args) {
		
		CaseStudyUtilities_SVM.initializeFilePaths();
		FeatureModelToDimacsExporter exporter = new FeatureModelToDimacsExporter();
		
		try {
			exporter.exportToDIMACS();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
