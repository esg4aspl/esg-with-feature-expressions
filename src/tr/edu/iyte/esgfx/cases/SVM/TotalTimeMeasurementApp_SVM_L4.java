package tr.edu.iyte.esgfx.cases.SVM;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement;

public class TotalTimeMeasurementApp_SVM_L4 extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {
		coverageLength = 4;
		CaseStudyUtilities_SVM.initializeFilePaths();
		
		TotalTimeMeasurement totalTimeMeasurement = new TotalTimeMeasurement();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
