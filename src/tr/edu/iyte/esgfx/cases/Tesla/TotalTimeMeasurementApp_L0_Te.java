package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement_L0;

public class TotalTimeMeasurementApp_L0_Te extends CaseStudyUtilities_Te {

    public static void main(String[] args) throws Exception {

    	CaseStudyUtilities_Te.coverageLength = 0;
        CaseStudyUtilities_Te.initializeFilePaths();

        /*
         * Random walk
         */
		TotalTimeMeasurement_L0 totalTimeMeasurement = new TotalTimeMeasurement_L0();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
    }
}
