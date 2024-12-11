package tr.edu.iyte.esgfx.cases.edgecoverage.HockertyShirts;

import tr.edu.iyte.esgfx.cases.edgecoverage.TotalTimeMeasurement;

public class TotalTimeMeasurementApp_HS extends CaseStudyUtilities_HS {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_HS.initializeFilePaths();
		
		TotalTimeMeasurement totalTimeMeasurement = new TotalTimeMeasurement();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
