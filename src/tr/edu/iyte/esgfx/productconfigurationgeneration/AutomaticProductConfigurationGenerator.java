package tr.edu.iyte.esgfx.productconfigurationgeneration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.List;
import java.io.File;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;

import tr.edu.iyte.esgfx.cases.CaseStudyUtilities;
import tr.edu.iyte.esgfx.cases.ProductIDUtil;
import tr.edu.iyte.esgfx.cases.resultrecordingutilities.ProductConfigurationFileWriter;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;

public class AutomaticProductConfigurationGenerator extends CaseStudyUtilities {

	public void writeAllProductConfigurationsToFile(FeatureModel featureModel,
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel)
			throws Exception {


		generateFeatureExpressionMapFromFeatureModel(featureModelFilePath, ESGFxFilePath);
		List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);


		SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();
		ISolver solver = SolverFactory.newDefault();
		satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel,
				featureExpressionList);

		int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
		int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));
		
		String shardSpecificFilePath = productConfigurationFilePath;
		if (N_SHARDS > 1) {
			shardSpecificFilePath = shards_productconfigurations + String.format("productconfiguration%02d", CURRENT_SHARD) + ".txt";
		}

		int productID = 0;

		while (solver.isSatisfiable()) {
			productID++;

			// Generate product name
			String productName = ProductIDUtil.format(productID);

			// Process solution 
			String productConfiguration = processSolution(solver.model(), featureExpressionList, productName);

			boolean isProductConfigurationValid = isProductConfigurationValid(featureModel,
					featureExpressionMapFromFeatureModel);

			if (isProductConfigurationValid) {
				if (productConfiguration != null) {
					

					
					if (((productID - 1) % N_SHARDS) == CURRENT_SHARD) {
						ProductConfigurationFileWriter.printProductConfiragutionToFile(shardSpecificFilePath,
								productConfiguration);

					}

					VecInt blockingClause = new VecInt();
					for (int literal : solver.model()) {
						blockingClause.push(-literal);
					}
					solver.addClause(blockingClause); // Explicitly exclude the current model
				}
			} else {
				productID--;
			}
		}
	}

	public List<FeatureExpression> getFeatureExpressionList(
			Map<String, FeatureExpression> featureExpressionMapFromFeatureModel) {

		Set<Entry<String, FeatureExpression>> entrySet = featureExpressionMapFromFeatureModel.entrySet();
		Iterator<Entry<String, FeatureExpression>> entrySetIterator = entrySet.iterator();
		List<FeatureExpression> featureExpressionList = new ArrayList<FeatureExpression>(
				featureExpressionMapFromFeatureModel.size() + 1);

		int index = 0;
		while (entrySetIterator.hasNext()) {
			Entry<String, FeatureExpression> entry = entrySetIterator.next();
			String featureName = entry.getKey();
			FeatureExpression featureExpression = entry.getValue();
			if (!featureName.contains("!") && !(featureExpression == null)
					&& !(featureExpression.getFeature().getName() == null)) {
				featureExpressionList.add(index, featureExpression);
				index++;
			}
		}
		return featureExpressionList;
	}
	
	private String processSolution(int[] model, List<FeatureExpression> featureExpressionList,
			String productName) {
		StringBuilder productConfiguration = new StringBuilder(productName + ": <");
		int numberOfFeatures = 0;

		for (int i = 0; i < model.length; i++) {
			FeatureExpression featureExpression = featureExpressionList.get(i);
			String featureName = featureExpression.getFeature().getName();
			if (model[i] > 0) {
				featureExpression.setTruthValue(true); // Orijinal kodunuzdaki gibi açık bıraktım
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

		return productConfiguration.toString();
	}
}