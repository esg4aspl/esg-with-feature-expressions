// file: src/main/java/tr/edu/iyte/esgfx/cases/SVM/RandomWalkTestSequenceRecorderApp_SVM.java
package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.RandomWalkTestSequenceRecorder;

/**
 * App entry for SVM SPL random-walk recording.
 */
public class RandomWalkTestSequenceRecorderApp_Svia extends CaseStudyUtilities_Svia {

    public static void main(String[] args) throws Exception {
        // coverageLength kept for a uniform signature (RW does not use it)
    	CaseStudyUtilities_Svia. coverageLength = 0;
        CaseStudyUtilities_Svia.initializeFilePaths();

        // Defaults: 200 tests per product, 50 steps, fixed seed.
        RandomWalkTestSequenceRecorder rec = new RandomWalkTestSequenceRecorder();
        rec.recordRandomWalkTestSequences();

        // Or customize:
        // new RandomWalkTestSequenceRecorder(500, 100, 1337L).recordRandomWalkTestSequences();
    }
}
