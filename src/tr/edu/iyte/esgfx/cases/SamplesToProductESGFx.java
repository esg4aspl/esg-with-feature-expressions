package tr.edu.iyte.esgfx.cases;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esgfx.conversion.dot.ESGFxToDOTFileConverter;
import tr.edu.iyte.esgfx.conversion.xml.ESGToEFGFileWriter;
import tr.edu.iyte.esgfx.model.featuremodel.Feature;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;

public class SamplesToProductESGFx extends CaseStudyUtilities {

    public void writeProductESGFxToFile() throws Exception {
        featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFile, ESGFxFile);
        List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);

        System.out.println("EFG&DOT FILE WRITER " + SPLName + " STARTED");

        Map<Integer, String> dimacsMapping = new HashMap<>();
        if (new File(DIMACSMappingFile).exists()) {
            List<String> mappingLines = Files.readAllLines(Paths.get(DIMACSMappingFile));
            for (String line : mappingLines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("ID:")) {
                    continue;
                }
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    dimacsMapping.put(Integer.parseInt(parts[0].trim()), parts[1].trim());
                }
            }
        } else {
            throw new Exception("DIMACS Mapping file not found at: " + DIMACSMappingFile);
        }

        List<List<Integer>> samples = new ArrayList<>();
        if (new File(SamplesFile).exists()) {
            List<String> sampleLines = Files.readAllLines(Paths.get(SamplesFile));
            for (String line : sampleLines) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                List<Integer> sample = new ArrayList<>();
                for (String part : parts) {
                    try {
                        int literal = Integer.parseInt(part);
                        if (literal != 0) {
                            sample.add(literal);
                        }
                    } catch (NumberFormatException e) {
                        // Ignored
                    }
                }
                if (!sample.isEmpty()) {
                    samples.add(sample);
                }
            }
        } else {
            throw new Exception("Samples file not found at: " + SamplesFile);
        }

        System.out.println("Total samples to process: " + samples.size());

        ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();

        String csvFilePathEFG = EFGFolder + SPLName + "_EFGFile_Generation_Times.csv";
        String csvFilePathDOT = DOTFolder + SPLName + "_DOTFile_Generation_Times.csv";
        String txtFilePath = caseStudyFolder + SPLName + "_ProductConfigurations.txt";

        String productConfigsFolder = caseStudyFolder + "productConfigurations/";
        File configDir = new File(productConfigsFolder);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        int productID = 0;
        int validProducts = 0;

        try (PrintWriter csvWriter1 = new PrintWriter(new FileWriter(csvFilePathEFG));
             PrintWriter csvWriter2 = new PrintWriter(new FileWriter(csvFilePathDOT));
             PrintWriter txtWriter = new PrintWriter(new FileWriter(txtFilePath))) {

            csvWriter1.println("ProductID;EFG File Generation Time (ms)");
            csvWriter2.println("ProductID;DOT File Generation Time (ms)");

            for (List<Integer> sample : samples) {
                productID++;

                for (FeatureExpression featureExpression : featureExpressionMapFromFeatureModel.values()) {
                    Feature feature = featureExpression.getFeature();
                    if (feature != null && feature.isMandatory()) {
                        featureExpression.setTruthValue(true);
                    } else {
                        featureExpression.setTruthValue(false);
                    }
                }

                for (int literal : sample) {
                    int varId = Math.abs(literal);
                    boolean isTrue = (literal > 0);
                    String featureName = dimacsMapping.get(varId);

                    if (featureName != null && featureExpressionMapFromFeatureModel.containsKey(featureName)) {
                        featureExpressionMapFromFeatureModel.get(featureName).setTruthValue(isTrue);
                    }
                }

                if (isRootFeatureInFeatureESGFx(featureModel, featureExpressionMapFromFeatureModel)) {
                    Feature root = featureModel.getRoot();
                    if (root != null) {
                        FeatureExpression rootExpr = featureExpressionMapFromFeatureModel.get(root.getName());
                        if (rootExpr != null) {
                            rootExpr.setTruthValue(true);
                        }
                    }
                }

                boolean changed;
                do {
                    changed = false;
                    for (Feature child : featureModel.getFeatureSet()) {
                        Feature parent = child.getParent();
                        if (parent != null) {
                            FeatureExpression childExpr = featureExpressionMapFromFeatureModel.get(child.getName());
                            FeatureExpression parentExpr = featureExpressionMapFromFeatureModel.get(parent.getName());

                            if (childExpr != null && parentExpr != null) {
                                if (childExpr.evaluate() && !parentExpr.evaluate()) {
                                    parentExpr.setTruthValue(true);
                                    changed = true;
                                }

                                if (parentExpr.evaluate() && child.isMandatory() && !featureModel.isORFeature(parent) && !featureModel.isXORFeature(parent)) {
                                    if (!childExpr.evaluate()) {
                                        childExpr.setTruthValue(true);
                                        changed = true;
                                    }
                                }
                            }
                        }
                    }
                } while (changed);

                boolean isProductConfigurationValid = isProductConfigurationValid(featureModel, featureExpressionMapFromFeatureModel);

                if (isProductConfigurationValid) {
                    validProducts++;

                    String productName = ProductIDUtil.format(validProducts, 3);

                    int numberOfFeatures = 0;
                    StringBuilder productConfiguration = new StringBuilder(productName + ": <");
                    StringBuilder individualConfigFileContent = new StringBuilder();

                    for (FeatureExpression fe : featureExpressionList) {
                        String fname = fe.getFeature().getName();
                        if (fe.evaluate()) {
                            productConfiguration.append(fname).append(", ");
                            individualConfigFileContent.append(fname).append("=true\n");
                            numberOfFeatures++;
                        } else {
                            individualConfigFileContent.append(fname).append("=false\n");
                        }
                    }

                    if (numberOfFeatures > 0) {
                        productConfiguration.setLength(productConfiguration.length() - 2);
                    }
                    productConfiguration.append(">:").append(numberOfFeatures).append(" features");

                    txtWriter.println(productConfiguration.toString());

                    File individualConfigFile = new File(productConfigsFolder + productName + ".config");
                    try (PrintWriter configWriter = new PrintWriter(new FileWriter(individualConfigFile))) {
                        configWriter.print(individualConfigFileContent.toString());
                    }

                    ESG productESGFx = productESGFxGenerator.generateProductESGFx(validProducts, productName, ESGFx);

                    long startTime1 = System.nanoTime();
                    ESGToEFGFileWriter.writeESGFxToEFGFile(productESGFx, productName, EFGFolder);
                    long endTime1 = System.nanoTime();
                    double timeInMilliseconds1 = (endTime1 - startTime1) / 1_000_000.0;
                    csvWriter1.println(ProductIDUtil.format(validProducts) + ";" + String.valueOf(timeInMilliseconds1).replace('.', ','));

                    long startTime2 = System.nanoTime();
                    ESGFxToDOTFileConverter.buildDOTFileFromESGFx(productESGFx, DOTFolder + coverageType + "/", productName);
                    long endTime2 = System.nanoTime();
                    double timeInMilliseconds2 = (endTime2 - startTime2) / 1_000_000.0;
                    csvWriter2.println(ProductIDUtil.format(validProducts) + ";" + String.valueOf(timeInMilliseconds2).replace('.', ','));

                    productESGFx = null;
                    System.gc();

                } else {
                    System.out.println("Warning: Product " + productID + " failed validation.");
                }
            }

            System.out.println("EFG&DOT FILE WRITER " + SPLName + " FINISHED " + validProducts + " products");

            txtWriter.println("Total Valid Products in SPL: " + validProducts);
            txtWriter.println("Total Sampled Products read from UniGen: " + productID);
            txtWriter.println("--------------------------------------------------");
        }
    }

    private boolean isRootFeatureInFeatureESGFx(FeatureModel featureModel, Map<String, FeatureExpression> featureExpressionMap) {
        boolean isRootInMap = false;

        Iterator<Entry<String, FeatureExpression>> entrySetIterator = featureExpressionMap.entrySet().iterator();
        while (entrySetIterator.hasNext()) {
            Entry<String, FeatureExpression> entry = entrySetIterator.next();
            FeatureExpression value = entry.getValue();
            Feature feature = value.getFeature();
            
            if (featureModel.getRoot() != null && feature.equals(featureModel.getRoot())) {
                isRootInMap = true;
                break;
            }
        }

        return isRootInMap;
    }
}