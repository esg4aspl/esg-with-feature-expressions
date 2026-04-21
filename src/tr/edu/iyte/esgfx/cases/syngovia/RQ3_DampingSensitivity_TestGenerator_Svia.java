package tr.edu.iyte.esgfx.cases.syngovia;
import tr.edu.iyte.esgfx.cases.RQ3_DampingSensitivity_TestGenerator;
public class RQ3_DampingSensitivity_TestGenerator_Svia extends RQ3_DampingSensitivity_TestGenerator {
    public static void main(String[] args) throws Exception {
        CaseStudyUtilities_Svia.initializeFilePaths();
        new RQ3_DampingSensitivity_TestGenerator_Svia().generateDampingSensitivityTests();
    }
}