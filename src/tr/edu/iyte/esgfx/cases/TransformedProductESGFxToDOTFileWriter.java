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
import tr.edu.iyte.esgfx.cases.Elevator.CaseStudyUtilities_El;
import tr.edu.iyte.esgfx.conversion.dot.ESGFxToDOTFileConverter;
import tr.edu.iyte.esgfx.conversion.xml.ESGToEFGFileWriter;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.eventtriplecoverage.TransformedESGFxGenerator;

public class TransformedProductESGFxToDOTFileWriter extends CaseStudyUtilities {

	public void writeTransformedProductESGFxToFile() throws Exception {

    	coverageLength = 3;
    	setCoverageType();
        featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFile,
                ESGFxFile);

        List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);

        SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();
        ISolver solver = new ModelIterator(SolverFactory.newDefault());

        System.out.println("Transformed product ESG-Fx DOT FILE WRITER " + SPLName + " STARTED");

        satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel,
                featureExpressionList);
        ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();
        TransformedESGFxGenerator transformedESGFxGenerator = new TransformedESGFxGenerator();

        int productID = 0;


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

                    
                    ESG productESGFx = productESGFxGenerator.generateProductESGFx(productID, productName, ESGFx);
                    
                    ESG transformedProductESGFx = transformedESGFxGenerator.generateTransformedESGFx(coverageLength, productESGFx);

                    ESGFxToDOTFileConverter.buildDOTFileFromESGFx(transformedProductESGFx, DOTFolder + coverageType + "/",
                            productName);

                    
                    productESGFx = null;
                    if (productID % 100 == 0) {
                        System.gc();
                    }

                } else {
                    productID--;
                }

                System.out.println("EFG, DOT & CONFIG FILE WRITER " + SPLName + " FINISHED " + productID + " products");
        }

	}

}
