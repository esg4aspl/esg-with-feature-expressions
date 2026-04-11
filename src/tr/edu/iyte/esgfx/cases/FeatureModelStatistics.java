package tr.edu.iyte.esgfx.cases;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import tr.edu.iyte.esgfx.model.featuremodel.Feature;

public class FeatureModelStatistics extends CaseStudyUtilities {

    public void getFeatureModelStatistics() throws Exception {

        featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFile, ESGFxFile);

        if (featureModel == null) {
            throw new Exception("Feature model could not be loaded!");
        }

        int totalFeatures = 0;
        int abstractFeatures = 0;
        int concreteFeatures = 0;
        int mandatoryFeatures = 0;
        int optionalFeatures = 0;

        for (Feature f : featureModel.getFeatureSet()) {
            totalFeatures++;
            if (f.isAbstract()) {
                abstractFeatures++;
            } else {
                concreteFeatures++;
            }

            if (f.isMandatory()) {
                mandatoryFeatures++;
            } else {
                optionalFeatures++;
            }
        }

        int orRelatedFeatures = countRelatedFeatures(featureModel.getORFeatures());
        int xorRelatedFeatures = countRelatedFeatures(featureModel.getXORFeatures());
        int andRelatedFeatures = countRelatedFeatures(featureModel.getChildANDFeatures(featureModel.getRoot()) != null ? 
                featureModel.getORFeatures() : null); // Adjust based on your AND logic if needed, using general count

        // Manually count AND related if it's stored differently or just iterate map
        int andRelatedFeaturesActual = 0;
        try {
             java.lang.reflect.Field andField = featureModel.getClass().getDeclaredField("andFeatures");
             andField.setAccessible(true);
             Map<Feature, Set<Feature>> andMap = (Map<Feature, Set<Feature>>) andField.get(featureModel);
             andRelatedFeaturesActual = countRelatedFeatures(andMap);
        } catch (Exception e) {
             // Fallback
        }

        int implicationConstraints = featureModel.getImpConstraints().size() + featureModel.getIffConstraints().size();
        int connectorConstraints = featureModel.getConnConstraints().size();

        double percentMandatoryToTotal = totalFeatures > 0 ? ((double) mandatoryFeatures / totalFeatures) * 100 : 0.0;
        double percentMandatoryToOptional = optionalFeatures > 0 ? ((double) mandatoryFeatures / optionalFeatures) * 100 : 0.0;
        double percentMandatoryToOR = orRelatedFeatures > 0 ? ((double) mandatoryFeatures / orRelatedFeatures) * 100 : 0.0;
        double percentMandatoryToXOR = xorRelatedFeatures > 0 ? ((double) mandatoryFeatures / xorRelatedFeatures) * 100 : 0.0;
        double percentORToXOR = xorRelatedFeatures > 0 ? ((double) orRelatedFeatures / xorRelatedFeatures) * 100 : 0.0;

        writeStatisticsToCSV(
                totalFeatures, abstractFeatures, concreteFeatures,
                mandatoryFeatures, optionalFeatures,
                orRelatedFeatures, xorRelatedFeatures, andRelatedFeaturesActual,
                implicationConstraints, connectorConstraints,
                percentMandatoryToTotal, percentMandatoryToOptional,
                percentMandatoryToOR, percentMandatoryToXOR, percentORToXOR
        );
    }

    private int countRelatedFeatures(Map<Feature, Set<Feature>> relationshipMap) {
        if (relationshipMap == null) return 0;
        
        int count = 0;
        for (Map.Entry<Feature, Set<Feature>> entry : relationshipMap.entrySet()) {
            Feature parent = entry.getKey();
            if (!parent.isAbstract()) {
                count++;
            }
            for (Feature child : entry.getValue()) {
                if (!child.isAbstract()) {
                    count++;
                }
            }
        }
        return count;
    }

    private void writeStatisticsToCSV(
            int total, int abs, int conc, int mand, int opt,
            int orRel, int xorRel, int andRel,
            int impCons, int connCons,
            double pMandTotal, double pMandOpt, double pMandOR, double pMandXOR, double pORXOR) {

        String outputFile = "files/Cases/CaseStudies_FeatureModel_Summary.csv";
        File file = new File(outputFile);
        boolean isNewFile = !file.exists();

        try {
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
                if (isNewFile) {
                    writer.println("SPL_Name,TotalFeatures,AbstractFeatures,ConcreteFeatures," +
                            "MandatoryFeatures,OptionalFeatures,OR_Related,XOR_Related,AND_Related," +
                            "ImplicationConstraints,ConnectorConstraints," +
                            "Mandatory_to_Total_%,Mandatory_to_Optional_%,Mandatory_to_OR_%," +
                            "Mandatory_to_XOR_%,OR_to_XOR_%");
                }

                writer.printf(java.util.Locale.US, "%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.2f,%.2f,%.2f,%.2f,%.2f%n",
                        SPLName != null ? SPLName : "UnknownSPL",
                        total, abs, conc, mand, opt,
                        orRel, xorRel, andRel,
                        impCons, connCons,
                        pMandTotal, pMandOpt, pMandOR, pMandXOR, pORXOR);
            }
            System.out.println("Feature model statistics successfully written to " + outputFile);
        } catch (Exception e) {
            System.err.println("Error writing statistics to CSV: " + e.getMessage());
        }
    }
}