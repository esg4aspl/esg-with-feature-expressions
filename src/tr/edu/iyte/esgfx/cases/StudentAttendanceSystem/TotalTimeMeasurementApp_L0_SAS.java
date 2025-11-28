package tr.edu.iyte.esgfx.cases.StudentAttendanceSystem;

import tr.edu.iyte.esgfx.cases.TotalTimeMeasurement_L0;

public class TotalTimeMeasurementApp_L0_SAS extends CaseStudyUtilities_SAS {

    public static void main(String[] args) throws Exception {

    	CaseStudyUtilities_SAS.coverageLength = 0;
        CaseStudyUtilities_SAS.initializeFilePaths();

        /*
         * Random walk
         */
		TotalTimeMeasurement_L0 totalTimeMeasurement = new TotalTimeMeasurement_L0();
		totalTimeMeasurement.measureToTalTimeForEdgeCoverage();
    }
}
