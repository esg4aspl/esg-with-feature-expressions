package tr.edu.iyte.esgfx.cases.edgecoverage;

import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.tools.ModelIterator;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.cases.CaseStudyUtilities;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;
import tr.edu.iyte.esgfx.testgeneration.TestSuiteFileWriter;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EdgeCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EulerCycleGeneratorForEdgeCoverage;
import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;

public class TestSequenceRecorder extends CaseStudyUtilities {

	public void recordTestSequences()
			throws Exception {
		featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFilePath,
				ESGFxFilePath);
		printFeatureExpressionMapFromFeatureModel(featureExpressionMapFromFeatureModel);

		List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);
		printFeatureExpressionList(featureExpressionList);

		SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();
		ISolver solver = new ModelIterator(SolverFactory.newDefault());
		try {
			satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel,
					featureExpressionMapFromFeatureModel, featureExpressionList);
		} catch (ContradictionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int productID = 0;
		while (solver.isSatisfiable()) {
			int[] model = solver.model();
			for (int i = 0; i < model.length; i++) {
				FeatureExpression featureExpression = featureExpressionList.get(i);
//				String featureName = featureExpression.getFeature().getName();
				if (model[i] > 0) {
//					System.out.println(featureName + " = true");
					featureExpression.setTruthValue(true);
				} else {
//					System.out.println(featureName + " = false");
					featureExpression.setTruthValue(false);
				}
			}
//			System.out.println("-----------------------------------");

			// Add a clause to block the current model to find the next one
			VecInt blockingClause = new VecInt();
			for (int i = 0; i < model.length; i++) {
				blockingClause.push(-model[i]);
			}
			solver.addClause(blockingClause);

			boolean isProductConfigurationValid = isProductConfigurationValid(featureModel,
					featureExpressionMapFromFeatureModel);

			if (isProductConfigurationValid) {
				productID++;
				String productName = "P";
				if (productID < 10)
					productName = "P0";

				String ESGFxName = productName + Integer.toString(productID);

				String productConfiguration = ESGFxName + ": <";
				int numberOfFeatures = 0;
				for (Entry<String, FeatureExpression> entry : featureExpressionMapFromFeatureModel.entrySet()) {
//					System.out.print(entry.getKey() + " - " + entry.getValue().evaluate() + "\n");
					if (entry.getValue().evaluate() == true) {
						productConfiguration += entry.getKey() + ", ";
						numberOfFeatures++;
					}
				}
				productConfiguration = productConfiguration.substring(0, productConfiguration.length() - 2);
				productConfiguration += ">";

				ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();
				ESG productESGFx = productESGFxGenerator.generateProductESGFx(productID, ESGFxName, ESGFx);

				ESG stronglyConnectedBalancedESGFx = StronglyConnectedBalancedESGFxGeneration
						.getStronglyConnectedBalancedESGFxGeneration(productESGFx);

				EulerCycleGeneratorForEdgeCoverage eulerCycleGeneratorForEdgeCoverage = new EulerCycleGeneratorForEdgeCoverage();
				eulerCycleGeneratorForEdgeCoverage.generateEulerCycle(stronglyConnectedBalancedESGFx);
				List<Vertex> eulerCycle = eulerCycleGeneratorForEdgeCoverage.getEulerCycle();
//			System.out.println("Euler Cycle: " + eulerCycle);
				EulerCycleToTestSequenceGenerator eulerCycleToTestSequenceGenerator = new EulerCycleToTestSequenceGenerator();
				Set<EventSequence> CESsOfESG = eulerCycleToTestSequenceGenerator.CESgenerator(eulerCycle);

				EdgeCoverageAnalyser edgeCoverageAnalyser = new EdgeCoverageAnalyser();
//				edgeCoverageAnalyser.esgEventSequenceSetPrinter(CESsOfESG);

				double coverage = edgeCoverageAnalyser.analyseEdgeCoverage(stronglyConnectedBalancedESGFx, CESsOfESG,
						featureExpressionMapFromFeatureModel);

				TestSuiteFileWriter.writeEventSequenceSetAndEdgeCoverageAnalysisToFile(testSuiteFilePath_edgeCoverage,
						productConfiguration, numberOfFeatures, CESsOfESG, coverage);
			}

		}
		System.out.println("Number of Products: " + productID);
	}

}
