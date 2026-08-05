package tr.edu.iyte.esgfx.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generates tests for an explicit, ordered set of product configurations.
 * Enumeration is the caller's business here — unlike
 * {@link AllProductsTestGenerationAPI}, which derives the set from the feature
 * model — and generation is delegated to {@link SingleProductTestGenerationAPI}
 * so all three entry points run the same pipeline.
 */
public final class MultiProductTestGenerationAPI {

	private MultiProductTestGenerationAPI() {
	}

	/**
	 * Results are numbered by position in {@code selections}, so a caller that
	 * let a user order the products can match each result back to the product
	 * it asked for.
	 *
	 * @throws InvalidConfigurationException if any configuration fails to
	 *         satisfy the feature model; the message names the position
	 */
	public static List<SingleProductTestResult> generateForProducts(LoadedSplModel model,
			List<Map<String, Boolean>> selections, int coverageLength) throws Exception {

		synchronized (model) {
			// Validate the whole set before generating any of it, so a mistake in
			// the last configuration does not surface only after the earlier ones
			// have been paid for.
			for (int i = 0; i < selections.size(); i++) {
				ValidationResult validation = SingleProductTestGenerationAPI.validate(model, selections.get(i));
				if (!validation.isValid()) {
					List<String> errors = new ArrayList<String>();
					for (String error : validation.getErrors()) {
						errors.add("Product " + (i + 1) + ": " + error);
					}
					throw new InvalidConfigurationException(errors);
				}
			}

			List<SingleProductTestResult> results = new ArrayList<SingleProductTestResult>(selections.size());
			for (int i = 0; i < selections.size(); i++) {
				results.add(SingleProductTestGenerationAPI.generate(model, i + 1, selections.get(i), coverageLength));
			}
			return results;
		}
	}
}
