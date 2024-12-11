package tr.edu.iyte.esgfx.cases.edgecoverage.SVM;

import tr.edu.iyte.esgfx.cases.edgecoverage.TotalTimeMeasurement;

public class TotalTimeMeasurementApp_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_SVM.initializeFilePaths();
		
		TotalTimeMeasurement totalTimeMeasurement = new TotalTimeMeasurement();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
