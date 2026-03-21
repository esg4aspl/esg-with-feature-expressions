package tr.edu.iyte.esgfx.cases;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;
import org.sat4j.tools.ModelIterator;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esgfx.conversion.dot.ESGFxToDOTFileConverter;
import tr.edu.iyte.esgfx.conversion.xml.ESGToEFGFileWriter;

import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;

public class ProductESGFxToEFGAndDOTFileWriter extends CaseStudyUtilities {

    public void writeProductESGFxToFile() throws Exception {
    	coverageLength = 2;
    	setCoverageType();
        featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFile,
                ESGFxFile);

        List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);

        SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();
        ISolver solver = new ModelIterator(SolverFactory.newDefault());

        System.out.println("EFG, DOT & CONFIG FILE WRITER " + SPLName + " STARTED");

        satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel,
                featureExpressionList);
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

        try (PrintWriter csvWriter1 = new PrintWriter(new FileWriter(csvFilePathEFG));
                PrintWriter csvWriter2 = new PrintWriter(new FileWriter(csvFilePathDOT));
                PrintWriter txtWriter = new PrintWriter(new FileWriter(txtFilePath))) {
            
            csvWriter1.println("ProductID,EFG File Generation Time (ms)"); 
            csvWriter2.println("ProductID,DOT File Generation Time (ms)"); 

            while (solver.isSatisfiable()) {

                productID++;

                int[] model = solver.model();
                for (int i = 0; i < model.length; i++) {
                    FeatureExpression featureExpression = featureExpressionList.get(i);
                    if (model[i] > 0) {
                        featureExpression.setTruthValue(true);
                    } else {
                        featureExpression.setTruthValue(false);
                    }
                }

                VecInt blockingClause = new VecInt();
                for (int i = 0; i < model.length; i++) {
                    blockingClause.push(-model[i]);
                }
                solver.addClause(blockingClause);

                boolean isProductConfigurationValid = isProductConfigurationValid(featureModel,
                        featureExpressionMapFromFeatureModel);

                if (isProductConfigurationValid) {
                    
                    String productName = ProductIDUtil.format(productID, 4);
                    
                    int numberOfFeatures = 0;
                    StringBuilder productConfiguration = new StringBuilder(productName + ": <");
                    StringBuilder individualConfigFileContent = new StringBuilder();
                    
                    for (int i = 0; i < model.length; i++) {
                        FeatureExpression fe = featureExpressionList.get(i);
                        String fname = fe.getFeature().getName();
                        
                        if (model[i] > 0) {
                            fe.setTruthValue(true);
                            productConfiguration.append(fname).append(", ");
                            individualConfigFileContent.append(fname).append("=true\n");
                            numberOfFeatures++;
                        } else {
                            fe.setTruthValue(false);
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

}