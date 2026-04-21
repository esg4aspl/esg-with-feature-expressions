package tr.edu.iyte.esgfx.cases.HockertyShirts;
import tr.edu.iyte.esgfx.cases.RQ3_DampingSensitivity_FaultDetection_EdgeOmitter;
public class RQ3_DampingSensitivity_FaultDetection_EventOmitter_HS extends RQ3_DampingSensitivity_FaultDetection_EdgeOmitter {
    public static void main(String[] args) throws Exception {
        CaseStudyUtilities_HS.initializeFilePaths();
        new RQ3_DampingSensitivity_FaultDetection_EventOmitter_HS().evaluateFaultDetection();
    }
}