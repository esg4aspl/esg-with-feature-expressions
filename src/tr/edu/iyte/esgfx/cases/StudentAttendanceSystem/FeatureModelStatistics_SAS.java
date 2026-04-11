package tr.edu.iyte.esgfx.cases.StudentAttendanceSystem;

import tr.edu.iyte.esgfx.cases.FeatureModelStatistics;

public class FeatureModelStatistics_SAS extends CaseStudyUtilities_SAS {

    public static void main(String[] args) throws Exception {

        CaseStudyUtilities_SAS.initializeFilePaths();

        new FeatureModelStatistics().getFeatureModelStatistics();
        
    }

}
