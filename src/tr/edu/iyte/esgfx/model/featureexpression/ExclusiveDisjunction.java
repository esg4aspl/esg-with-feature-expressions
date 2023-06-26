package tr.edu.iyte.esgfx.model.featureexpression;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class ExclusiveDisjunction extends FeatureExpression {
	private Set<FeatureExpression> operands;
	private final String operatorName = "XOR";
	private final String operatorSign = "^";

	public ExclusiveDisjunction() {
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

		Iterator<FeatureExpression> operandsIterator = (Iterator<FeatureExpression>) operands.iterator();
		if (!operands.isEmpty()) {
			FeatureExpression first = operandsIterator.next();
			boolean value = first.evaluate();
			operands.remove(first);

			if (!operands.isEmpty()) {
				while (operandsIterator.hasNext()) {
					FeatureExpression operand = operandsIterator.next();
					value = value ^ operand.evaluate();
				}
				operands.add(first);
				return value;
			} else
				return false;

		} else
			return false;
	}

	public String getOperatorName() {
		return operatorName;
	}

	public String getOperatorSign() {
		return operatorSign;
	}
}
