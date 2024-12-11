package tr.edu.iyte.esgfx.cases.edgecoverage.HockertyShirts;

import java.util.List;
import java.util.Map;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esgfx.conversion.mxe.MXEFileToESGFxConverter;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;
import tr.edu.iyte.esgfx.productconfigurationgeneration.AutomaticProductConfigurationGenerator;
import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;

public class AutomaticProductConfigurationGeneratorApp2 extends CaseStudyUtilities_HS {

    public static void main(String[] args) throws ContradictionException, TimeoutException {

        // Initialize paths and parsers
        CaseStudyUtilities_HS.initializeFilePaths();
        MXEFileToESGFxConverter MXEFileToESGFxConverter = new MXEFileToESGFxConverter();
        FeatureModel featureModel = null;
        try {
            featureModel = MXEFileToESGFxConverter.parseFeatureModel(featureModelFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("Feature Model" + featureModel);
        System.out.println("------------------------------------------------------");

        ESG ESGFx = null;
        try {
            ESGFx = MXEFileToESGFxConverter.parseMXEFileForESGFxCreation(ESGFxFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Extract feature expressions
        Map<String, FeatureExpression> featureExpressionMapFromFeatureModel = MXEFileToESGFxConverter
                .getFeatureExpressionMap();
        AutomaticProductConfigurationGenerator automaticProductConfigurationGenerator = new AutomaticProductConfigurationGenerator();
        List<FeatureExpression> featureExpressionList = automaticProductConfigurationGenerator
                .getFeatureExpressionList(featureExpressionMapFromFeatureModel);

        // Initialize solver and add clauses
        SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();
        ISolver solver = SolverFactory.newDefault(); // No ModelIterator

        satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel,
                featureExpressionList);

        System.out.println("Product Configurations");
        int productID = 0;

        while (solver.isSatisfiable()) {
            productID++;

            // Generate product name
            String productName = "P" + (productID < 10 ? "0" : "") + productID;

            // Process solution
            String productConfiguration = processSolution(solver.model(), featureExpressionList, productName);
            ProductConfigurationFileWriter.printProductConfiragutionToFile(productConfigurationFilePath, productConfiguration);

            // Add a blocking clause to exclude the current model
            VecInt blockingClause = new VecInt();
            for (int literal : solver.model()) {
                blockingClause.push(-literal);
            }
            solver.addClause(blockingClause); // Explicitly exclude the current model
        }
    }

    private static String processSolution(int[] model, List<FeatureExpression> featureExpressionList, String productName) {
        StringBuilder productConfiguration = new StringBuilder(productName + ": <");
        int numberOfFeatures = 0;

        for (int i = 0; i < model.length; i++) {
            FeatureExpression featureExpression = featureExpressionList.get(i);
            String featureName = featureExpression.getFeature().getName();
            if (model[i] > 0) {
                featureExpression.setTruthValue(true);
                productConfiguration.append(featureName).append(", ");
                numberOfFeatures++;
            } else {
                featureExpression.setTruthValue(false);
            }
        }

        // Finalize product configuration string
        if (numberOfFeatures > 0) {
            productConfiguration.setLength(productConfiguration.length() - 2); // Remove trailing ", "
        }
        productConfiguration.append(">:").append(numberOfFeatures).append(" features");

        return productConfiguration.toString();
    }
}
