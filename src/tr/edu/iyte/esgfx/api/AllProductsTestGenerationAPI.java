package tr.edu.iyte.esgfx.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;
import org.sat4j.tools.ModelIterator;

import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;
import tr.edu.iyte.esgfx.productconfigurationgeneration.ProductConfigurationValidator;
import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;

/**
 * Generates tests for every valid product configuration of a feature model.
 * Enumeration and validation live here; the generation itself is delegated to
 * {@link SingleProductTestGenerationAPI} so both entry points run the same
 * pipeline.
 */
public final class AllProductsTestGenerationAPI {

	private AllProductsTestGenerationAPI() {
	}

	public static List<SingleProductTestResult> generateForAllProducts(LoadedSplModel model, int coverageLength)
			throws Exception {

		synchronized (model) {
			Map<String, FeatureExpression> featureExpressionMap = model.getFeatureExpressionMap();
			FeatureModel featureModel = model.getFeatureModel();

			List<FeatureExpression> featureExpressionList = SingleProductTestGenerationAPI
					.buildFeatureExpressionList(featureExpressionMap);

			SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();
			ISolver solver = new ModelIterator(SolverFactory.newDefault());
			satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel, featureExpressionMap,
					featureExpressionList);

			ProductConfigurationValidator validator = new ProductConfigurationValidator();
			List<SingleProductTestResult> results = new ArrayList<SingleProductTestResult>();

			// Product IDs count valid configurations only, matching the numbering the
			// research pipelines write their per-product files under.
			int productId = 0;

			while (solver.isSatisfiable()) {
				int[] modelArray = solver.model();

				for (int i = 0; i < modelArray.length; i++) {
					featureExpressionList.get(i).setTruthValue(modelArray[i] > 0);
				}

				boolean exhausted = SingleProductTestGenerationAPI.blockCurrentModel(solver, modelArray);

				if (validator.validate(featureModel, featureExpressionMap)) {
					productId++;
					results.add(SingleProductTestGenerationAPI.generate(model, productId,
							currentSelection(featureExpressionMap), coverageLength));
				}
				if (exhausted) {
					break;
				}
			}

			return results;
		}
	}

	private static Map<String, Boolean> currentSelection(Map<String, FeatureExpression> featureExpressionMap) {
		Map<String, Boolean> selection = new LinkedHashMap<String, Boolean>();
		for (Map.Entry<String, FeatureExpression> entry : featureExpressionMap.entrySet()) {
			if (!entry.getKey().contains("!")) {
				selection.put(entry.getKey(), entry.getValue().evaluate());
			}
		}
		return selection;
	}
}
