package tr.edu.iyte.esgfx.cases.HockertyShirts;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement;

public class TotalTimeMeasurementApp_HS_L4 extends CaseStudyUtilities_HS {

	public static void main(String[] args) throws Exception {
		
		coverageLength = 4;
		
		CaseStudyUtilities_HS.initializeFilePaths();
		
		TotalTimeMeasurement totalTimeMeasurement = new TotalTimeMeasurement();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
