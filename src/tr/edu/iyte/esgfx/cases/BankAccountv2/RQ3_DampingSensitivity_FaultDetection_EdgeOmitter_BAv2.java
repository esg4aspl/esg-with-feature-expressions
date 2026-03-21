package tr.edu.iyte.esgfx.cases.BankAccountv2;
import tr.edu.iyte.esgfx.cases.RQ3_DampingSensitivity_FaultDetection_EdgeOmitter;
public class RQ3_DampingSensitivity_FaultDetection_EdgeOmitter_BAv2 extends RQ3_DampingSensitivity_FaultDetection_EdgeOmitter {
    public static void main(String[] args) throws Exception {
        CaseStudyUtilities_BAv2.initializeFilePaths();
        new RQ3_DampingSensitivity_FaultDetection_EdgeOmitter_BAv2().evaluateFaultDetection();
    }
}