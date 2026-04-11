package tr.edu.iyte.esgfx.cases.Elevator;
import tr.edu.iyte.esgfx.cases.RQ3_DampingSensitivity_FaultDetection_EventOmitter;
public class RQ3_DampingSensitivity_FaultDetection_EventOmitter_El extends CaseStudyUtilities_El {
    public static void main(String[] args) throws Exception {
        CaseStudyUtilities_El.initializeFilePaths();
        new RQ3_DampingSensitivity_FaultDetection_EventOmitter().evaluateFaultDetection();
    }
}