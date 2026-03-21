package tr.edu.iyte.esgfx.cases;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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

public class UniformRandomSamplingByEnumeration extends CaseStudyUtilities {

    private static final int TARGET_SAMPLE_SIZE = 400; 

    public void writeUniformSampledEFGAndDOTFiles(int totalValidProductsInSPL) throws Exception {
        
        featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFile, ESGFxFile);
        List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);

        SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();
        ISolver solver = new ModelIterator(SolverFactory.newDefault());

        satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel, featureExpressionList);

        System.out.println("EFG&DOT FILE WRITER " + SPLName + " STARTED");
        
        //Generate TARGET_SAMPLE_SIZE unique and uniform random IDs
        Set<Integer> randomIdsSet = new HashSet<>();
        Random random = new Random(42); // Fixed seed for strict scientific reproducibility
        
        while (randomIdsSet.size() < TARGET_SAMPLE_SIZE) {
            int randomId = random.nextInt(totalValidProductsInSPL) + 1;
            randomIdsSet.add(randomId);
        }
        
        //Sort the IDs to match the SAT solver's enumeration sequence
        List<Integer> targetIds = new ArrayList<>(randomIdsSet);
        Collections.sort(targetIds);

        int productID = 0;
        int targetProductID = 0; 
        int currentTargetProductID = targetIds.get(targetProductID); 

        ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();
        
		String csvFilePathEFG = EFGFolder + SPLName + "_EFGFile_Generation_Times.csv";
		String csvFilePathDOT = DOTFolder + SPLName + "_DOTFile_Generation_Times.csv";
		String txtFilePath = caseStudyFolder + SPLName + "_ProductConfigurations.txt";
        
		try (PrintWriter csvWriter1 = new PrintWriter(new FileWriter(csvFilePathEFG));
				PrintWriter csvWriter2 = new PrintWriter(new FileWriter(csvFilePathDOT));
				PrintWriter txtWriter = new PrintWriter(new FileWriter(txtFilePath))) {
			
			csvWriter1.println("ProductID,EFG File Generation Time (ms)"); 
			csvWriter2.println("ProductID,DOT File Generation Time (ms)"); 
            
            // Iterate through the SAT space
            while (solver.isSatisfiable() && targetProductID < TARGET_SAMPLE_SIZE) {
                
                int[] model = solver.model(); 

                // Add a blocking clause to find the next model
                VecInt blockingClause = new VecInt();
                for (int i = 0; i < model.length; i++) {
                    blockingClause.push(-model[i]);
                }
                solver.addClause(blockingClause);

                // Apply configuration to the feature tree
                for (int i = 0; i < model.length; i++) {
                    FeatureExpression featureExpression = featureExpressionList.get(i);
                    featureExpression.setTruthValue(model[i] > 0);
                }

                boolean isProductConfigurationValid = isProductConfigurationValid(featureModel, featureExpressionMapFromFeatureModel);

                if (isProductConfigurationValid) {
                    productID++; 

                    // Check if the current valid product matches our pre-selected random ID
                    if (productID == currentTargetProductID) {
                        
            			int numberOfFeatures = 0;
            			
            			StringBuilder productConfiguration = new StringBuilder(ProductIDUtil.format(productID) + ": <");
            			for (int i = 0; i < model.length; i++) {
            				FeatureExpression fe = featureExpressionList.get(i);
            				String fname = fe.getFeature().getName();
            				if (model[i] > 0) {
            					fe.setTruthValue(true);
            					productConfiguration.append(fname).append(", ");
            					numberOfFeatures++;
            				} else {
            					fe.setTruthValue(false);
            				}
            			}
            			if (numberOfFeatures > 0)
            				productConfiguration.setLength(productConfiguration.length() - 2);
            			productConfiguration.append(">:").append(numberOfFeatures).append(" features");
                        
                        // Write the ID and its features to the TXT file (e.g., 1066 {FeatureA, FeatureB})
                        txtWriter.println(productConfiguration.toString());

                        int sampleNumber = targetProductID + 1;
                        String productName = ProductIDUtil.format(sampleNumber,4);

                        ESG productESGFx = productESGFxGenerator.generateProductESGFx(productID, productName, ESGFx);

    					long startTime1 = System.nanoTime();
    					ESGToEFGFileWriter.writeESGFxToEFGFile(productESGFx, productName, EFGFolder);
    					long endTime1 = System.nanoTime();
    					double timeInMilliseconds1 = (endTime1 - startTime1) / 1_000_000.0;
    					// Log timing data to CSV
    					csvWriter1.println(ProductIDUtil.format(productID) + ";" + String.valueOf(timeInMilliseconds1).replace('.', ','));

    					long startTime2 = System.nanoTime();
    					ESGFxToDOTFileConverter.buildDOTFileFromESGFx(productESGFx, DOTFolder + coverageType + "/",
    							productName);
    					long endTime2 = System.nanoTime();
    					double timeInMilliseconds2 = (endTime2 - startTime2) / 1_000_000.0;
    					csvWriter2.println(ProductIDUtil.format(productID) + ";" + String.valueOf(timeInMilliseconds2).replace('.', ','));

                        // Nullify object to prevent memory leaks (8GB RAM constraint)
                        productESGFx = null; 
                        
                        System.out.println("Progress: Sample " + sampleNumber + " generated (Matched Random ID: " + currentTargetProductID + ")");
                        
                        // Move to the next target ID
                        targetProductID++;
                        if (targetProductID < TARGET_SAMPLE_SIZE) {
                            currentTargetProductID = targetIds.get(targetProductID);
                        }
                        
                        // Force garbage collection periodically
                        System.gc(); 
                    }
                }
            }
            // Write final metadata to the TXT file
            txtWriter.println("Total Valid Products in SPL: " + totalValidProductsInSPL);
            txtWriter.println("Total Sampled Products: " + TARGET_SAMPLE_SIZE);
            txtWriter.println("Sampling Method: Uniform Random Sampling with Fixed Seed (42)");
            txtWriter.println("Note: The random IDs were generated to ensure a uniform distribution across the entire product space.");
            txtWriter.println("This approach allows for a more representative sample of the product configurations, especially in cases where the valid product space is large and complex.");
            txtWriter.println("--------------------------------------------------");
            
            txtWriter.println("Feature Model:\n " + featureModel);
            
            
         
        }
        
        System.out.println("SAMPLING AND GENERATION FINISHED PERFECTLY!");
        System.out.println("Total Valid Products Traversed: " + productID);
        System.out.println("Random IDs and Features exported to: " + txtFilePath);
        
    }
}