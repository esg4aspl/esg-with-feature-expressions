package tr.edu.iyte.esgfx.cases.SVM;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement;

public class TotalTimeMeasurementApp_SVM_L2 extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {
		coverageLength = 2;
		CaseStudyUtilities_SVM.initializeFilePaths();
		
		TotalTimeMeasurement totalTimeMeasurement = new TotalTimeMeasurement();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
