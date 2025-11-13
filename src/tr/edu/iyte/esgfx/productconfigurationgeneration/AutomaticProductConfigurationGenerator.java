package tr.edu.iyte.esgfx.productconfigurationgeneration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.List;

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

		System.out.println(featureModel);
		System.out.println("-----------------------------");
		generateFeatureExpressionMapFromFeatureModel(featureModelFilePath, ESGFxFilePath);
		printFeatureExpressionMapFromFeatureModel(featureExpressionMapFromFeatureModel);
		List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);
		printFeatureExpressionList(featureExpressionList);
		
		System.out.println("Number of vertices: " + ESGFx.getVertexList().size());
		System.out.println("Number of real vertices: " + ESGFx.getRealVertexList().size());
		
		System.out.println("Number of edges: " + ESGFx.getEdgeList().size());
		System.out.println("Number of real edges: " + ESGFx.getRealEdgeList().size());
		System.out.println("-----------------------------");
		
		

		SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();
		ISolver solver = SolverFactory.newDefault();
		satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel,
				featureExpressionList);

		int productID = 0;
		while (solver.isSatisfiable()) {
			productID++;

			// Generate product name
			String productName = ProductIDUtil.format(productID);

			// Process solution and write directly to the output file
			String productConfiguration = processSolution(solver.model(), featureExpressionList, productName);

			boolean isProductConfigurationValid = isProductConfigurationValid(featureModel,
					featureExpressionMapFromFeatureModel);

			if (isProductConfigurationValid) {
				if (productConfiguration != null) {
					ProductConfigurationFileWriter.printProductConfiragutionToFile(productConfigurationFilePath,
							productConfiguration);
					// Add a blocking clause to exclude the current model
					VecInt blockingClause = new VecInt();
					for (int literal : solver.model()) {
						blockingClause.push(-literal);
					}
					solver.addClause(blockingClause); // Explicitly exclude the current model
				}
			}else {
				productID--;
			}
		}
		System.out.println("Number of Products: " + productID);
	}

	/*
	 * This method puts feature expression objects into an array list starting from
	 * index 0 to use the indices as the variable in SAT problem
	 */
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
//				System.out.println(featureName + " - " + (index));
				index++;
			}

		}
//		System.out.println("------------------------------");

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
				featureExpression.setTruthValue(true);
				productConfiguration.append(featureName).append(", ");
				numberOfFeatures++;
			} else {
				featureExpression.setTruthValue(false);
			}
		}

		// Finalize product configuration string
		if (numberOfFeatures > 0) {
			productConfiguration.setLength(productConfiguration.length() - 2); // Remove trailing ", "
		}
		productConfiguration.append(">:").append(numberOfFeatures).append(" features");

		// Return the product configuration string
		return productConfiguration.toString();
	}
	
}
