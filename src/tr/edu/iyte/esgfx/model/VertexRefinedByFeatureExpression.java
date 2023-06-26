package tr.edu.iyte.esgfx.model;

import tr.edu.iyte.esg.model.Event;
import tr.edu.iyte.esg.model.VertexSimple;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

public class VertexRefinedByFeatureExpression extends VertexSimple {

	private FeatureExpression featureExpression;

	public VertexRefinedByFeatureExpression(int ID, Event event) {
		super(ID, event);
	}

	public VertexRefinedByFeatureExpression(int ID, Event event, FeatureExpression featureExpression) {
		super(ID, event);
		this.featureExpression = featureExpression;

	}

	public FeatureExpression getFeatureExpression() {
		return featureExpression;
	}

	@Override
	public boolean equals(Object object) {

		if (object instanceof VertexRefinedByFeatureExpression) {
			VertexRefinedByFeatureExpression toCompare = (VertexRefinedByFeatureExpression) object;
			if (this.featureExpression.equals(toCompare.getFeatureExpression())
					&& this.getEvent().getName().equals(toCompare.getEvent().getName())) {
				return true;
			} else
				return false;
		}

		return false;

	}

	@Override
	public String toString() {
		return super.toString();
	}

}
