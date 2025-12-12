package tr.edu.iyte.esgfx.model;

import java.util.Objects;

import tr.edu.iyte.esg.model.Event;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

public class VertexRefinedByFeatureExpression extends Vertex {

	protected FeatureExpression featureExpression;

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
	public String getShape() {
		return "\", shape = ellipse";
	}

	@Override
	public String getDotLanguageFormat() {
		return super.toString();
	}

	@Override
	public String getColor() {
		return color;
	}

	@Override
	public int hashCode() {

		return Objects.hash(this.getEvent().getName(), this.featureExpression);
	}

	@Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null) return false;
        
        if (getClass() != object.getClass()) return false;
        
        VertexRefinedByFeatureExpression other = (VertexRefinedByFeatureExpression) object;

        
        return getID() == other.getID();
    }

	public String toString() {

		if (this.isPseudoStartVertex() || this.isPseudoEndVertex()) {
			return super.toString();
		} else {
			String str = this.getEvent().getName() + "/" + this.featureExpression.toString();
			return str;
		}

	}

}
