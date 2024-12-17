package tr.edu.iyte.esgfx.cases.edgecoverage;

import java.util.List;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;

import org.sat4j.specs.ISolver;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.cases.CaseStudyUtilities;
import tr.edu.iyte.esgfx.cases.resultrecordingutilities.TestSequenceGenerationTimeMeasurementWriter;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;

import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EulerCycleGeneratorForEdgeCoverage;
import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;

public class TotalTimeMeasurement extends CaseStudyUtilities {

	public void measureToTalTimeForEdgeCoverage() throws Exception {
		

		featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFilePath,
				ESGFxFilePath);

		List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);

		double startTime1 = System.nanoTime();
		// Initialize solver and add clauses
		SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();
		ISolver solver = SolverFactory.newDefault(); // No ModelIterator
		satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel,
				featureExpressionList);

		int productID = 0;
		while (solver.isSatisfiable()) {
			
			int[] model = solver.model();
			for (int i = 0; i < model.length; i++) {
				FeatureExpression featureExpression = featureExpressionList.get(i);
				if (model[i] > 0) {
					featureExpression.setTruthValue(true);
				} else {
					featureExpression.setTruthValue(false);
				}
			}

			// Add a blocking clause to exclude the current model
			VecInt blockingClause = new VecInt();
			for (int literal : solver.model()) {
				blockingClause.push(-literal);
			}
			solver.addClause(blockingClause); // Explicitly exclude the current model

			boolean isProductConfigurationValid = isProductConfigurationValid(featureModel,
					featureExpressionMapFromFeatureModel);

			if (isProductConfigurationValid) {
				productID++;
				
				// Generate product name
				String productName = "P" + (productID < 10 ? "0" : "") + productID;
				String ESGFxName = productName + Integer.toString(productID);

				ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();
				ESG productESGFx = productESGFxGenerator.generateProductESGFx(productID, ESGFxName, ESGFx);

				ESG stronglyConnectedBalancedESGFx = StronglyConnectedBalancedESGFxGeneration
						.getStronglyConnectedBalancedESGFxGeneration(productESGFx);

				EulerCycleGeneratorForEdgeCoverage eulerCycleGeneratorForEdgeCoverage = new EulerCycleGeneratorForEdgeCoverage();
				eulerCycleGeneratorForEdgeCoverage.generateEulerCycle(stronglyConnectedBalancedESGFx);
				List<Vertex> eulerCycle = eulerCycleGeneratorForEdgeCoverage.getEulerCycle();

				EulerCycleToTestSequenceGenerator eulerCycleToTestSequenceGenerator = new EulerCycleToTestSequenceGenerator();
				eulerCycleToTestSequenceGenerator.CESgenerator(eulerCycle);
			}

		}
		double stopTime1 = System.nanoTime();
		double timeElapsed1 = (stopTime1 - startTime1) / (double) 1000000;
		System.out.println("Execution time of all products test sequence generation in miliseconds  : " + timeElapsed1);
		TestSequenceGenerationTimeMeasurementWriter.writeTotalTimeMeasurementForSPL(timeElapsed1,
				timemeasurementFolderPath, SPLName);
	}

}
