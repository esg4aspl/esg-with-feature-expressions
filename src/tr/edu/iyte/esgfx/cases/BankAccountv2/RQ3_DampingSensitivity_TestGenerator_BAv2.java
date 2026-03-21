package tr.edu.iyte.esgfx.cases.BankAccountv2;
import tr.edu.iyte.esgfx.cases.RQ3_DampingSensitivity_TestGenerator;
public class RQ3_DampingSensitivity_TestGenerator_BAv2 extends RQ3_DampingSensitivity_TestGenerator {
    public static void main(String[] args) throws Exception {
        CaseStudyUtilities_BAv2.initializeFilePaths();
        new RQ3_DampingSensitivity_TestGenerator_BAv2().generateDampingSensitivityTests();
    }
}