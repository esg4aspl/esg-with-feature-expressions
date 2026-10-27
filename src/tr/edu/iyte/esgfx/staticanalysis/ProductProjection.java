package tr.edu.iyte.esgfx.staticanalysis;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;

/**
 * Projects the family model onto one configuration with the generation code
 * path ({@link ProductESGFxGenerator}) and maps every vertex of the family
 * model to its copy in the projection.
 */
public final class ProductProjection {

	private final ESG product;
	/** Copy of family vertex i in the projection, or null when the vertex is not in the product. */
	private final Vertex[] copies;

	private ProductProjection(ESG product, Vertex[] copies) {
		this.product = product;
		this.copies = copies;
	}

	/**
	 * @param family        the family model (vertex order as in the encoder)
	 * @param featureMap    the feature expression map the guards of {@code family} read
	 * @param phiD          the feature variables; every feature of the map must be one of them
	 * @param trueFeatures  the features set to true; every other feature of the map is set to false.
	 *                      Abstract features the guards read must be included when they are true.
	 */
	public static ProductProjection of(ESG family, Map<String, FeatureExpression> featureMap, PhiD phiD,
			Collection<String> trueFeatures) {
		ESG product = project(family, featureMap, phiD, trueFeatures);
		return mapCopies(family, product);
	}

	/** Lines 1-2 of the Section 9 baseline: set the truth values and project. */
	public static ESG project(ESG family, Map<String, FeatureExpression> featureMap, PhiD phiD,
			Collection<String> trueFeatures) {
		for (Map.Entry<String, FeatureExpression> entry : featureMap.entrySet()) {
			FeatureExpression expression = entry.getValue();
			if (entry.getKey().contains("!") || expression == null || expression.getFeature() == null
					|| expression.getFeature().getName() == null) {
				continue;
			}
			String name = expression.getFeature().getName();
			if (phiD.variableOf(name) == null) {
				throw new IllegalStateException("Feature " + name + " of the model is not a variable of Phi_d ("
						+ phiD.getSource() + ")");
			}
			expression.setTruthValue(trueFeatures.contains(name));
		}
		return new ProductESGFxGenerator().generateProductESGFx(0, "projection", family);
	}

	private static ProductProjection mapCopies(ESG family, ESG product) {
		// The generator keeps the order of the family vertex list and adds exactly
		// the vertices whose guard holds, so the two lists can be walked together.
		List<Vertex> familyVertices = family.getVertexList();
		List<Vertex> productVertices = product.getVertexList();
		Vertex[] copies = new Vertex[familyVertices.size()];
		int next = 0;
		for (int i = 0; i < familyVertices.size(); i++) {
			Vertex vertex = familyVertices.get(i);
			if (((VertexRefinedByFeatureExpression) vertex).getFeatureExpression().evaluate()) {
				Vertex copy = productVertices.get(next++);
				if (!copy.getEvent().getName().equals(vertex.getEvent().getName())) {
					throw new IllegalStateException("Projection order differs at " + vertex.getEvent().getName());
				}
				copies[i] = copy;
			}
		}
		if (next != productVertices.size()) {
			throw new IllegalStateException("Projection has vertices the family model does not explain");
		}
		return new ProductProjection(product, copies);
	}

	public ESG getProduct() {
		return product;
	}

	/** The copy of family vertex {@code index}, or null when it is not in the product. */
	public Vertex copyOf(int index) {
		return copies[index];
	}
}
