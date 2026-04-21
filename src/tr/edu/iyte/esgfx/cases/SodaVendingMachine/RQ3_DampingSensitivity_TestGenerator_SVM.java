package tr.edu.iyte.esgfx.cases.SodaVendingMachine;
import tr.edu.iyte.esgfx.cases.RQ3_DampingSensitivity_TestGenerator;
public class RQ3_DampingSensitivity_TestGenerator_SVM extends RQ3_DampingSensitivity_TestGenerator {
    public static void main(String[] args) throws Exception {
        CaseStudyUtilities_SVM.initializeFilePaths();
        new RQ3_DampingSensitivity_TestGenerator_SVM().generateDampingSensitivityTests();
    }
}