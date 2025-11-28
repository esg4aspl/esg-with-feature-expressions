package tr.edu.iyte.esgfx.cases.eMail;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement_L0;

public class TotalTimeMeasurementApp_L0_eM extends CaseStudyUtilities_eM {

    public static void main(String[] args) throws Exception {

    	CaseStudyUtilities_eM.coverageLength = 0;
    	CaseStudyUtilities_eM.initializeFilePaths();

        /*
         * Random walk
         */
		TotalTimeMeasurement_L0 totalTimeMeasurement = new TotalTimeMeasurement_L0();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
    }
}
