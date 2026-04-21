package tr.edu.iyte.esgfx.cases.eMail;
import tr.edu.iyte.esgfx.cases.RQ3_DampingSensitivity_FaultDetection_EventOmitter;
public class RQ3_DampingSensitivity_FaultDetection_EdgeOmitter_eM extends RQ3_DampingSensitivity_FaultDetection_EventOmitter {
    public static void main(String[] args) throws Exception {
        CaseStudyUtilities_eM.initializeFilePaths();
        new RQ3_DampingSensitivity_FaultDetection_EdgeOmitter_eM().evaluateFaultDetection();
    }
}