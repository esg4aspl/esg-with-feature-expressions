package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement;

public class TotalTimeMeasurementApp_Svia_L2 extends CaseStudyUtilities_Svia {

	public static void main(String[] args) throws Exception {
		coverageLength = 2;
		CaseStudyUtilities_Svia.initializeFilePaths();
		
		TotalTimeMeasurement totalTimeMeasurement = new TotalTimeMeasurement();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
