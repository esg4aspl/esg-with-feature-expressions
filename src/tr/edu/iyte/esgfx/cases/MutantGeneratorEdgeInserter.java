package tr.edu.iyte.esgfx.cases;

import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.LinkedHashSet;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;
import org.sat4j.tools.ModelIterator;

import tr.edu.iyte.esg.model.ESG;

import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

import tr.edu.iyte.esgfx.mutationtesting.faultdetection.FaultDetector;
import tr.edu.iyte.esgfx.mutationtesting.mutationoperators.MutationOperator;
import tr.edu.iyte.esgfx.mutationtesting.mutationoperators.EdgeInserter;
import tr.edu.iyte.esgfx.mutationtesting.resultutils.FaultDetectionResultRecorder;
import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;

public class MutantGeneratorEdgeInserter extends MutantGenerator {

	public void generateMutants() throws Exception {

		featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFilePath,
				ESGFxFilePath);

		List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);

		SATSolverGenerationFromFeatureModel sat = new SATSolverGenerationFromFeatureModel();
		ISolver solver = new ModelIterator(SolverFactory.newDefault());
		sat.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel, featureExpressionList);

		int productID = 0;

		int numberOfMutantsInSPL = 0;

		int numberOfDetectedMutantsInSPL_L2 = 0;
		int numberOfDetectedMutantsInSPL_L3 = 0;
		int numberOfDetectedMutantsInSPL_L4 = 0;

		while (solver.isSatisfiable()) {
			productID++;
			String productName = ProductIDUtil.format(productID);

			StringBuilder productConfiguration = new StringBuilder(productName + ": <");
			int numberOfFeatures = 0;

			int[] model = solver.model();
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

			// Block current model
			VecInt blocking = new VecInt();
			for (int i = 0; i < model.length; i++)
				blocking.push(-model[i]);
			solver.addClause(blocking);

			if (!isProductConfigurationValid(featureModel, featureExpressionMapFromFeatureModel)) {
				productID--;
				continue;
			}
			// Build product ESG-Fx once
			String ESGFxName = productName + productID;
			ProductESGFxGenerator productGen = new ProductESGFxGenerator();
			ESG productESGFx = productGen.generateProductESGFx(productID, ESGFxName, ESGFx);

			// Build test suites for L=2,3,4
			// Reuse detectors; do NOT recreate per mutant
			FaultDetector detectorL2 = generateFaultDetector(productESGFx, 2);
			FaultDetector detectorL3 = generateFaultDetector(productESGFx, 3);
			FaultDetector detectorL4 = generateFaultDetector(productESGFx, 4);

			// Generate mutants on original product ESG-Fx
			MutationOperator mutationOperator = new EdgeInserter();
			mutationOperator.generateMutantESGFxSets(productESGFx);

			Set<ESG> validMutants = mutationOperator.getValidMutantESGFxSet();
			Set<ESG> invalidMutants = mutationOperator.getInvalidMutantESGFxSet();
			Set<ESG> allMutants = new LinkedHashSet<>();
			allMutants.addAll(validMutants);
			allMutants.addAll(invalidMutants);
			int numberOfAllMutantsCurrentProduct = allMutants.size();
//			int numberOfValidMutantsCurrentProduct = validMutants.size();
//			int numberOfInvalidMutantsCurrentProduct = invalidMutants.size();

			numberOfMutantsInSPL += numberOfAllMutantsCurrentProduct;

			int numberOfDetectedValidPerProductL2 = 0;
			int numberOfDetectedInValidPerProductL2 = 0;

			int numberOfDetectedValidPerProductL3 = 0;
			int numberOfDetectedInValidPerProductL3 = 0;

			int numberOfDetectedValidPerProductL4 = 0;
			int numberOfDetectedInValidPerProductL4 = 0;

			for (Entry<String, ESG> e : ((EdgeInserter) mutationOperator).getEdgeMutantMap().entrySet()) {
				String mutationElement = e.getKey();
				ESG mutantESGFx = e.getValue();

				int mutantID = ((ESGFx) mutantESGFx).getID();
				boolean isValidMutant = validMutants.contains(mutantESGFx);

				boolean d2 = detectorL2.isFaultDetected(mutantESGFx);
				boolean d3 = detectorL3.isFaultDetected(mutantESGFx);
				boolean d4 = detectorL4.isFaultDetected(mutantESGFx);

				if (d2) {
					if (isValidMutant)
						numberOfDetectedValidPerProductL2++;
					else
						numberOfDetectedInValidPerProductL2++;
				}
				if (d3) {

					if (isValidMutant)
						numberOfDetectedValidPerProductL3++;
					else
						numberOfDetectedValidPerProductL3++;
				}
				if (d4) {

					if (isValidMutant)
						numberOfDetectedValidPerProductL4++;
					else
						numberOfDetectedValidPerProductL4++;
				}

				if ((!d2 || !d3 || !d4) && !(mutationElementSet.contains(mutationElement))) {
					mutationElementSet.add(mutationElement);
					String colL2, colL3, colL4;

					colL2 = d2 ? "TRUE" : "FALSE";
					colL3 = d3 ? "TRUE" : "FALSE";
					colL4 = d4 ? "TRUE" : "FALSE";

					// for each product's mutants
					FaultDetectionResultRecorder.writeDetailedFaultDetectionResultL234(
							detailedFaultDetectionResults + "_EdgeInserter", productID, productConfiguration.toString(),
							mutationOperator.getName(), mutationElement, mutantID, isValidMutant, colL2, colL3, colL4);
				}
			} // endfor
			int numberOfDetectedPerProductL2 = numberOfDetectedValidPerProductL2 + numberOfDetectedInValidPerProductL2;
			int numberOfDetectedPerProductL3 = numberOfDetectedValidPerProductL3 + numberOfDetectedInValidPerProductL3;
			int numberOfDetectedPerProductL4 = numberOfDetectedValidPerProductL4 + numberOfDetectedInValidPerProductL4;

			numberOfDetectedMutantsInSPL_L2 += numberOfDetectedPerProductL2;
			numberOfDetectedMutantsInSPL_L3 += numberOfDetectedPerProductL3;
			numberOfDetectedMutantsInSPL_L4 += numberOfDetectedPerProductL4;

			double percentagePerProductL2 = percentageOfFaultDetection(numberOfAllMutantsCurrentProduct,
					numberOfDetectedPerProductL2);
			double percentagePerProductL3 = percentageOfFaultDetection(numberOfAllMutantsCurrentProduct,
					numberOfDetectedPerProductL3);
			double percentagePerProductL4 = percentageOfFaultDetection(numberOfAllMutantsCurrentProduct,
					numberOfDetectedPerProductL4);

			double max = 100.00;

			if ((percentagePerProductL2 < max) || (percentagePerProductL3 < max) || (percentagePerProductL4 < max)) {
				// Per-product summary with per-L counts + per-L percentages for each operator
				FaultDetectionResultRecorder.writeFaultDetectionResultsForPerProductSPL(
						faultDetectionResultsForPerProductInSPL, mutationOperator.getName(), productID,
						validMutants.size(), invalidMutants.size(), numberOfDetectedValidPerProductL2,
						numberOfDetectedInValidPerProductL2, percentagePerProductL2, numberOfDetectedValidPerProductL3,
						numberOfDetectedInValidPerProductL3, percentagePerProductL3, numberOfDetectedValidPerProductL4,
						numberOfDetectedInValidPerProductL4, percentagePerProductL4);
			}

		} // endwhile

		double percentageInSPLL2 = percentageOfFaultDetection(numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L2);
		double percentageInSPLL3 = percentageOfFaultDetection(numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L3);
		double percentageInSPLL4 = percentageOfFaultDetection(numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L4);

		FaultDetectionResultRecorder.writeFaultDetectionResultsForSPL(SPLSummary_FaultDetection, SPLName,
				"Edge Inserter", numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L2, percentageInSPLL2,
				numberOfDetectedMutantsInSPL_L3, percentageInSPLL3, numberOfDetectedMutantsInSPL_L4, percentageInSPLL4);

	}

}
