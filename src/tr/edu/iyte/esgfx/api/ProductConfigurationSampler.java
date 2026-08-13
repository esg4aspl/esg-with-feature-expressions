package tr.edu.iyte.esgfx.api;

import java.util.List;

/**
 * Draws a sample of valid product configurations from a feature model.
 *
 * <p>Kept as an interface so that a sampler which scales past enumeration — a
 * SAT-based uniform sampler such as UniGen — can replace
 * {@link UniformEnumerationSampler} without touching the generation path.
 */
public interface ProductConfigurationSampler {

	/**
	 * @param sampleSize how many configurations to draw; a sampler returns
	 *        everything it has if the model holds fewer than this
	 * @param seed fixes the draw, so the same model and seed give the same sample
	 */
	List<SampledConfiguration> sample(LoadedSplModel model, int sampleSize, long seed) throws Exception;
}
