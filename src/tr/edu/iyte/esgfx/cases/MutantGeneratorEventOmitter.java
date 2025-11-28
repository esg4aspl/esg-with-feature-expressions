package tr.edu.iyte.esgfx.cases;

import java.util.List;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;
import org.sat4j.tools.ModelIterator;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

import tr.edu.iyte.esgfx.mutationtesting.faultdetection.FaultDetector;
import tr.edu.iyte.esgfx.mutationtesting.mutationoperators.EventOmitter;
import tr.edu.iyte.esgfx.mutationtesting.resultutils.FaultDetectionResultRecorder;
import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;

public class MutantGeneratorEventOmitter extends MutantGenerator {

	public void generateMutants() throws Exception {

	    

        featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFilePath,
                ESGFxFilePath);
        List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);

        SATSolverGenerationFromFeatureModel sat = new SATSolverGenerationFromFeatureModel();
        ISolver solver = new ModelIterator(SolverFactory.newDefault());
        sat.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel, featureExpressionList);

        int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
        int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));

        int handledProducts = 0;
        int productID = 0;
        int numberOfMutantsInSPL = 0;

        // Counters for L0-L4
        int numberOfDetectedMutantsInSPL_L0 = 0;
        int numberOfDetectedMutantsInSPL_L1 = 0;
        int numberOfDetectedMutantsInSPL_L2 = 0;
        int numberOfDetectedMutantsInSPL_L3 = 0;
        int numberOfDetectedMutantsInSPL_L4 = 0;

        // Reusable Generator
        ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();

        while (solver.isSatisfiable()) {
            productID++;

            // --- Product Configuration Setup ---
            String productName = ProductIDUtil.format(productID);
            // (String building logic kept simple for brevity, logic remains same)
            
            int[] model = solver.model();
            VecInt blockingClause = new VecInt();
            for (int i = 0; i < model.length; i++) {
                // Update feature expressions based on model
                FeatureExpression fe = featureExpressionList.get(i);
                if (model[i] > 0) fe.setTruthValue(true);
                else fe.setTruthValue(false);
                
                blockingClause.push(-model[i]);
            }
            solver.addClause(blockingClause);

            boolean isProductConfigurationValid = isProductConfigurationValid(featureModel, featureExpressionMapFromFeatureModel);

            if (!isProductConfigurationValid) {
                // productID--; 
                continue;
            }

            // --- SHARD GATE ---
            if (((productID - 1) % N_SHARDS) != CURRENT_SHARD) {
                continue;
            }
            
            handledProducts++;
            String ESGFxName = productName + productID;
            
            // 1. Generate Product ESG (Base for all mutants)
            ESG productESGFx = productESGFxGenerator.generateProductESGFx(productID, ESGFxName, ESGFx);
            List<Vertex> productESGFxVertices = productESGFx.getRealVertexList();
            
            // Global mutant count updates (Add only once per product)
            numberOfMutantsInSPL += productESGFxVertices.size();
            
            EventOmitter eventOmitter = new EventOmitter();
            int localMutantID = 0;

            // =================================================================================
            // RAM OPTIMIZATION: LOOP INVERSION STRATEGY
            // We create detectors ONE BY ONE, use them, and destroy them immediately.
            // =================================================================================

            // ----------------------------- LEVEL 0 -----------------------------
            FaultDetector detectorL0 = generateFaultDetector(productESGFx, 0);
            long execTimeCurrentProductL0 = 0;
            localMutantID = 0; // Reset ID for consistent iteration

				for (Vertex eventToOmit : productESGFxVertices) {
					localMutantID++;
					ESG mutant = eventOmitter.createSingleMutant(productESGFx, eventToOmit, localMutantID);

					// Warmup & Measure
					runDetector(detectorL0, mutant);
					execTimeCurrentProductL0 += measureTime(detectorL0, mutant);

					if (detectorL0.isFaultDetected(mutant)) {
						numberOfDetectedMutantsInSPL_L0++;
					}
					mutant = null; // Destroy mutant immediately
				}
			
            totalExecTimeNanosL0 += execTimeCurrentProductL0;
            detectorL0 = null; // DESTROY DETECTOR L0 (Free RAM)
            System.gc(); // Suggest GC to clean up L0 mess

            // ----------------------------- LEVEL 1 -----------------------------
            FaultDetector detectorL1 = generateFaultDetector(productESGFx, 1);
            long execTimeCurrentProductL1 = 0;
            localMutantID = 0;

			for (Vertex eventToOmit : productESGFxVertices) {
				localMutantID++;
				ESG mutant = eventOmitter.createSingleMutant(productESGFx, eventToOmit, localMutantID);

					// Warmup & Measure
					runDetector(detectorL1, mutant);
					execTimeCurrentProductL1 += measureTime(detectorL1, mutant);

					if (detectorL1.isFaultDetected(mutant)) {
						numberOfDetectedMutantsInSPL_L1++;
					}
					mutant = null; // Destroy mutant immediately
				}
            totalExecTimeNanosL1 += execTimeCurrentProductL1;
            detectorL1 = null; // DESTROY DETECTOR L1
            System.gc();

            // ----------------------------- LEVEL 2 -----------------------------
            FaultDetector detectorL2 = generateFaultDetector(productESGFx, 2);
            long execTimeCurrentProductL2 = 0;
            localMutantID = 0;
            
			for (Vertex eventToOmit : productESGFxVertices) {
				localMutantID++;
				ESG mutant = eventOmitter.createSingleMutant(productESGFx, eventToOmit, localMutantID);

					// Warmup & Measure
					runDetector(detectorL2, mutant);
					execTimeCurrentProductL2 += measureTime(detectorL2, mutant);

					if (detectorL2.isFaultDetected(mutant)) {
						numberOfDetectedMutantsInSPL_L2++;
					}
					mutant = null; // Destroy mutant immediately
				}
			
            totalExecTimeNanosL2 += execTimeCurrentProductL2;
            detectorL2 = null; // DESTROY DETECTOR L2
            System.gc();

            // ----------------------------- LEVEL 3 -----------------------------
            FaultDetector detectorL3 = generateFaultDetector(productESGFx, 3);
            long execTimeCurrentProductL3 = 0;
            localMutantID = 0;

			for (Vertex eventToOmit : productESGFxVertices) {
				localMutantID++;
				ESG mutant = eventOmitter.createSingleMutant(productESGFx, eventToOmit, localMutantID);

					// Warmup & Measure
					runDetector(detectorL3, mutant);
					execTimeCurrentProductL3 += measureTime(detectorL3, mutant);

					if (detectorL3.isFaultDetected(mutant)) {
						numberOfDetectedMutantsInSPL_L3++;
					}
					mutant = null; // Destroy mutant immediately
				}
			
            totalExecTimeNanosL3 += execTimeCurrentProductL3;
            detectorL3 = null; // DESTROY DETECTOR L3
            System.gc();

            // ----------------------------- LEVEL 4 -----------------------------
            FaultDetector detectorL4 = generateFaultDetector(productESGFx, 4);
            long execTimeCurrentProductL4 = 0;
            localMutantID = 0;

			for (Vertex eventToOmit : productESGFxVertices) {
				localMutantID++;
				ESG mutant = eventOmitter.createSingleMutant(productESGFx, eventToOmit, localMutantID);

					// Warmup & Measure
					runDetector(detectorL4, mutant);
					execTimeCurrentProductL4 += measureTime(detectorL4, mutant);

					if (detectorL4.isFaultDetected(mutant)) {
						numberOfDetectedMutantsInSPL_L4++;
					}
					mutant = null; // Destroy mutant immediately
				}
			
            totalExecTimeNanosL4 += execTimeCurrentProductL4;
            detectorL4 = null; // DESTROY DETECTOR L4
            System.gc();

            // -------------------------------------------------------------------
            
            
            
            // Clean up product level objects
            productESGFx = null;
            eventOmitter = null;
            
            // Force GC periodically
            if (handledProducts % 25 == 0) {
                System.gc();
            }

        } // endwhile

        // --- Calculate Stats & Record Results ---

		double percentageInSPLL0 = percentageOfFaultDetection(numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L0);
		double percentageInSPLL1 = percentageOfFaultDetection(numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L1);
		double percentageInSPLL2 = percentageOfFaultDetection(numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L2);
		double percentageInSPLL3 = percentageOfFaultDetection(numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L3);
		double percentageInSPLL4 = percentageOfFaultDetection(numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L4);

		double totalSecondsL0 = totalExecTimeNanosL0 / 1_000_000_000.0;
		double totalSecondsL1 = totalExecTimeNanosL1 / 1_000_000_000.0;
		double totalSecondsL2 = totalExecTimeNanosL2 / 1_000_000_000.0;
		double totalSecondsL3 = totalExecTimeNanosL3 / 1_000_000_000.0;
		double totalSecondsL4 = totalExecTimeNanosL4 / 1_000_000_000.0;

		double killedPerSecondL0 = (totalSecondsL0 > 0) ? numberOfDetectedMutantsInSPL_L0 / totalSecondsL0 : 0;
		double killedPerSecondL1 = (totalSecondsL1 > 0) ? numberOfDetectedMutantsInSPL_L1 / totalSecondsL1 : 0;
		double killedPerSecondL2 = (totalSecondsL2 > 0) ? numberOfDetectedMutantsInSPL_L2 / totalSecondsL2 : 0;
		double killedPerSecondL3 = (totalSecondsL3 > 0) ? numberOfDetectedMutantsInSPL_L3 / totalSecondsL3 : 0;
		double killedPerSecondL4 = (totalSecondsL4 > 0) ? numberOfDetectedMutantsInSPL_L4 / totalSecondsL4 : 0;

		if (N_SHARDS > 1) {

			String shardResultFilePath = shards_mutantgenerator_eventomitter
					+ String.format("faultdetection.shard%02d.csv", CURRENT_SHARD);
			FaultDetectionResultRecorder.writeFaultDetectionResultsForSPL(shardResultFilePath, SPLName, "Event Omitter",
					numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L0, percentageInSPLL0, killedPerSecondL0,
					numberOfDetectedMutantsInSPL_L1, percentageInSPLL1, killedPerSecondL1,
					numberOfDetectedMutantsInSPL_L2, percentageInSPLL2, killedPerSecondL2,
					numberOfDetectedMutantsInSPL_L3, percentageInSPLL3, killedPerSecondL3,
					numberOfDetectedMutantsInSPL_L4, percentageInSPLL4, killedPerSecondL4);
		} else {

			FaultDetectionResultRecorder.writeFaultDetectionResultsForSPL(SPLSummary_FaultDetection, SPLName,
					"Event Omitter", numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L0, percentageInSPLL0,
					killedPerSecondL0, numberOfDetectedMutantsInSPL_L1, percentageInSPLL1, killedPerSecondL1,
					numberOfDetectedMutantsInSPL_L2, percentageInSPLL2, killedPerSecondL2,
					numberOfDetectedMutantsInSPL_L3, percentageInSPLL3, killedPerSecondL3,
					numberOfDetectedMutantsInSPL_L4, percentageInSPLL4, killedPerSecondL4);
		}

	}
}