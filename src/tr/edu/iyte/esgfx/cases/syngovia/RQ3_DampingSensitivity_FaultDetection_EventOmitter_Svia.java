package tr.edu.iyte.esgfx.cases.syngovia;
import tr.edu.iyte.esgfx.cases.RQ3_DampingSensitivity_FaultDetection_EdgeOmitter;
public class RQ3_DampingSensitivity_FaultDetection_EventOmitter_Svia extends RQ3_DampingSensitivity_FaultDetection_EdgeOmitter {
    public static void main(String[] args) throws Exception {
        CaseStudyUtilities_Svia.initializeFilePaths();
        new RQ3_DampingSensitivity_FaultDetection_EventOmitter_Svia().evaluateFaultDetection();
    }
}