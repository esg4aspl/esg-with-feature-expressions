package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.RQ2_ExtremeScalability_L234;


public class TotalTimeMeasurementApp_L4_Svia extends CaseStudyUtilities_Svia {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_Svia.coverageLength = 4;
		CaseStudyUtilities_Svia.initializeFilePaths();
		
		/*
		 * Event quadruple coverage
		 */
		RQ2_ExtremeScalability_L234 totalTimeMeasurement = new RQ2_ExtremeScalability_L234();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
	}

}
