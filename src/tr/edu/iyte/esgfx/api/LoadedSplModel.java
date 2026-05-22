package tr.edu.iyte.esgfx.api;

import java.util.Map;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;

public final class LoadedSplModel {

	private final FeatureModel featureModel;
	private final ESG esgFx;
	private final Map<String, FeatureExpression> featureExpressionMap;

	public LoadedSplModel(FeatureModel featureModel, ESG esgFx,
			Map<String, FeatureExpression> featureExpressionMap) {
		this.featureModel = featureModel;
		this.esgFx = esgFx;
		this.featureExpressionMap = featureExpressionMap;
	}

	public FeatureModel getFeatureModel() {
		return featureModel;
	}

	public ESG getEsgFx() {
		return esgFx;
	}

	public Map<String, FeatureExpression> getFeatureExpressionMap() {
		return featureExpressionMap;
	}
}
