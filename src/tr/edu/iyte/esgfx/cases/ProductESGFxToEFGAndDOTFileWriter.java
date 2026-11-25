package tr.edu.iyte.esgfx.cases;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;


import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esgfx.conversion.dot.ESGFxToDOTFileConverter;
import tr.edu.iyte.esgfx.conversion.xml.ESGToEFGFileWriter;

import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;

public class ProductESGFxToEFGAndDOTFileWriter extends CaseStudyUtilities {

    public void writeProductESGFxToFile() throws Exception {
    	coverageLength = 2;
    	setCoverageType();
        featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFile,
                ESGFxFile);

        List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);

        System.out.println("EFG, DOT & CONFIG FILE WRITER " + SPLName + " STARTED");

        // The products are enumerated from the DIMACS file of the feature model:
        // one product for every distinct assignment to the concrete features.
        ProductSpace productSpace = ProductSpace.open(DIMACSFile, DIMACSMappingFile, featureModelFile,
                featureExpressionMapFromFeatureModel, mandatoryFeatures);
        ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();

        String csvFilePathEFG = EFGFolder + SPLName + "_EFGFile_Generation_Times.csv";
        String csvFilePathDOT = DOTFolder + SPLName + "_DOTFile_Generation_Times.csv";
        String txtFilePath = caseStudyFolder + SPLName + "_ProductConfigurations.txt";

        String productConfigsFolder = caseStudyFolder + "productConfigurations/";

        // The drivers read every file of these folders, so the product files of an
        // earlier product set must not stay in them. They are moved, not deleted.
        String archiveFolder = caseStudyFolder + "productFilesBefore_"
                + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()) + "/";
        moveProductFiles(new File(productConfigsFolder), ".config", new File(archiveFolder + "productConfigurations"));
        moveProductFiles(new File(DOTFolder + coverageType), ".dot", new File(archiveFolder + "DOTs_" + coverageType));
        moveProductFiles(new File(EFGFolder), ".efg", new File(archiveFolder + "EFGs"));
        File configDir = new File(productConfigsFolder);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        int productID = 0;

        try (PrintWriter csvWriter1 = new PrintWriter(new FileWriter(csvFilePathEFG));
                PrintWriter csvWriter2 = new PrintWriter(new FileWriter(csvFilePathDOT));
                PrintWriter txtWriter = new PrintWriter(new FileWriter(txtFilePath))) {
            
            csvWriter1.println("ProductID,EFG File Generation Time (ms)"); 
            csvWriter2.println("ProductID,DOT File Generation Time (ms)"); 

            while (productSpace.next()) {

                productID++;
                productSpace.exclude();

                boolean isProductConfigurationValid = isProductConfigurationValid(featureModel,
                        featureExpressionMapFromFeatureModel);

                if (isProductConfigurationValid) {
                    
                    String productName = ProductIDUtil.format(productID, 4);
                    
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
                    
                    if (numberOfFeatures > 0)
                        productConfiguration.setLength(productConfiguration.length() - 2);
                    productConfiguration.append(">:").append(numberOfFeatures).append(" features");
                    
                    txtWriter.println(productConfiguration.toString());
                    
                    File individualConfigFile = new File(productConfigsFolder + productName + ".config");
                    try (PrintWriter configWriter = new PrintWriter(new FileWriter(individualConfigFile))) {
                        configWriter.print(individualConfigFileContent.toString());
                    }
                    
                    ESG productESGFx = productESGFxGenerator.generateProductESGFx(productID, productName, ESGFx);

                    long startTime1 = System.nanoTime();
                    ESGToEFGFileWriter.writeESGFxToEFGFile(productESGFx, productName, EFGFolder);
                    long endTime1 = System.nanoTime();
                    double timeInMilliseconds1 = (endTime1 - startTime1) / 1_000_000.0;
                    csvWriter1.println(ProductIDUtil.format(productID) + ";" + String.valueOf(timeInMilliseconds1).replace('.', ','));

                    long startTime2 = System.nanoTime();
                    ESGFxToDOTFileConverter.buildDOTFileFromESGFx(productESGFx, DOTFolder + coverageType + "/",
                            productName);
                    long endTime2 = System.nanoTime();
                    double timeInMilliseconds2 = (endTime2 - startTime2) / 1_000_000.0;
                    csvWriter2.println(ProductIDUtil.format(productID) + ";" + String.valueOf(timeInMilliseconds2).replace('.', ','));
                    
                    productESGFx = null;
                    if (productID % 100 == 0) {
                        System.gc();
                    }

                } else {
                    productID--;
                }

                System.out.println("EFG, DOT & CONFIG FILE WRITER " + SPLName + " FINISHED " + productID + " products");

            }
            
            txtWriter.println("Total Valid Products in SPL: " + productID);
            txtWriter.println("Total Sampled Products: " + productID);
            txtWriter.println("--------------------------------------------------");
        }

    }

    /** Moves the files with the given extension out of a folder, if there are any. */
    private static void moveProductFiles(File folder, String extension, File target) throws java.io.IOException {
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(extension));
        if (files == null || files.length == 0) {
            return;
        }
        target.mkdirs();
        for (File file : files) {
            java.nio.file.Files.move(file.toPath(), new File(target, file.getName()).toPath());
        }
        System.out.println("Moved " + files.length + " " + extension + " files of " + folder + " to " + target);
    }
}
