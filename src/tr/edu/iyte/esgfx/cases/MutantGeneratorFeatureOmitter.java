package tr.edu.iyte.esgfx.cases;

import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;


import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.cases.CaseStudyUtilities;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.mutationtesting.faultdetection.FaultDetector;
import tr.edu.iyte.esgfx.mutationtesting.mutationoperators.FeatureOmitter;
import tr.edu.iyte.esgfx.mutationtesting.mutationoperators.MutationOperator;
import tr.edu.iyte.esgfx.mutationtesting.resultutils.FaultDetectionResultRecorder;
import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EulerCycleGeneratorForEdgeCoverage;
import tr.edu.iyte.esgfx.testgeneration.eventtriplecoverage.TransformedESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;

public class MutantGeneratorFeatureOmitter extends CaseStudyUtilities {

	public void generateMutants() throws Exception {
		featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFilePath,
				ESGFxFilePath);
		printFeatureExpressionMapFromFeatureModel(featureExpressionMapFromFeatureModel);

		List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);
		printFeatureExpressionList(featureExpressionList);

		// Initialize solver and add clauses
		SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();
		ISolver solver = SolverFactory.newDefault(); // No ModelIterator
		satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel,
				featureExpressionList);

		int productID = 0;
		while (solver.isSatisfiable()) {
			productID++;
			
			// Generate product name
			String productName = ProductIDUtil.format(productID);
			
			StringBuilder productConfiguration = new StringBuilder(productName + ": <");
			int numberOfFeatures = 0;
			
			int[] model = solver.model();
			for (int i = 0; i < model.length; i++) {
				FeatureExpression featureExpression = featureExpressionList.get(i);
				if (model[i] > 0) {
					String featureName = featureExpression.getFeature().getName();
//					System.out.println(featureName + " = true");
					featureExpression.setTruthValue(true);
					productConfiguration.append(featureName).append(", ");
					numberOfFeatures++;
				} else {
//					System.out.println(featureName + " = false");
					featureExpression.setTruthValue(false);
				}
			}
			// Finalize product configuration string
			if (numberOfFeatures > 0) {
				productConfiguration.setLength(productConfiguration.length() - 2); // Remove trailing ", "
			}
			productConfiguration.append(">:").append(numberOfFeatures).append(" features");			

			// Add a clause to block the current model to find the next one
			VecInt blockingClause = new VecInt();
			for (int i = 0; i < model.length; i++) {
				blockingClause.push(-model[i]);
			}
			solver.addClause(blockingClause);

			boolean isProductConfigurationValid = isProductConfigurationValid(featureModel,
					featureExpressionMapFromFeatureModel);

			if (isProductConfigurationValid) {
				
				String ESGFxName = productName + Integer.toString(productID);

				ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();
				ESG productESGFx = productESGFxGenerator.generateProductESGFx(productID, ESGFxName, ESGFx);

				TransformedESGFxGenerator transformedESGFxGenerator = new TransformedESGFxGenerator();
				ESG transformedProductESGFx = transformedESGFxGenerator.generateTransformedESGFx(coverageLength,
						productESGFx);

				ESG stronglyConnectedBalancedESGFx = StronglyConnectedBalancedESGFxGeneration
						.getStronglyConnectedBalancedESGFxGeneration(transformedProductESGFx);

				EulerCycleGeneratorForEdgeCoverage eulerCycleGeneratorForEdgeCoverage = new EulerCycleGeneratorForEdgeCoverage();
				eulerCycleGeneratorForEdgeCoverage.generateEulerCycle(stronglyConnectedBalancedESGFx);
				List<Vertex> eulerCycle = eulerCycleGeneratorForEdgeCoverage.getEulerCycle();
//			System.out.println("Euler Cycle: " + eulerCycle);
				EulerCycleToTestSequenceGenerator eulerCycleToTestSequenceGenerator = new EulerCycleToTestSequenceGenerator();
				Set<EventSequence> CESsOfESG = eulerCycleToTestSequenceGenerator.CESgenerator(eulerCycle);

				MutationOperator mutationOperator = new FeatureOmitter();
				((FeatureOmitter) mutationOperator).setFeatureESGSet(featureESGSet);
				((FeatureOmitter) mutationOperator).setFeatureExpressionMap(featureExpressionMapFromFeatureModel);

				mutationOperator.generateMutantESGFxSets(transformedProductESGFx);
				mutationOperator.reportNumberOfMutants();

				Set<ESG> mutantESGFxSet = mutationOperator.getValidMutantESGFxSet();
				mutantESGFxSet.addAll(mutationOperator.getInvalidMutantESGFxSet());

				int totalFaultCount = 0;
				int validMutantFaultCount = 0;
				int invalidMutantFaultCount = 0;
				for (Entry<String, ESG> entry : ((FeatureOmitter) mutationOperator).getFeatureNameMutantMap()
						.entrySet()) {
					FaultDetector faultDetector = new FaultDetector(CESsOfESG);
					String mutationElement = entry.getKey();
					ESG mutantESGFx = entry.getValue();
					int mutantID = ((ESGFx) mutantESGFx).getID();
//					System.out.println("Edges: " + mutationElement + " Mutant: " + mutantID);

					boolean isFaultDetected = faultDetector.isFaultDetected(mutantESGFx);
					if (isFaultDetected) {
						totalFaultCount++;
						if (mutationOperator.getValidMutantESGFxSet().contains(mutantESGFx)) {
							validMutantFaultCount++;
						} else {
							invalidMutantFaultCount++;
						}
					}
//					System.out.println(" isFaultDetected: " + isFaultDetected);
//					System.out.println();

					boolean isMutantValid = mutationOperator.getValidMutantESGFxSet().contains(mutantESGFx);
//					FaultDetectionResultRecorder.writeDetailedFaultDetectionResult(
//							detailedFaultDetectionResults + "_FeatureOmitter", productID, productConfiguration.toString(),
//							coverageType,mutationOperator.getName(), mutationElement, mutantID, isMutantValid, isFaultDetected);
				}

				FaultDetectionResultRecorder.writeFaultDetectionResultsForSPL(faultDetectionResultsForPerProductInSPL,
						coverageType,mutationOperator.getName(), productID, mutationOperator.getValidMutantESGFxSet().size(),
						mutationOperator.getInvalidMutantESGFxSet().size(), validMutantFaultCount,
						invalidMutantFaultCount, mutantESGFxSet.size(), totalFaultCount);
				System.out.println("Mutant count: " + mutantESGFxSet.size());
				System.out.println("Fault count: " + totalFaultCount);

			}else {
				productID--;
			}
		}
	}

}
