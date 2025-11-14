package tr.edu.iyte.esgfx.cases;

import java.util.List;

import java.util.Set;
import java.util.LinkedHashSet;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;

import tr.edu.iyte.esg.conversion.dot.ESGToDOTFileConverter;
import tr.edu.iyte.esg.eventsequence.EventSequence;

import tr.edu.iyte.esgfx.model.sequenceesgfx.EventSequenceUtilities;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.conversion.xml.ESGToEFGFileWriter;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;
import tr.edu.iyte.esgfx.testgeneration.TestSuiteFileWriter;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EdgeCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EulerCycleGeneratorForEdgeCoverage;
import tr.edu.iyte.esgfx.testgeneration.eventtriplecoverage.TransformedESGFxGenerator;
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
		
		/* -----------------  SAT TABANLI ŞERİT KISITI (opsiyonel)  -----------------
		 * N_SHARDS_SAT > 0 ise, SHARD değerinin bitlerini ilk k değişkene unit clause
		 * olarak kilitleyerek solver uzayını şeritlere böl.
		 */
		int N_SAT = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS_SAT", "0"));
		if (N_SAT > 0) {
			int S_SAT = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));
			int kbits = (int) Math.ceil(Math.log(N_SAT) / Math.log(2));
			for (int b = 0; b < kbits && b < featureExpressionList.size(); b++) {
				boolean bit = ((S_SAT >> b) & 1) == 1;
				int var = b + 1; // 1-based SAT var index
				solver.addClause(new VecInt(new int[]{ bit ? var : -var }));
			}
		}
		/* ------------------------------------------------------------------------ */

		int ESGFx_numberOfVertices = ESGFx.getRealVertexList().size();
		int ESGFx_numberOfEdges = ESGFx.getRealEdgeList().size();

		int totalNumberOfSequences_L2 = 0;
		int totalNumberOfEvents_L2 = 0;
		int totalNumberOfSequences_L3 = 0;
		int totalNumberOfEvents_L3 = 0;
		int totalNumberOfSequences_L4 = 0;
		int totalNumberOfEvents_L4 = 0;

		int totalNumberOfVertices = 0;
		int totalNumberOfEdges = 0;

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
			
			/* -----------------  ŞERİT (SHARD) KAPISI  -----------------
			 * N_SHARDS : toplam şerit sayısı (örn. 2, 4, 8)
			 * SHARD    : bu sürecin şerit indeksi [0 .. N_SHARDS-1]
			 * productID 1'den başladığı için (productID-1) ile mod alıyoruz.
			 * Şart sağlanmıyorsa bu ürünü bu süreç atlar.
			 */
			int N = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
			int S = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));
			if (((productID - 1) % N) != S) {
			    continue;
			}
			/* --------------------------------------------------------- */

			String ESGFxName = productName + Integer.toString(productID);

			ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();
			ESG productESGFx = productESGFxGenerator.generateProductESGFx(productID, ESGFxName, ESGFx);

			int perProduct_NumberOfVertices = productESGFx.getRealVertexList().size();
			int perProduct_NumberOfEdges = productESGFx.getRealEdgeList().size();

			totalNumberOfVertices += perProduct_NumberOfVertices;
			totalNumberOfEdges += perProduct_NumberOfEdges;

//			System.out.println(productID);


			for (coverageLength = 2; coverageLength <= 4; coverageLength++) {
				
				int perProduct_NumberOfSequences = 0;
				int perProduct_NumberOfEvents = 0;
				
//				System.out.println("COVERAGE" + coverageLength);

				setCoverageType();

				TransformedESGFxGenerator transformedESGFxGenerator = new TransformedESGFxGenerator();
				ESG transformedProductESGFx = transformedESGFxGenerator.generateTransformedESGFx(coverageLength,
						productESGFx);

//				ESGToDOTFileConverter.buildDOTFileFromESG(transformedProductESGFx,
//						DOTFolderPath + productName + "_" + coverageType + ".dot");

				ESG stronglyConnectedBalancedESGFx = StronglyConnectedBalancedESGFxGeneration
						.getStronglyConnectedBalancedESGFxGeneration(transformedProductESGFx);

				EulerCycleGeneratorForEdgeCoverage eulerCycleGeneratorForEdgeCoverage = new EulerCycleGeneratorForEdgeCoverage();
				eulerCycleGeneratorForEdgeCoverage.generateEulerCycle(stronglyConnectedBalancedESGFx);
				List<Vertex> eulerCycle = eulerCycleGeneratorForEdgeCoverage.getEulerCycle();

				EulerCycleToTestSequenceGenerator eulerCycleToTestSequenceGenerator = new EulerCycleToTestSequenceGenerator();
				Set<EventSequence> CESsOfESG = eulerCycleToTestSequenceGenerator.CESgenerator(eulerCycle);

//				tr.edu.iyte.esg.eventsequence.EventSequenceUtilities.esgEventSequenceSetPrinter(CESsOfESG);

				EdgeCoverageAnalyser edgeCoverageAnalyser = new EdgeCoverageAnalyser();
				double coverage = edgeCoverageAnalyser.analyseEdgeCoverage(stronglyConnectedBalancedESGFx, CESsOfESG,
						featureExpressionMapFromFeatureModel);

				Set<EventSequence> newCESsOfESG = new LinkedHashSet<EventSequence>();
				for (EventSequence es : CESsOfESG) {

					EventSequence newES = EventSequenceUtilities.removeRepetitionsFromEventSequence(coverageLength, es);
					newCESsOfESG.add(newES);
					perProduct_NumberOfSequences += 1;
					perProduct_NumberOfEvents += newES.length();

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
				}
				
//				tr.edu.iyte.esg.eventsequence.EventSequenceUtilities.esgEventSequenceSetPrinter(newCESsOfESG);
				
//				TestSuiteFileWriter.writeEventSequenceSetAndCoverageAnalysisToFilePerProduct(testSuiteFilePath,
//						productConfiguration.toString(),perProduct_NumberOfVertices, perProduct_NumberOfEdges,
//						perProduct_NumberOfSequences, perProduct_NumberOfEvents, newCESsOfESG, coverageLength,
//						coverageType, coverage);
			}
		}
		
//		System.out.println("----------------------------------------------");
		
		int S = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));
		if (S == 0) {


		TestSuiteFileWriter.writeSPLModelTestSuiteSummary(SPLSummary_TestSuite, SPLName, productID, ESGFx_numberOfVertices,
				ESGFx_numberOfEdges, totalNumberOfVertices, totalNumberOfEdges, totalNumberOfSequences_L2,
				totalNumberOfEvents_L2, totalNumberOfSequences_L3, totalNumberOfEvents_L3, totalNumberOfSequences_L4,
				totalNumberOfEvents_L4);
		}

//		System.out.println("Number of vertices: " + totalNumberOfVertices);
//		System.out.println("Number of edges: " + totalNumberOfEdges);
//		
//		System.out.println("Number of Sequences L2: "+  + totalNumberOfSequences_L2);
//		System.out.println("Number of Events L2: "+  + totalNumberOfEvents_L2);
//		System.out.println("Number of Sequences L3: "+  + totalNumberOfSequences_L3);
//		System.out.println("Number of Events L3: "+  + totalNumberOfEvents_L3);
//		System.out.println("Number of Sequences L4: "+  + totalNumberOfSequences_L4);
//		System.out.println("Number of Events L4: "+  + totalNumberOfEvents_L4);
//
//		System.out.println("Number of Products: " + productID);

	}

}
