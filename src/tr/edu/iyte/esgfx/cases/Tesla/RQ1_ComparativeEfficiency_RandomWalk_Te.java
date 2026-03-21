package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.RQ1_ComparativeEfficiency_RandomWalk;

public class RQ1_ComparativeEfficiency_RandomWalk_Te extends CaseStudyUtilities_Te {

    public static void main(String[] args) throws Exception {

        CaseStudyUtilities_Te.initializeFilePaths();

        /*
         * Random walk
         */
		RQ1_ComparativeEfficiency_RandomWalk totalTimeMeasurement = new RQ1_ComparativeEfficiency_RandomWalk();
		totalTimeMeasurement.measurePipeLineForRandomWalk();
    }
}
