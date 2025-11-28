package tr.edu.iyte.esgfx.cases.Elevator;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement_L0;

public class TotalTimeMeasurementApp_L0_El extends CaseStudyUtilities_El {

    public static void main(String[] args) throws Exception {

    	CaseStudyUtilities_El.coverageLength = 0;
    	CaseStudyUtilities_El.initializeFilePaths();

        /*
         * Random walk
         */
		TotalTimeMeasurement_L0 totalTimeMeasurement = new TotalTimeMeasurement_L0();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
    }
}
