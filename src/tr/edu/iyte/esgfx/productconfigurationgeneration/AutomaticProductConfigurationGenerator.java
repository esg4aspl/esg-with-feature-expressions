package tr.edu.iyte.esgfx.productconfigurationgeneration;

import java.util.Map;

import java.util.List;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;
import org.sat4j.tools.ModelIterator;

import tr.edu.iyte.esgfx.cases.CaseStudyUtilities;
import tr.edu.iyte.esgfx.cases.ProductIDUtil;
import tr.edu.iyte.esgfx.cases.resultrecordingutilities.ProductConfigurationFileWriter;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;

public class AutomaticProductConfigurationGenerator extends CaseStudyUtilities {

	public void writeAllProductConfigurationsToFile(FeatureModel featureModel,
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel)
			throws Exception {

		featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFilePath,
				ESGFxFilePath);

		List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);
//		printFeatureExpressionList(featureExpressionList);

		SATSolverGenerationFromFeatureModel sat = new SATSolverGenerationFromFeatureModel();
		ISolver solver = new ModelIterator(SolverFactory.newDefault());
		sat.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel, featureExpressionList);

		int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
		int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));

		
		
		String shardSpecificFilePath = shards_productconfigurations;
		if (N_SHARDS > 1) {
			shardSpecificFilePath +=  String.format("productconfiguration%02d", CURRENT_SHARD) + ".txt";
		}

		int productID = 0;

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

			VecInt blocking = new VecInt();
			for (int i = 0; i < model.length; i++)
				blocking.push(-model[i]);
			solver.addClause(blocking);

			if (!isProductConfigurationValid(featureModel, featureExpressionMapFromFeatureModel)) {
				productID--;
				continue;
			}

			// ---SHARD GATE ---
			if (((productID - 1) % N_SHARDS) != CURRENT_SHARD) {
				continue;
			}
			


			if(N_SHARDS > 1) {
				ProductConfigurationFileWriter.printProductConfiragutionToFile(shardSpecificFilePath, productConfiguration.toString());
			}else {
				ProductConfigurationFileWriter.printProductConfiragutionToFile(productConfigurationFilePath, productConfiguration.toString());
			}
		}
	}


}