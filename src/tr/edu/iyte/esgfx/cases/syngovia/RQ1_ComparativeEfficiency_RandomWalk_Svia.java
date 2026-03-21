package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.RQ1_ComparativeEfficiency_RandomWalk;

public class RQ1_ComparativeEfficiency_RandomWalk_Svia extends CaseStudyUtilities_Svia {

    public static void main(String[] args) throws Exception {

    	CaseStudyUtilities_Svia.coverageLength = 0;
        CaseStudyUtilities_Svia.initializeFilePaths();

        /*
         * Random walk
         */
		RQ1_ComparativeEfficiency_RandomWalk totalTimeMeasurement = new RQ1_ComparativeEfficiency_RandomWalk();
		totalTimeMeasurement.measurePipeLineForRandomWalk();
    }
}
