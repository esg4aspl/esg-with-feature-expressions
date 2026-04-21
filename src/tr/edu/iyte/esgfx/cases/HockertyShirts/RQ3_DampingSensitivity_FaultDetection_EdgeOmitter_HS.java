package tr.edu.iyte.esgfx.cases.HockertyShirts;
import tr.edu.iyte.esgfx.cases.RQ3_DampingSensitivity_FaultDetection_EventOmitter;
public class RQ3_DampingSensitivity_FaultDetection_EdgeOmitter_HS extends RQ3_DampingSensitivity_FaultDetection_EventOmitter {
    public static void main(String[] args) throws Exception {
        CaseStudyUtilities_HS.initializeFilePaths();
        new RQ3_DampingSensitivity_FaultDetection_EdgeOmitter_HS().evaluateFaultDetection();
    }
}