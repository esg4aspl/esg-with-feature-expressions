package tr.edu.iyte.esgfx.model.featureexpression;

import tr.edu.iyte.esgfx.model.featuremodel.Feature;

public class FeatureExpression {

	private Feature feature;
	protected boolean truthValue;

	public FeatureExpression() {
		setFeature(null);
		setTruthValue(false);
	}

	public FeatureExpression(Feature feature) {
		setFeature(feature);
		setTruthValue(false);
	}

	public FeatureExpression(Feature feature, boolean truthValue) {
		setFeature(feature);
		setTruthValue(truthValue);
	}
	
	public FeatureExpression(FeatureExpression featureExpression) {
		setFeature(featureExpression.getFeature());
		setTruthValue(featureExpression.evaluate());
	}
	
	public Feature getFeature() {
		return feature;
	}

	public void setFeature(Feature feature) {
		this.feature = feature;
	}

	public boolean evaluate() {
		return truthValue;
	}
	
	public void setTruthValue(boolean truthValue) {
		this.truthValue = truthValue;
	}
	
	@Override
	public String toString() {
		return feature.getName(); //+ " " + truthValue;
	}
	
	@Override
	public boolean equals(Object object) {
        
		if (this == object) return true;
		if (object == null || getClass() != object.getClass()) return false;
		
        
		FeatureExpression other = (FeatureExpression) object;
		
       
		if (feature == null) return other.feature == null;
        
		return feature.getName().equals(other.feature.getName());
	}
	
	@Override
	public int hashCode() {
		return (feature != null) ? feature.getName().hashCode() : 0;
	}

}
