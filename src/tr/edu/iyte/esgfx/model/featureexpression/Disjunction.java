package tr.edu.iyte.esgfx.model.featureexpression;

import java.util.LinkedHashSet;
import java.util.Set;

public class Disjunction extends FeatureExpression {
    private Set<FeatureExpression> operands;
    private final String operatorName = "OR";
    private final String operatorSign = "||";

    public Disjunction() {
        this.operands = new LinkedHashSet<FeatureExpression>();
    }

    public Set<FeatureExpression> getOperands() {
        return operands;
    }

    public void addOperand(FeatureExpression operand) {
        operands.add(operand);
    }

    @Override
    public boolean evaluate() {
    	boolean value = false;
        for (FeatureExpression operand : operands) {
        	value = value || operand.evaluate();
        }
        return value;
    }

	public String getOperatorName() {
		return operatorName;
	}

	public String getOperatorSign() {
		return operatorSign;
	}
}




