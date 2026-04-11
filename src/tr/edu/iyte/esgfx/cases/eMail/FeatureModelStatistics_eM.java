package tr.edu.iyte.esgfx.cases.eMail;

import tr.edu.iyte.esgfx.cases.FeatureModelStatistics;

public class FeatureModelStatistics_eM extends CaseStudyUtilities_eM {

    public static void main(String[] args) throws Exception {

        CaseStudyUtilities_eM.initializeFilePaths();

        new FeatureModelStatistics().getFeatureModelStatistics();
        
    }

}
