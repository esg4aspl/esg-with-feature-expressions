// file: src/main/java/tr/edu/iyte/esgfx/cases/RandomWalkTestSequenceRecorder.java
package tr.edu.iyte.esgfx.cases;

import java.util.List;
import java.util.Set;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.TestSuiteFileWriter;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EdgeCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.eventtriplecoverage.TransformedESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.randomwalktesting.RandomWalkTestGenerator;

/**
 * RandomWalkTestSequenceRecorder
 *
 * For each valid product configuration:
 * 1) Builds product ESG-Fx.
 * 2) Runs a random-walk generator to obtain Set<EventSequence>.
 * 3) Computes edge coverage on the product ESG.
 * 4) Appends sequences and coverage to the suite file.
 *
 * Uses TestSuiteFileWriter overload WITHOUT coverageLength
 * to write raw random-walk sequences.
 */
public class RandomWalkTestSequenceRecorder extends CaseStudyUtilities {


    public void recordRandomWalkTestSequences() throws Exception {
        featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFilePath, ESGFxFilePath);
        printFeatureExpressionMapFromFeatureModel(featureExpressionMapFromFeatureModel);

        List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);
        printFeatureExpressionList(featureExpressionList);

        // Initialize SAT solver over the feature model
        SATSolverGenerationFromFeatureModel sat = new SATSolverGenerationFromFeatureModel();
        ISolver solver = SolverFactory.newDefault(); // single-model iteration
        sat.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel, featureExpressionList);

        int productID = 0;

        while (solver.isSatisfiable()) {
            productID++;

            // Build product configuration string and set truth values
            String productName = ProductIDUtil.format(productID);
            StringBuilder productConfiguration = new StringBuilder(productName + ": <");
            int numberOfFeatures = 0;

            int[] model = solver.model();
            for (int i = 0; i < model.length; i++) {
                FeatureExpression fe = featureExpressionList.get(i);
                if (model[i] > 0) {
                    fe.setTruthValue(true);
                    productConfiguration.append(fe.getFeature().getName()).append(", ");
                    numberOfFeatures++;
                } else {
                    fe.setTruthValue(false);
                }
            }
            if (numberOfFeatures > 0) {
                productConfiguration.setLength(productConfiguration.length() - 2);
            }
            productConfiguration.append(">:").append(numberOfFeatures).append(" features");

            // Block current model to enumerate the next
            VecInt blocking = new VecInt();
            for (int lit : model) blocking.push(-lit);
            solver.addClause(blocking);

            boolean valid = isProductConfigurationValid(featureModel, featureExpressionMapFromFeatureModel);
            if (!valid) {
                productID--;
                continue;
            }

            // Generate product ESG-Fx
            String ESGFxName = productName + productID;
            ProductESGFxGenerator productGen = new ProductESGFxGenerator();
            ESG productESG = productGen.generateProductESGFx(productID, ESGFxName, ESGFx);

            System.out.println("Product ESG-Fx");
            System.out.println(productESG.toString());
            
			TransformedESGFxGenerator transformedESGFxGenerator = new TransformedESGFxGenerator();

			ESG transformedProductESGFx = transformedESGFxGenerator.generateTransformedESGFx(coverageLength, productESG);
//			System.out.println((coverageLength -2) + " TRANSFORMED Product ESG-Fx");
//			System.out.println(transformedProductESGFx);

            // Random-walk generation on product ESG-Fx
            RandomWalkTestGenerator rw = new RandomWalkTestGenerator(transformedProductESGFx);
            rw.generateTestSequences();
            
            Set<EventSequence> tests = rw.getTestSequences();

            // Edge coverage on the product ESG
            EdgeCoverageAnalyser analyser = new EdgeCoverageAnalyser();
            double coverage = analyser.analyseEdgeCoverage(productESG, tests, featureExpressionMapFromFeatureModel);

            // Persist sequences and coverage. Use the writer overload WITHOUT coverageLength.
            TestSuiteFileWriter.writeEventSequenceSetAndCoverageAnalysisToFile(
                    testSuiteFilePath,
                    productConfiguration.toString(),
                    numberOfFeatures,
                    tests,
                    "Random-walk coverage is",
                    coverage
            );
        }

        System.out.println("Number of Products: " + productID);
    }
}

