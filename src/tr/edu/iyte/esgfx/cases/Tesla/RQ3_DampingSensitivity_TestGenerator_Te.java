package tr.edu.iyte.esgfx.cases.Tesla;
import tr.edu.iyte.esgfx.cases.RQ3_DampingSensitivity_TestGenerator;
public class RQ3_DampingSensitivity_TestGenerator_Te extends RQ3_DampingSensitivity_TestGenerator {
    public static void main(String[] args) throws Exception {
        CaseStudyUtilities_Te.initializeFilePaths();
        new RQ3_DampingSensitivity_TestGenerator_Te().generateDampingSensitivityTests();
    }
}