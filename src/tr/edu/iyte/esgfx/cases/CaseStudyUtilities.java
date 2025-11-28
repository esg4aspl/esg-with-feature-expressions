package tr.edu.iyte.esgfx.cases;

import java.io.File; 

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esgfx.conversion.mxe.MXEFileToESGFxConverter;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;
import tr.edu.iyte.esgfx.productconfigurationgeneration.ProductConfigurationValidator;

public class CaseStudyUtilities {

    protected static String caseStudyFolderPath;
    protected static String featureModelFilePath;
    protected static String ESGFxFilePath;

    protected static String detailedFaultDetectionResults;
    protected static String faultDetectionResultsForPerProductInSPL;

    protected static String testsequencesFolderPath;
    protected static String timemeasurementFolderPath;

    protected static String testSuiteFilePath;
    protected static String SPLName;
    
    protected static String featureESGSetFolderPath_FeatureInsertion;
    protected static String featureESGSetFolderPath_FeatureOmission;
    
    protected static String productConfigurationFilePath;
    protected static String coverageType;
    protected static int coverageLength;
    
    protected static String EFGFolderPath;
    
    protected static String DOTFolderPath;
    
    protected static String SPLSummary_TestSuite;
    protected static String SPLSummary_FaultDetection;
    
    protected static String mutantEventName;
    protected static String mutantFeatureName;
    
    protected static Set<ESG> featureESGSet;
    
    protected static String mutationOperatorName;
    
    protected static String shards_efgfilewriter;
    protected static String shards_mutantgenerator_edgeinserter;
    protected static String shards_mutantgenerator_edgeomitter;
    protected static String shards_mutantgenerator_edgeredirector;
    protected static String shards_mutantgenerator_eventinserter;
    protected static String shards_mutantgenerator_eventomitter;
    protected static String shards_productconfigurations;
    protected static String shards_testsequencegeneration;
    protected static String shards_timemeasurement;
    
    
    protected FeatureModel featureModel;
    protected ESG ESGFx;
    protected Map<String, FeatureExpression> featureExpressionMapFromFeatureModel;
    
    public static void setCoverageType() {
        
        if(coverageLength == 0) {
            coverageType = "randomwalk";
        }else if(coverageLength == 1) {
            coverageType = "eventcoverage";
        }else if(coverageLength == 2) {
            coverageType = "eventcouplecoverage";
        }else if(coverageLength == 3) {
            coverageType = "eventtriplecoverage";
        }else if(coverageLength == 4) {
            coverageType =  "eventquadruplecoverage";
        }else {
            coverageType =  "undetermined";
        }
        
        setSPLRelatedPaths();
    }
    
    private static void setSPLRelatedPaths() {
        featureESGSet = new LinkedHashSet<>();
        ESGFxFilePath = caseStudyFolderPath + SPLName + "_ESGFx.mxe";

        featureModelFilePath = caseStudyFolderPath + "configs/model.xml";

        testsequencesFolderPath = caseStudyFolderPath + "testsequences/";
        
        detailedFaultDetectionResults = testsequencesFolderPath + "faultdetection/" + SPLName + "_detailedFaultDetectionResults";
        faultDetectionResultsForPerProductInSPL = testsequencesFolderPath + "faultdetection/" + SPLName + "_faultDetectionResultsForSPL.csv";
        
        timemeasurementFolderPath = caseStudyFolderPath + "timemeasurement/";
    
        testSuiteFilePath = testsequencesFolderPath + SPLName + "_" + coverageType + ".txt";


        productConfigurationFilePath = caseStudyFolderPath + "productConfigurations_" + SPLName + ".txt";
        
        featureESGSetFolderPath_FeatureOmission = caseStudyFolderPath + "featureESGModels";
        featureESGSetFolderPath_FeatureInsertion = "files/Cases/SodaVendingMachinev2/featureESGModels";
        
        EFGFolderPath = caseStudyFolderPath + "EFGs";
        DOTFolderPath = caseStudyFolderPath + "DOTs/";
        
        SPLSummary_TestSuite =  "files/Cases/" + "SPLTestSuiteSummary.csv";
        SPLSummary_FaultDetection = "files/Cases/" + "SPLFaultDetectionSummary.csv";
//      mutationOperatorName = "";
    
        // --- SHARD PATHS CONFIGURATION ---
        // IMPORTANT: Defined with trailing slashes to ensure they are treated as directories.
        
        shards_efgfilewriter = EFGFolderPath + "/shards_efgfilewriter/";
        shards_mutantgenerator_edgeinserter = caseStudyFolderPath + "shards_mutantgenerator_edgeinserter/";
        shards_mutantgenerator_edgeomitter = caseStudyFolderPath + "shards_mutantgenerator_edgeomitter/"; // Fixed duplicate assignment
        shards_mutantgenerator_edgeredirector = caseStudyFolderPath + "shards_mutantgenerator_edgeredirector/";
        shards_mutantgenerator_eventinserter = caseStudyFolderPath + "shards_mutantgenerator_eventinserter/";
        shards_mutantgenerator_eventomitter = caseStudyFolderPath + "shards_mutantgenerator_eventomitter/";
        shards_productconfigurations = caseStudyFolderPath + "shards_productconfigurations";
        shards_testsequencegeneration = caseStudyFolderPath + "shards_testsequencegeneration/";
        shards_timemeasurement = caseStudyFolderPath + "shards_timemeasurement/" + coverageType + "/";
        
        // --- AUTOMATIC DIRECTORY CREATION ---
        // Create these folders physically to prevent IOException in writer classes
        try {
            createDir(shards_efgfilewriter);
            createDir(shards_mutantgenerator_edgeinserter);
            createDir(shards_mutantgenerator_edgeomitter);
            createDir(shards_mutantgenerator_edgeredirector);
            createDir(shards_mutantgenerator_eventinserter);
            createDir(shards_mutantgenerator_eventomitter);
            createDir(shards_productconfigurations);
            createDir(shards_testsequencegeneration);
            createDir(shards_timemeasurement);
        } catch (Exception e) {
            System.err.println("Warning: Could not create shard directories: " + e.getMessage());
        }
    }
    
    // Helper method to create directory if it doesn't exist
    private static void createDir(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    protected Map<String, FeatureExpression> generateFeatureExpressionMapFromFeatureModel(String featureModelFilePath,
            String ESGFxFilePath) throws Exception {
        MXEFileToESGFxConverter MXEFileToESGFxConverter = new MXEFileToESGFxConverter();

        
        featureModel = MXEFileToESGFxConverter.parseFeatureModel(featureModelFilePath);
        
//        System.out.println(featureModel);
        
        ESGFx = MXEFileToESGFxConverter.parseMXEFileForESGFxCreation(ESGFxFilePath);
        
        featureExpressionMapFromFeatureModel = MXEFileToESGFxConverter.getFeatureExpressionMap();
        
//        printFeatureExpressionMapFromFeatureModel(featureExpressionMapFromFeatureModel);
        return featureExpressionMapFromFeatureModel;
    }

    protected static boolean isProductConfigurationValid(FeatureModel featureModel,
            Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {

        ProductConfigurationValidator productConfigurationValidator = new ProductConfigurationValidator();
        boolean isValid = productConfigurationValidator.validate(featureModel, featureExpressionMapFromFeatureModel);
        return isValid;
    }

    /*
     * This method puts feature expression objects into an array list starting from
     * index 0 to use the indices as the variable in SAT problem
     */
    protected List<FeatureExpression> getFeatureExpressionList(
            Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {

        Set<Entry<String, FeatureExpression>> entrySet = featureExpressionMapFromFeatureModel.entrySet();
        Iterator<Entry<String, FeatureExpression>> entrySetIterator = entrySet.iterator();
        List<FeatureExpression> featureExpressionList = new ArrayList<FeatureExpression>(
                featureExpressionMapFromFeatureModel.size() + 1);

        int index = 0;
        while (entrySetIterator.hasNext()) {

            Entry<String, FeatureExpression> entry = entrySetIterator.next();
            String featureName = entry.getKey();
            FeatureExpression featureExpression = entry.getValue();
            if (!featureName.contains("!") && !(featureExpression == null)
                    && !(featureExpression.getFeature().getName() == null)) {
                featureExpressionList.add(index, featureExpression);
//              System.out.println(featureName + " - " + (index));
                index++;
            }

        }
//      System.out.println("------------------------------");

        return featureExpressionList;
    }

    protected void printFeatureExpressionList(List<FeatureExpression> featureExpressionList) {

        System.out.println("Feature Expression List");
        Iterator<FeatureExpression> featureExpressionListIterator = featureExpressionList.iterator();

        while (featureExpressionListIterator.hasNext()) {
            FeatureExpression featureExpression = featureExpressionListIterator.next();
            int index = featureExpressionList.indexOf(featureExpression);
            System.out.println(featureExpression.getFeature().getName() + " - " + (index + 1));

        }
        System.out.println("-----------------------------");

    }

    protected void printFeatureExpressionMapFromFeatureModel(
            Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {
        System.out.println("Feature Expression Map From Feature Model");
        for (Map.Entry<String, FeatureExpression> entry : featureExpressionMapFromFeatureModel.entrySet()) {
            String featureName = entry.getKey();
            FeatureExpression featureExpression = entry.getValue();
            System.out.print(featureName + " - " + featureExpression + "\n");
        }
        System.out.println("-----------------------------");

    }
}