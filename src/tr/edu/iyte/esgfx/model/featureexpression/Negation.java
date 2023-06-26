package tr.edu.iyte.esgfx.model.featureexpression;

public class Negation extends FeatureExpression {

	private FeatureExpression featureExpression;
	private boolean isSetExplicitly = false;


	public Negation(FeatureExpression featureExpression) {
		this.featureExpression = featureExpression;
		this.setFeature(featureExpression.getFeature());
		this.featureExpression.getFeature().setName(featureExpression.getFeature().getName());
	}

	@Override
	public boolean evaluate() {
		if(isSetExplicitly) {
//			isSetExplicitly = false;
			return this.truthValue;
		}else {
			return !this.featureExpression.evaluate();
		}
	}

	public FeatureExpression getFeatureExpression() {
		return featureExpression;
	}

	public void setTruthValueExplicitly(boolean truthValue) {
//		System.out.println("setTruthValueExplicitly");
		isSetExplicitly = true;
		this.truthValue = truthValue;
	}
	
	
	@Override
	public String toString() {
		return "!" + featureExpression.getFeature().getName(); // + " " + truthValue;
	}

}