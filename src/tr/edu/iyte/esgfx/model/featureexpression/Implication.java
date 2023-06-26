package tr.edu.iyte.esgfx.model.featureexpression;
public class Implication extends FeatureExpression {
	
    private FeatureExpression leftOperand;
    private FeatureExpression rightOperand;
    
	public Implication(FeatureExpression leftOperand, FeatureExpression rightOperand) {
		this.leftOperand = leftOperand;
		this.rightOperand = rightOperand;
	}

	@Override
	public boolean evaluate() {
		return !getLeftOperand().evaluate() || getRightOperand().evaluate();
	}
	
    public FeatureExpression getLeftOperand() {
        return leftOperand;
    }

    public FeatureExpression getRightOperand() {
        return rightOperand;
    }

}
