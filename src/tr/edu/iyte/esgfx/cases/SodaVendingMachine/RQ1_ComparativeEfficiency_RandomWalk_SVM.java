package tr.edu.iyte.esgfx.cases.SodaVendingMachine;

import tr.edu.iyte.esgfx.cases.RQ1_ComparativeEfficiency_RandomWalk;

public class RQ1_ComparativeEfficiency_RandomWalk_SVM extends CaseStudyUtilities_SVM {

    public static void main(String[] args) throws Exception {

    	CaseStudyUtilities_SVM.coverageLength = 0;
        CaseStudyUtilities_SVM.initializeFilePaths();

        /*
         * Random walk
         */
		RQ1_ComparativeEfficiency_RandomWalk totalTimeMeasurement = new RQ1_ComparativeEfficiency_RandomWalk();
		totalTimeMeasurement.measurePipeLineForRandomWalk();
    }
}
