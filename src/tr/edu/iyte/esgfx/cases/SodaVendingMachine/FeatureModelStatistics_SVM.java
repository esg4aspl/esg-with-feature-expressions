package tr.edu.iyte.esgfx.cases.SodaVendingMachine;

import tr.edu.iyte.esgfx.cases.FeatureModelStatistics;

public class FeatureModelStatistics_SVM extends CaseStudyUtilities_SVM {

    public static void main(String[] args) throws Exception {

        CaseStudyUtilities_SVM.initializeFilePaths();

        new FeatureModelStatistics().getFeatureModelStatistics();
        
    }

}
