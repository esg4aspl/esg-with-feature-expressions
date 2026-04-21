package tr.edu.iyte.esgfx.cases.StudentAttendanceSystem;
import tr.edu.iyte.esgfx.cases.RQ3_DampingSensitivity_FaultDetection_EventOmitter;
public class RQ3_DampingSensitivity_FaultDetection_EdgeOmitter_SAS extends RQ3_DampingSensitivity_FaultDetection_EventOmitter {
    public static void main(String[] args) throws Exception {
        CaseStudyUtilities_SAS.initializeFilePaths();
        new RQ3_DampingSensitivity_FaultDetection_EdgeOmitter_SAS().evaluateFaultDetection();
    }
}