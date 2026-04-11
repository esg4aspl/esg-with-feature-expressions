package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.FeatureModelStatistics;

public class FeatureModelStatistics_Te extends CaseStudyUtilities_Te {

    public static void main(String[] args) throws Exception {

        CaseStudyUtilities_Te.initializeFilePaths();

        new FeatureModelStatistics().getFeatureModelStatistics();
        
    }

}
