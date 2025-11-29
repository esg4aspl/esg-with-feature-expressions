package tr.edu.iyte.esgfx.cases;

import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;
import org.sat4j.tools.ModelIterator;

import tr.edu.iyte.esg.conversion.dot.ESGToDOTFileConverter;
import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.conversion.xml.ESGToEFGFileWriter;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.mutationtesting.faultdetection.FaultDetector;
import tr.edu.iyte.esgfx.mutationtesting.mutationoperators.MutationOperator;
import tr.edu.iyte.esgfx.mutationtesting.mutationoperators.EdgeInserter;
import tr.edu.iyte.esgfx.mutationtesting.resultutils.FaultDetectionResultRecorder;
import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EulerCycleGeneratorForEdgeCoverage;
import tr.edu.iyte.esgfx.testgeneration.eventtriplecoverage.TransformedESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;

public class ProductESGToEFGFileWriter extends CaseStudyUtilities {

	public void writeToEFGFile() throws Exception {
		featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFilePath,
				ESGFxFilePath);
//		printFeatureExpressionMapFromFeatureModel(featureExpressionMapFromFeatureModel);

		List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);
//		printFeatureExpressionList(featureExpressionList);

		SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();
		ISolver solver = new ModelIterator(SolverFactory.newDefault());
		
		int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
		int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));

		satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel,
				featureExpressionList);
		ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();

		int productID = 0;
		int processedCount = 0;
		while (solver.isSatisfiable()) {

			productID++;
			// Generate product name
			String productName = ProductIDUtil.format(productID);

			
			int numberOfFeatures = 0;

			int[] model = solver.model();
			for (int i = 0; i < model.length; i++) {
				FeatureExpression featureExpression = featureExpressionList.get(i);
				if (model[i] > 0) {
					featureExpression.setTruthValue(true);
					numberOfFeatures++;
				} else {
					featureExpression.setTruthValue(false);
				}
			}

			// Add a clause to block the current model to find the next one
			VecInt blockingClause = new VecInt();
			for (int i = 0; i < model.length; i++) {
				blockingClause.push(-model[i]);
			}
			solver.addClause(blockingClause);

			boolean isProductConfigurationValid = isProductConfigurationValid(featureModel,
					featureExpressionMapFromFeatureModel);

			if (isProductConfigurationValid) {
				
				// ---SHARD GATE ---
				if (((productID - 1) % N_SHARDS) != CURRENT_SHARD) {
					continue;
				}
				processedCount++;
				ESG productESGFx = productESGFxGenerator.generateProductESGFx(productID, productName, ESGFx);
				
				if (N_SHARDS > 1) {
					String shardResultFolderPath = shards_efgfilewriter + String.format("shard%02d/", CURRENT_SHARD) + productName;
					ESGToEFGFileWriter.writeESGToEFGFile(productESGFx, productName , shardResultFolderPath);
					
				}else {
					ESGToEFGFileWriter.writeESGToEFGFile(productESGFx, productName , EFGFolderPath);
					
				}
				
				productESGFx = null;
                if (processedCount % 50 == 0) {
                    System.gc();
                }
				
				
//				ESGToDOTFileConverter.buildDOTFileFromESG(productESGFx,DOTFolderPath + productName + ".dot");
//				ESGToEFGFileWriter.writeESGToEFGFile(productESGFx, productName , EFGFolderPath);
				

			} else {
				productID--;
			}
		}
	}

}
