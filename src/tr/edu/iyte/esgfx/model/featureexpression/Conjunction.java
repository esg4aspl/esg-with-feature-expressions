package tr.edu.iyte.esgfx.model.featureexpression;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class Conjunction extends FeatureExpression {
    private Set<FeatureExpression> operands;
    private final String operatorName = "AND";
    private final String operatorSign = "&&";

    public Conjunction() {
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
    	boolean value = true;
        for (FeatureExpression operand : operands) {
        	value = value && operand.evaluate();
        }
        return value;
    }

	public String getOperatorName() {
		return operatorName;
	}

	public String getOperatorSign() {
		return operatorSign;
	}
	
	public String toString() {
		String s = "";
		
		Iterator<FeatureExpression> operandsIterator = operands.iterator();
		int count = 1;
//		System.out.println("Number of operands " + operands.size());
		
		while(count < operands.size() ) {
			FeatureExpression featureExpression = operandsIterator.next();
//			System.out.println(featureExpression.toString());
			s = s.concat(featureExpression.toString() + " " + this.operatorName +  " ");
			count++;
		}
		
		FeatureExpression featureExpression = operandsIterator.next();
//		System.out.println(featureExpression.toString());
		s = s.concat(featureExpression.toString());
//		System.out.println(s);
		
		return s;
	}
}




