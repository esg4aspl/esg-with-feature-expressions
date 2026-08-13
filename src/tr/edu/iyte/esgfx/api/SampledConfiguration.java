package tr.edu.iyte.esgfx.api;

import java.util.Map;

/**
 * One configuration drawn by a {@link ProductConfigurationSampler}.
 *
 * <p>{@code productId} is the configuration's 1-based position in the feature
 * model's valid-configuration enumeration, which is the same numbering the
 * research pipelines write their per-product files under. A sampler that does
 * not enumerate — a SAT-based uniform sampler, say — cannot know that position
 * and reports the sample's own ordinal instead.
 */
public final class SampledConfiguration {

	private final int productId;
	private final Map<String, Boolean> selection;

	public SampledConfiguration(int productId, Map<String, Boolean> selection) {
		this.productId = productId;
		this.selection = selection;
	}

	public int getProductId() {
		return productId;
	}

	public Map<String, Boolean> getSelection() {
		return selection;
	}
}
