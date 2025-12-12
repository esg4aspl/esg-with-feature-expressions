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
		System.out.println("EVENT OMITTER " + SPLName + " STARTED");

		try {
			featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFilePath,
					ESGFxFilePath);
			List<FeatureExpression> featureExpressionList = getFeatureExpressionList(
					featureExpressionMapFromFeatureModel);

			SATSolverGenerationFromFeatureModel sat = new SATSolverGenerationFromFeatureModel();
			ISolver solver = new ModelIterator(SolverFactory.newDefault());
			sat.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel, featureExpressionList);

			int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
			int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));

			int handledProducts = 0;
			int productID = 0;
			int numberOfMutantsInSPL = 0;

			int numberOfDetectedMutantsInSPL_L0 = 0;
			int numberOfDetectedMutantsInSPL_L1 = 0;
			int numberOfDetectedMutantsInSPL_L2 = 0;
			int numberOfDetectedMutantsInSPL_L3 = 0;
			int numberOfDetectedMutantsInSPL_L4 = 0;

			ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();

			while (solver.isSatisfiable()) {
				productID++;
				String productName = ProductIDUtil.format(productID);

				int[] model = solver.model();
				VecInt blockingClause = new VecInt();
				for (int i = 0; i < model.length; i++) {
					FeatureExpression fe = featureExpressionList.get(i);
					if (model[i] > 0)
						fe.setTruthValue(true);
					else
						fe.setTruthValue(false);

					blockingClause.push(-model[i]);
				}
				solver.addClause(blockingClause);

				boolean isProductConfigurationValid = isProductConfigurationValid(featureModel,
						featureExpressionMapFromFeatureModel);

				if (!isProductConfigurationValid) {
					productID--;
					continue;
				}

				// --- SHARD GATE ---
				if (((productID - 1) % N_SHARDS) != CURRENT_SHARD) {
					continue;
				}

				handledProducts++;

				EventOmitter eventOmitter = new EventOmitter();
				int localMutantID = 0;

//				System.out.println("----------------------------- LEVEL 0 -----------------------------");
				ESG productESGFx = productESGFxGenerator.generateProductESGFx(productID, productName, ESGFx);
				List<Vertex> productESGFxVertices = productESGFx.getRealVertexList();

				numberOfMutantsInSPL += productESGFxVertices.size();
				
				FaultDetector detectorL0 = generateFaultDetector(productESGFx, 0);
				long execTimeCurrentProductL0 = 0;
				localMutantID = 0; 
//
//				System.out.println("Mutation of Level 0");
				for (Vertex eventToOmit : productESGFxVertices) {
					localMutantID++;
					ESG mutant = eventOmitter.createSingleMutant(productESGFx, eventToOmit, localMutantID);
					// //System.out.println("eventToOmit "+ eventToOmit);

					runDetector(detectorL0, mutant);
					execTimeCurrentProductL0 += measureTime(detectorL0, mutant);

					if (detectorL0.isFaultDetected(mutant)) {
						numberOfDetectedMutantsInSPL_L0++;
//						System.out.println("Mutant " + localMutantID + " DETECTED");
					} else
//						System.out.println("Mutant " + localMutantID + " NOT DETECTED");

						mutant = null; // Destroy mutant immediately
				}

				totalExecTimeNanosL0 += execTimeCurrentProductL0;
				detectorL0 = null; // DESTROY DETECTOR L0 (Free RAM)
				System.gc(); // Suggest GC to clean up L0 mess

//				System.out.println("----------------------------- LEVEL 1-----------------------------");
				productESGFx = null;
				productESGFx = productESGFxGenerator.generateProductESGFx(productID, productName, ESGFx);
				productESGFxVertices = productESGFx.getRealVertexList();

				FaultDetector detectorL1 = generateFaultDetector(productESGFx, 1);
				long execTimeCurrentProductL1 = 0;
				localMutantID = 0;

//				System.out.println("Mutation of Level 1");
				for (Vertex eventToOmit : productESGFxVertices) {
					localMutantID++;
					ESG mutant = eventOmitter.createSingleMutant(productESGFx, eventToOmit, localMutantID);
					// //System.out.println("eventToOmit ".toUpperCase() + eventToOmit);

					runDetector(detectorL1, mutant);
					execTimeCurrentProductL1 += measureTime(detectorL1, mutant);

					if (detectorL1.isFaultDetected(mutant)) {
						numberOfDetectedMutantsInSPL_L1++;
//						System.out.println("Mutant " + localMutantID + " DETECTED");
					} else
//						System.out.println("Mutant " + localMutantID + " NOT DETECTED");
						mutant = null;

				}
				// //System.out.println("S1");
				totalExecTimeNanosL1 += execTimeCurrentProductL1;
				detectorL1 = null;
				System.gc();

//				System.out.println("----------------------------- LEVEL 2 -----------------------------");
				productESGFx = null;
				productESGFx = productESGFxGenerator.generateProductESGFx(productID, productName, ESGFx);
				productESGFxVertices = productESGFx.getRealVertexList();
				FaultDetector detectorL2 = generateFaultDetector(productESGFx, 2);
				long execTimeCurrentProductL2 = 0;
				localMutantID = 0;

//				System.out.println("Mutation of Level 2");
				for (Vertex eventToOmit : productESGFxVertices) {
					localMutantID++;
					ESG mutant = eventOmitter.createSingleMutant(productESGFx, eventToOmit, localMutantID);

					runDetector(detectorL2, mutant);
					execTimeCurrentProductL2 += measureTime(detectorL2, mutant);

					if (detectorL2.isFaultDetected(mutant)) {
						numberOfDetectedMutantsInSPL_L2++;
//						System.out.println("Mutant " + localMutantID + " DETECTED");
					} else
//						System.out.println("Mutant " + localMutantID + " NOT DETECTED");
						mutant = null; // Destroy mutant immediately
				}

				totalExecTimeNanosL2 += execTimeCurrentProductL2;
				detectorL2 = null; // DESTROY DETECTOR L2
				System.gc();

//				System.out.println("----------------------------- LEVEL 3 -----------------------------");
				productESGFx = null;
				productESGFx = productESGFxGenerator.generateProductESGFx(productID, productName, ESGFx);
				productESGFxVertices = productESGFx.getRealVertexList();

				FaultDetector detectorL3 = generateFaultDetector(productESGFx, 3);
				long execTimeCurrentProductL3 = 0;
				localMutantID = 0;

//				System.out.println("Mutation of Level 3");
				for (Vertex eventToOmit : productESGFxVertices) {
					localMutantID++;
					ESG mutant = eventOmitter.createSingleMutant(productESGFx, eventToOmit, localMutantID);

					runDetector(detectorL3, mutant);
					execTimeCurrentProductL3 += measureTime(detectorL3, mutant);

					if (detectorL3.isFaultDetected(mutant)) {
						numberOfDetectedMutantsInSPL_L3++;
//						System.out.println("Mutant " + localMutantID + " DETECTED");
					} else
//						System.out.println("Mutant " + localMutantID + " NOT DETECTED");
						mutant = null; // Destroy mutant immediately
				}
				;
				totalExecTimeNanosL3 += execTimeCurrentProductL3;
				detectorL3 = null; // DESTROY DETECTOR L3
				System.gc();

//				System.out.println("----------------------------- LEVEL 4 -----------------------------");
				productESGFx = null;
				productESGFx = productESGFxGenerator.generateProductESGFx(productID, productName, ESGFx);
				productESGFxVertices = productESGFx.getRealVertexList();

				FaultDetector detectorL4 = generateFaultDetector(productESGFx, 4);
				long execTimeCurrentProductL4 = 0;
				localMutantID = 0;

//				System.out.println("Mutation of Level 4");
				for (Vertex eventToOmit : productESGFxVertices) {
					localMutantID++;
					ESG mutant = eventOmitter.createSingleMutant(productESGFx, eventToOmit, localMutantID);
					// //System.out.println("eventToOmit "+ eventToOmit);
					// Warmup & Measure
					runDetector(detectorL4, mutant);
					execTimeCurrentProductL4 += measureTime(detectorL4, mutant);

					if (detectorL4.isFaultDetected(mutant)) {
						numberOfDetectedMutantsInSPL_L4++;
//						System.out.println("Mutant " + localMutantID + " DETECTED");
					} else
//						System.out.println("Mutant " + localMutantID + " NOT DETECTED");
						mutant = null; // Destroy mutant immediately
				}

				totalExecTimeNanosL4 += execTimeCurrentProductL4;
				detectorL4 = null; // DESTROY DETECTOR L4
				System.gc();
				// //System.out.println("L4 finished");
				// -------------------------------------------------------------------

				// Clean up product level objects
				productESGFx = null;
				eventOmitter = null;

				// Force GC periodically
				if (handledProducts % 50 == 0) {
					System.out.println("Processed " + handledProducts + " products. Current product ID: " + productID);
					System.gc();
				}

			} // endwhile

			// --- Calculate Stats & Record Results ---

			double percentageInSPLL0 = percentageOfFaultDetection(numberOfMutantsInSPL,
					numberOfDetectedMutantsInSPL_L0);
			double percentageInSPLL1 = percentageOfFaultDetection(numberOfMutantsInSPL,
					numberOfDetectedMutantsInSPL_L1);
			double percentageInSPLL2 = percentageOfFaultDetection(numberOfMutantsInSPL,
					numberOfDetectedMutantsInSPL_L2);
			double percentageInSPLL3 = percentageOfFaultDetection(numberOfMutantsInSPL,
					numberOfDetectedMutantsInSPL_L3);
			double percentageInSPLL4 = percentageOfFaultDetection(numberOfMutantsInSPL,
					numberOfDetectedMutantsInSPL_L4);

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
				FaultDetectionResultRecorder.writeFaultDetectionResultsForSPL(shardResultFilePath, SPLName,
						"Event Omitter", numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L0, percentageInSPLL0,
						killedPerSecondL0, numberOfDetectedMutantsInSPL_L1, percentageInSPLL1, killedPerSecondL1,
						numberOfDetectedMutantsInSPL_L2, percentageInSPLL2, killedPerSecondL2,
						numberOfDetectedMutantsInSPL_L3, percentageInSPLL3, killedPerSecondL3,
						numberOfDetectedMutantsInSPL_L4, percentageInSPLL4, killedPerSecondL4);
				System.out.println("EVENT OMITTER " + SPLName + " Shard " + CURRENT_SHARD + " FINISHED");
				System.out.println("Total Products Processed by this Shard: " + handledProducts); // <--- Add this
				System.out.println("Total Mutants Generated: " + numberOfMutantsInSPL);
			} else {

				FaultDetectionResultRecorder.writeFaultDetectionResultsForSPL(SPLSummary_FaultDetection, SPLName,
						"Event Omitter", numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L0, percentageInSPLL0,
						killedPerSecondL0, numberOfDetectedMutantsInSPL_L1, percentageInSPLL1, killedPerSecondL1,
						numberOfDetectedMutantsInSPL_L2, percentageInSPLL2, killedPerSecondL2,
						numberOfDetectedMutantsInSPL_L3, percentageInSPLL3, killedPerSecondL3,
						numberOfDetectedMutantsInSPL_L4, percentageInSPLL4, killedPerSecondL4);
				System.out.println("EVENT OMITTER " + SPLName + " FINISHED " + productID + " products");
			}
		} finally {

			// Kill the executor thread pool so JVM can exit.
			shutdownExecutor();
		}

	}
}