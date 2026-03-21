package tr.edu.iyte.esgfx.cases.Tesla;
import tr.edu.iyte.esgfx.cases.RQ3_DampingSensitivity_FaultDetection_EdgeOmitter;
public class RQ3_DampingSensitivity_FaultDetection_EventOmitter_Te extends RQ3_DampingSensitivity_FaultDetection_EdgeOmitter {
    public static void main(String[] args) throws Exception {
        CaseStudyUtilities_Te.initializeFilePaths();
        new RQ3_DampingSensitivity_FaultDetection_EventOmitter_Te().evaluateFaultDetection();
    }
}