package tr.edu.iyte.esgfx.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;
import org.sat4j.tools.ModelIterator;

import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;
import tr.edu.iyte.esgfx.productconfigurationgeneration.ProductConfigurationValidator;
import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;

/**
 * Draws a uniform random sample by picking positions in the valid-configuration
 * enumeration and then walking the SAT space to collect them — the method the
 * research pipelines use.
 *
 * <p>Uniform over valid configurations, and reproducible for a given seed. The
 * cost is that it walks the enumeration, so it suits models that can be
 * enumerated at all; past that point a sampler that does not enumerate is
 * needed.
 */
public final class UniformEnumerationSampler implements ProductConfigurationSampler {

	@Override
	public List<SampledConfiguration> sample(LoadedSplModel model, int sampleSize, long seed) throws Exception {
		if (sampleSize < 1) {
			throw new IllegalArgumentException("sampleSize must be at least 1, got " + sampleSize);
		}

		synchronized (model) {
			// The draw is over positions, so the number of valid configurations
			// has to be known before any position can be chosen.
			long validCount = SingleProductTestGenerationAPI.countValidConfigurations(model);
			if (validCount == 0) {
				return new ArrayList<SampledConfiguration>();
			}

			int drawCount = (int) Math.min(sampleSize, validCount);
			Set<Integer> positions = new LinkedHashSet<Integer>();
			Random random = new Random(seed);
			while (positions.size() < drawCount) {
				positions.add(random.nextInt((int) Math.min(validCount, Integer.MAX_VALUE)) + 1);
			}

			List<Integer> targets = new ArrayList<Integer>(positions);
			Collections.sort(targets);

			return collect(model, targets);
		}
	}

	/** Walks the enumeration once, keeping the configurations at the target positions. */
	private List<SampledConfiguration> collect(LoadedSplModel model, List<Integer> targets) throws Exception {
		Map<String, FeatureExpression> featureExpressionMap = model.getFeatureExpressionMap();
		FeatureModel featureModel = model.getFeatureModel();

		List<FeatureExpression> featureExpressionList =
				SingleProductTestGenerationAPI.buildFeatureExpressionList(featureExpressionMap);

		SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel =
				new SATSolverGenerationFromFeatureModel();
		ISolver solver = new ModelIterator(SolverFactory.newDefault());
		satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel, featureExpressionMap,
				featureExpressionList);

		ProductConfigurationValidator validator = new ProductConfigurationValidator();
		List<SampledConfiguration> sampled = new ArrayList<SampledConfiguration>(targets.size());

		int productId = 0;
		int nextTarget = 0;

		while (nextTarget < targets.size() && solver.isSatisfiable()) {
			int[] modelArray = solver.model();

			for (int i = 0; i < modelArray.length; i++) {
				featureExpressionList.get(i).setTruthValue(modelArray[i] > 0);
			}

			boolean exhausted = SingleProductTestGenerationAPI.blockCurrentModel(solver, modelArray);

			if (validator.validate(featureModel, featureExpressionMap)) {
				productId++;
				if (productId == targets.get(nextTarget)) {
					sampled.add(new SampledConfiguration(productId, currentSelection(featureExpressionMap)));
					nextTarget++;
				}
			}
			if (exhausted) {
				break;
			}
		}

		return sampled;
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
