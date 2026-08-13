package tr.edu.iyte.esgfx.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates tests for a sample of a feature model's valid product
 * configurations. Drawing the sample is the sampler's business and generating
 * the tests is {@link SingleProductTestGenerationAPI}'s, so this only joins the
 * two.
 */
public final class SampledProductsTestGenerationAPI {

	/** Fixed by default so that a run can be repeated exactly. */
	public static final long DEFAULT_SEED = 42L;

	private SampledProductsTestGenerationAPI() {
	}

	public static List<SingleProductTestResult> generateForSample(LoadedSplModel model, int sampleSize,
			int coverageLength) throws Exception {
		return generateForSample(model, new UniformEnumerationSampler(), sampleSize, DEFAULT_SEED, coverageLength);
	}

	public static List<SingleProductTestResult> generateForSample(LoadedSplModel model,
			ProductConfigurationSampler sampler, int sampleSize, long seed, int coverageLength) throws Exception {

		synchronized (model) {
			List<SampledConfiguration> sampled = sampler.sample(model, sampleSize, seed);

			List<SingleProductTestResult> results = new ArrayList<SingleProductTestResult>(sampled.size());
			for (SampledConfiguration configuration : sampled) {
				// The sampled configuration keeps its position in the full
				// enumeration, so a sampled result can be compared against the
				// same product from an all-products run.
				results.add(SingleProductTestGenerationAPI.generate(model, configuration.getProductId(),
						configuration.getSelection(), coverageLength));
			}
			return results;
		}
	}
}
