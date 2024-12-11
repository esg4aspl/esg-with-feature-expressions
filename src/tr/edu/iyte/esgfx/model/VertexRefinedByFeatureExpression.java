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
			if (this.toString().equals(toCompare.toString())) {
				return true;
			} else
				return false;
		}

		return false;

	}
	
	public String toString() {
		
		if (this.getEvent().getName().trim().equals("[") || this.getEvent().getName().trim().equals("]")) {
			return super.toString();
		}else {
			String str = this.getEvent().getName().trim() + "/" + this.featureExpression.toString();
			return str;
		}

	}

}
