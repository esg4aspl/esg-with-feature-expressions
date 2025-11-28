package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement_L0;

public class TotalTimeMeasurementApp_L0_Svia extends CaseStudyUtilities_Svia {

    public static void main(String[] args) throws Exception {

    	CaseStudyUtilities_Svia.coverageLength = 0;
        CaseStudyUtilities_Svia.initializeFilePaths();

        /*
         * Random walk
         */
		TotalTimeMeasurement_L0 totalTimeMeasurement = new TotalTimeMeasurement_L0();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
    }
}
