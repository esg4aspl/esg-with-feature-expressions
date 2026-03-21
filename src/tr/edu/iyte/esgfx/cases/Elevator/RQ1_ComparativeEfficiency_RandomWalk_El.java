package tr.edu.iyte.esgfx.cases.Elevator;

import tr.edu.iyte.esgfx.cases.RQ1_ComparativeEfficiency_RandomWalk;

public class RQ1_ComparativeEfficiency_RandomWalk_El extends CaseStudyUtilities_El {

	public static void main(String[] args) throws Exception {
    	CaseStudyUtilities_El.initializeFilePaths();

        /*
         * Random walk
         */
		RQ1_ComparativeEfficiency_RandomWalk totalTimeMeasurement = new RQ1_ComparativeEfficiency_RandomWalk();
		totalTimeMeasurement.measurePipeLineForRandomWalk();

	}

}
