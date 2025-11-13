package tr.edu.iyte.esgfx.cases;

import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.LinkedHashSet;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;
import org.sat4j.tools.ModelIterator;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.cases.CaseStudyUtilities;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.sequenceesgfx.EventSequenceUtilities;
import tr.edu.iyte.esgfx.mutationtesting.faultdetection.FaultDetector;
import tr.edu.iyte.esgfx.mutationtesting.mutationoperators.EventOmitter;
import tr.edu.iyte.esgfx.mutationtesting.mutationoperators.MutationOperator;
import tr.edu.iyte.esgfx.mutationtesting.resultutils.FaultDetectionResultRecorder;
import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EulerCycleGeneratorForEdgeCoverage;
import tr.edu.iyte.esgfx.testgeneration.eventtriplecoverage.TransformedESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;

public class MutantGeneratorEventOmitter extends MutantGenerator {

    public void generateMutants() throws Exception {
    	
        featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(
                featureModelFilePath, ESGFxFilePath);
       //printFeatureExpressionMapFromFeatureModel(featureExpressionMapFromFeatureModel);

        List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);
        //printFeatureExpressionList(featureExpressionList);

        // Initialize solver and add clauses
        SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();
        ISolver solver = SolverFactory.newDefault(); // keep as-is (no ModelIterator)
        satSolverGenerationFromFeatureModel.addSATClauses(
                solver, featureModel, featureExpressionMapFromFeatureModel, featureExpressionList);

        int productID = 0;
        
		int numberOfMutantsInSPL = 0;

		int numberOfDetectedMutantsInSPL_L2 = 0;
		int numberOfDetectedMutantsInSPL_L3 = 0;
		int numberOfDetectedMutantsInSPL_L4 = 0;
		
        while (solver.isSatisfiable()) {
            productID++;

            // Generate product name and configuration string
            String productName = ProductIDUtil.format(productID);
            StringBuilder productConfiguration = new StringBuilder(productName + ": <");
            int numberOfFeatures = 0;

            int[] model = solver.model();
            for (int i = 0; i < model.length; i++) {
                FeatureExpression featureExpression = featureExpressionList.get(i);
                if (model[i] > 0) {
                    String featureName = featureExpression.getFeature().getName();
                    featureExpression.setTruthValue(true);
                    productConfiguration.append(featureName).append(", ");
                    numberOfFeatures++;
                } else {
                    featureExpression.setTruthValue(false);
                }
            }
            if (numberOfFeatures > 0) {
                productConfiguration.setLength(productConfiguration.length() - 2);
            }
            productConfiguration.append(">:").append(numberOfFeatures).append(" features");

            // Block current model to find the next one
            VecInt blockingClause = new VecInt();
            for (int i = 0; i < model.length; i++) blockingClause.push(-model[i]);
            solver.addClause(blockingClause);

            boolean isProductConfigurationValid = isProductConfigurationValid(
                    featureModel, featureExpressionMapFromFeatureModel);

            if (!isProductConfigurationValid) {
                productID--;
                continue;
            }

            // Build product ESG-Fx once
            String ESGFxName = productName + Integer.toString(productID);
            ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();
            ESG productESGFx = productESGFxGenerator.generateProductESGFx(productID, ESGFxName, ESGFx);

			// Build test suites for L=2,3,4
			// Reuse detectors; do NOT recreate per mutant
			FaultDetector detectorL2 = generateFaultDetector(productESGFx, 2);
			FaultDetector detectorL3 = generateFaultDetector(productESGFx, 3);
			FaultDetector detectorL4 = generateFaultDetector(productESGFx, 4);

            // Generate mutants ON ORIGINAL product ESG-Fx
            MutationOperator mutationOperator = new EventOmitter();
            mutationOperator.generateMutantESGFxSets(productESGFx);
            //mutationOperator.reportNumberOfMutants();

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

            for (Entry<String, ESG> entry : ((EventOmitter) mutationOperator).getEventMutantMap().entrySet()) {
                String mutationElement = entry.getKey();
                ESG mutantESGFx = entry.getValue();
                int mutantID = ((ESGFx) mutantESGFx).getID();
                boolean isMutantValid = validMutants.contains(mutantESGFx);


                boolean d2 = detectorL2.isFaultDetected(mutantESGFx);
				boolean d3 = detectorL3.isFaultDetected(mutantESGFx);
				boolean d4 = detectorL4.isFaultDetected(mutantESGFx);
				
				if (d2) {
					if (isMutantValid)
						numberOfDetectedValidPerProductL2++;
					else
						numberOfDetectedInValidPerProductL2++;
				}
				if (d3) {

					if (isMutantValid)
						numberOfDetectedValidPerProductL3++;
					else
						numberOfDetectedValidPerProductL3++;
				}
				
				if (d4) {

					if (isMutantValid)
						numberOfDetectedValidPerProductL4++;
					else
						numberOfDetectedValidPerProductL4++;
				}
				String colL2, colL3, colL4;
				
				colL2 = d2 ? "TRUE" : "FALSE";
				colL3 = d3 ? "TRUE" : "FALSE";
				colL4 = d4 ? "TRUE" : "FALSE";
				
				// for each product's mutants
				FaultDetectionResultRecorder.writeDetailedFaultDetectionResultL234(
						detailedFaultDetectionResults + "_EventOmitter", productID, productConfiguration.toString(),
						mutationOperator.getName(), mutationElement, mutantID, isMutantValid, colL2, colL3, colL4);
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

			// Per-product summary with per-L counts + per-L percentages for each operator
			FaultDetectionResultRecorder.writeFaultDetectionResultsForPerProductSPL(faultDetectionResultsForPerProductInSPL,
					mutationOperator.getName(), productID, validMutants.size(), invalidMutants.size(),
					numberOfDetectedValidPerProductL2, numberOfDetectedInValidPerProductL2, percentagePerProductL2,
					numberOfDetectedValidPerProductL3, numberOfDetectedInValidPerProductL3, percentagePerProductL3,
					numberOfDetectedValidPerProductL4, numberOfDetectedInValidPerProductL4, percentagePerProductL4);
		} // endwhile
		
		double percentageInSPLL2 = percentageOfFaultDetection(numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L2);
		double percentageInSPLL3 = percentageOfFaultDetection(numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L3);
		double percentageInSPLL4 = percentageOfFaultDetection(numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L4);

		FaultDetectionResultRecorder.writeFaultDetectionResultsForSPL(SPLSummary_FaultDetection, SPLName,
				"Event Omitter", numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L2, percentageInSPLL2,
				numberOfDetectedMutantsInSPL_L3, percentageInSPLL3, numberOfDetectedMutantsInSPL_L4, percentageInSPLL4);

	}
}
