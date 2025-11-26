package tr.edu.iyte.esgfx.cases;

import java.util.List;


import java.util.Set;
import java.util.LinkedHashSet;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esgfx.model.sequenceesgfx.EventSequenceUtilities;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;
import tr.edu.iyte.esgfx.testgeneration.TestSuiteFileWriter;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EdgeCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EulerCycleGeneratorForEdgeCoverage;
import tr.edu.iyte.esgfx.testgeneration.eventcoverage.EulerCycleGeneratorForEventCoverage;
import tr.edu.iyte.esgfx.testgeneration.eventcoverage.EventCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.eventtriplecoverage.TransformedESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.randomwalktesting.RandomWalkTestGenerator;
import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;

public class TestSequenceRecorder extends CaseStudyUtilities {

	public void recordTestSequences() throws Exception {

		featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFilePath,
				ESGFxFilePath);
		List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);

		SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();
		ISolver solver = SolverFactory.newDefault();

		satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel,
				featureExpressionList);

		// --- SHARD CONFIGURATION ---
		int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
		int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));

		int ESGFx_numberOfVertices = ESGFx.getRealVertexList().size();
		int ESGFx_numberOfEdges = ESGFx.getRealEdgeList().size();

		// Sequence and Event Counts
		int totalNumberOfSequences_L0 = 0;
		int totalNumberOfEvents_L0 = 0;
		int totalNumberOfSequences_L1 = 0;
		int totalNumberOfEvents_L1 = 0;
		int totalNumberOfSequences_L2 = 0;
		int totalNumberOfEvents_L2 = 0;
		int totalNumberOfSequences_L3 = 0;
		int totalNumberOfEvents_L3 = 0;
		int totalNumberOfSequences_L4 = 0;
		int totalNumberOfEvents_L4 = 0;

		// Coverage Accumulators (To calculate average later)
		double totalCoverage_L0 = 0.0;
		double totalCoverage_L1 = 0.0;
		double totalCoverage_L2 = 0.0;
		double totalCoverage_L3 = 0.0;
		double totalCoverage_L4 = 0.0;

		int totalNumberOfVertices = 0;
		int totalNumberOfEdges = 0;

		int handledProducts = 0; // Counts the products actually processed by this shard
		int productID = 0;

		while (solver.isSatisfiable()) {
			productID++;

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

			// Blocking Clause (Must be added to find the next solution)
			VecInt blockingClause = new VecInt();
			for (int i = 0; i < model.length; i++)
				blockingClause.push(-model[i]);
			solver.addClause(blockingClause);

			boolean isProductConfigurationValid = isProductConfigurationValid(featureModel,
					featureExpressionMapFromFeatureModel);

			if (!isProductConfigurationValid) {
				productID--;
				continue;
			}

			// --- SHARD GATE ---
			// Skip this product if it does not belong to the current shard
			if (((productID - 1) % N_SHARDS) != CURRENT_SHARD) {
				continue;
			}

			handledProducts++; // Increment only for processed products
			String ESGFxName = productName;

			ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();
			ESG productESGFx = productESGFxGenerator.generateProductESGFx(productID, ESGFxName, ESGFx);

//			System.out.println(productESGFx);
			int perProduct_NumberOfVertices = productESGFx.getRealVertexList().size();
			int perProduct_NumberOfEdges = productESGFx.getRealEdgeList().size();

			totalNumberOfVertices += perProduct_NumberOfVertices;
			totalNumberOfEdges += perProduct_NumberOfEdges;

			for (coverageLength = 0; coverageLength <= 4; coverageLength++) {
				setCoverageType();
//				System.out.println(coverageType);
				
				if (coverageLength == 0) {

					int safetyLimit = (int) (5 * Math.pow((productESGFx.getVertexList().size()), 3));

					RandomWalkTestGenerator rw = new RandomWalkTestGenerator((ESGFx) productESGFx, 0.85);
					Set<EventSequence> CESsOfESG_L0 = rw.generateWalkUntilEdgeCoverage(100, safetyLimit);
					EdgeCoverageAnalyser edgeCoverageAnalyser = new EdgeCoverageAnalyser();
					double coverage_L0 = edgeCoverageAnalyser.analyseEdgeCoverage(productESGFx, CESsOfESG_L0,
							featureExpressionMapFromFeatureModel);
					totalCoverage_L0 += coverage_L0;
					
//					int numberOfEvents_L0 = 0;
					for (EventSequence es : CESsOfESG_L0) {
//						numberOfEvents_L0++;
						totalNumberOfSequences_L0 += 1;
						totalNumberOfEvents_L0 += es.length();
					}
					
//					CESsOfESG_L0.forEach(e->System.out.println(e.toString()));

//					TestSuiteFileWriter.writeEventSequenceSetAndCoverageAnalysisToFilePerProduct(
//							testSuiteFilePath, 
//							productConfiguration.toString(),
//							productESGFx.getRealVertexList().size(), 
//							productESGFx.getRealEdgeList().size(), 
//							CESsOfESG_L0.size(),
//							numberOfEvents_L0, 
//							CESsOfESG_L0,
//							coverageLength, 
//							coverageType, 
//							coverage_L0);

				} else if (coverageLength == 1) {
					
					ESG ESGFx = StronglyConnectedBalancedESGFxGeneration
							.getStronglyConnectedBalancedESGFxGeneration(productESGFx);
					EulerCycleGeneratorForEventCoverage eulerCycleGeneratorForEventCoverage = new EulerCycleGeneratorForEventCoverage(
							featureExpressionMapFromFeatureModel);

					eulerCycleGeneratorForEventCoverage.generateEulerCycle(ESGFx);
					List<Vertex> eulerCycleEventCoverage = eulerCycleGeneratorForEventCoverage.getEulerCycle();
					
					EulerCycleToTestSequenceGenerator eulerCycleToTestSequenceGenerator = new EulerCycleToTestSequenceGenerator();
					
					Set<EventSequence> CESsOfESG_L1 = eulerCycleToTestSequenceGenerator
							.CESgenerator(eulerCycleEventCoverage);
					
					EventCoverageAnalyser eventCoverageAnalyser = new EventCoverageAnalyser();
					double coverage_L1 = eventCoverageAnalyser.analyseEventCoverage(ESGFx, CESsOfESG_L1,
							featureExpressionMapFromFeatureModel);
					totalCoverage_L1 += coverage_L1;
//					CESsOfESG_L1.forEach(e->System.out.println(e.toString()));
//					int numberOfEvents_L1 = 0;
					for (EventSequence es : CESsOfESG_L1) {

//						numberOfEvents_L1++;
						totalNumberOfSequences_L1 += 1;
						totalNumberOfEvents_L1 += es.length();
					}
					
//					TestSuiteFileWriter.writeEventSequenceSetAndCoverageAnalysisToFilePerProduct(
//							testSuiteFilePath, 
//							productConfiguration.toString(),
//							productESGFx.getRealVertexList().size(), 
//							productESGFx.getRealEdgeList().size(), 
//							CESsOfESG_L1.size(),
//							numberOfEvents_L1, 
//							CESsOfESG_L1,
//							coverageLength, 
//							coverageType, 
//							coverage_L1);

				} else {

					TransformedESGFxGenerator transformedESGFxGenerator = new TransformedESGFxGenerator();
					ESG transformedProductESGFx = transformedESGFxGenerator.generateTransformedESGFx(coverageLength,
							productESGFx);

					ESG stronglyConnectedBalancedESGFx = StronglyConnectedBalancedESGFxGeneration
							.getStronglyConnectedBalancedESGFxGeneration(transformedProductESGFx);
					EulerCycleGeneratorForEdgeCoverage eulerCycleGeneratorForEdgeCoverage = new EulerCycleGeneratorForEdgeCoverage();
					eulerCycleGeneratorForEdgeCoverage.generateEulerCycle(stronglyConnectedBalancedESGFx);
					List<Vertex> eulerCycle = eulerCycleGeneratorForEdgeCoverage.getEulerCycle();

					EulerCycleToTestSequenceGenerator eulerCycleToTestSequenceGenerator = new EulerCycleToTestSequenceGenerator();
					
					Set<EventSequence> CESsOfESG = eulerCycleToTestSequenceGenerator.CESgenerator(eulerCycle);

					
					EdgeCoverageAnalyser edgeCoverageAnalyser = new EdgeCoverageAnalyser();
					double coverage = edgeCoverageAnalyser.analyseEdgeCoverage(stronglyConnectedBalancedESGFx,
							CESsOfESG, featureExpressionMapFromFeatureModel);

					if (coverageLength == 2) {
						totalCoverage_L2 += coverage;
					} else if (coverageLength == 3) {
						totalCoverage_L3 += coverage;
					} else if (coverageLength == 4) {
						totalCoverage_L4 += coverage;
					}

//					int numberOfEvents = 0;
					Set<EventSequence> newCESsOfESG = new LinkedHashSet<EventSequence>();
					for (EventSequence es : CESsOfESG) {

						EventSequence newES = EventSequenceUtilities.removeRepetitionsFromEventSequence(coverageLength,
								es);
						newCESsOfESG.add(newES);

//						numberOfEvents += newES.length();
						if (coverageLength == 2) {
							totalNumberOfSequences_L2 += 1;
							totalNumberOfEvents_L2 += newES.length();
						} else if (coverageLength == 3) {
							totalNumberOfSequences_L3 += 1;
							totalNumberOfEvents_L3 += newES.length();
						} else if (coverageLength == 4) {
							totalNumberOfSequences_L4 += 1;
							totalNumberOfEvents_L4 += newES.length();
						}
					} // end for
//					newCESsOfESG.forEach(e->System.out.println(e.toString()));
//					TestSuiteFileWriter.writeEventSequenceSetAndCoverageAnalysisToFilePerProduct(
//							testSuiteFilePath, 
//							productConfiguration.toString(),
//							productESGFx.getRealVertexList().size(), 
//							productESGFx.getRealEdgeList().size(), 
//							CESsOfESG.size(),
//							numberOfEvents, 
//							newCESsOfESG,
//							coverageLength, 
//							coverageType, 
//							coverage);
				} // end else
//System.out.println("-------------------------------------------------");
			} // End Coverage Loop
		} // End While Loop

		// --- CALCULATE AVERAGE COVERAGE ---
		double avgCoverageL0 = (handledProducts > 0) ? (totalCoverage_L0 / handledProducts) : 0.0;
		double avgCoverageL1 = (handledProducts > 0) ? (totalCoverage_L1 / handledProducts) : 0.0;
		double avgCoverageL2 = (handledProducts > 0) ? (totalCoverage_L2 / handledProducts) : 0.0;
		double avgCoverageL3 = (handledProducts > 0) ? (totalCoverage_L3 / handledProducts) : 0.0;
		double avgCoverageL4 = (handledProducts > 0) ? (totalCoverage_L4 / handledProducts) : 0.0;

//      System.out.println("Shard " + CURRENT_SHARD + " Processed Products: " + handledProducts);
//      System.out.println("Shard " + CURRENT_SHARD + " Avg Coverage L2: " + avgCoverageL2);
//      System.out.println("Shard " + CURRENT_SHARD + " Avg Coverage L3: " + avgCoverageL3);
//      System.out.println("Shard " + CURRENT_SHARD + " Avg Coverage L4: " + avgCoverageL4);

		// --- WRITE TO FILE ---
		if (N_SHARDS > 1) {
			// Assuming 'shards_testsequencegeneration' is defined in CaseStudyUtilities
//			System.out.println("Shard " + CURRENT_SHARD + " Completed.");
//			System.out.println("Total Products Processed by this Shard: " + handledProducts); // <--- Add this

			String shardResultFilePath = shards_testsequencegeneration
					+ String.format("testsuite.shard%02d.csv", CURRENT_SHARD);

			TestSuiteFileWriter.writeSPLModelTestSuiteSummary(shardResultFilePath, SPLName, handledProducts,
					ESGFx_numberOfVertices, ESGFx_numberOfEdges, totalNumberOfVertices, totalNumberOfEdges,
					totalNumberOfSequences_L0, totalNumberOfEvents_L0, avgCoverageL0, totalNumberOfSequences_L1,
					totalNumberOfEvents_L1, avgCoverageL1, totalNumberOfSequences_L2, totalNumberOfEvents_L2,
					avgCoverageL2, totalNumberOfSequences_L3, totalNumberOfEvents_L3, avgCoverageL3,
					totalNumberOfSequences_L4, totalNumberOfEvents_L4, avgCoverageL4);

		} else {
			TestSuiteFileWriter.writeSPLModelTestSuiteSummary(SPLSummary_TestSuite, SPLName, productID,
					ESGFx_numberOfVertices, ESGFx_numberOfEdges, totalNumberOfVertices, totalNumberOfEdges,
					totalNumberOfSequences_L0, totalNumberOfEvents_L0, avgCoverageL0, totalNumberOfSequences_L1,
					totalNumberOfEvents_L1, avgCoverageL1, totalNumberOfSequences_L2, totalNumberOfEvents_L2,
					avgCoverageL2, totalNumberOfSequences_L3, totalNumberOfEvents_L3, avgCoverageL3,
					totalNumberOfSequences_L4, totalNumberOfEvents_L4, avgCoverageL4);
		}

	}
}