package tr.edu.iyte.esgfx.cases.Elevator;

import tr.edu.iyte.esgfx.cases.FeatureModelStatistics;

public class FeatureModelStatistics_El extends CaseStudyUtilities_El {

    public static void main(String[] args) throws Exception {

        CaseStudyUtilities_El.initializeFilePaths();

        new FeatureModelStatistics().getFeatureModelStatistics();
        
    }

}
