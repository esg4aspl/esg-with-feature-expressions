package tr.edu.iyte.esgfx.cases.Elevator;
import tr.edu.iyte.esgfx.cases.RQ3_DampingSensitivity_FaultDetection_EdgeOmitter;
public class RQ3_DampingSensitivity_FaultDetection_EdgeOmitter_El extends RQ3_DampingSensitivity_FaultDetection_EdgeOmitter {
    public static void main(String[] args) throws Exception {
        CaseStudyUtilities_El.initializeFilePaths();
        new RQ3_DampingSensitivity_FaultDetection_EdgeOmitter_El().evaluateFaultDetection();
    }
}