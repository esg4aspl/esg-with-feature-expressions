package tr.edu.iyte.esgfx.cases.StudentAttendanceSystem;

import tr.edu.iyte.esgfx.cases.RQ1_ComparativeEfficiency_RandomWalk;

public class RQ1_ComparativeEfficiency_RandomWalk_SAS extends CaseStudyUtilities_SAS {

    public static void main(String[] args) throws Exception {

        CaseStudyUtilities_SAS.initializeFilePaths();

        /*
         * Random walk
         */
		RQ1_ComparativeEfficiency_RandomWalk totalTimeMeasurement = new RQ1_ComparativeEfficiency_RandomWalk();
		totalTimeMeasurement.measurePipeLineForRandomWalk();
    }
}
