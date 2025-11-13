package tr.edu.iyte.esgfx.model.sequenceesgfx;

import tr.edu.iyte.esg.model.Event;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esg.model.sequenceesg.Sequence;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.Conjunction;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

public class VertexRefinedBySequence extends VertexRefinedByFeatureExpression {
	private Sequence<Vertex> sequence;

	public VertexRefinedBySequence(int ID, Event event) {
		super(ID, event);
		this.sequence = new Sequence<Vertex>();
	}

	public VertexRefinedBySequence(int ID, Event event, Sequence<Vertex> sequence) {
		super(ID, event);
		this.sequence = sequence;
	}

	public VertexRefinedBySequence(int ID, Event event, FeatureExpression featureExpression) {
		super(ID, event, featureExpression);
		this.sequence = new Sequence<Vertex>();
	}

	public VertexRefinedBySequence(int ID, Event event, FeatureExpression featureExpression,
			Sequence<Vertex> sequence) {
		super(ID, event, featureExpression);
		this.sequence = sequence;
	}

	public Sequence<Vertex> getSequence() {
		return sequence;
	}

	public void setFeatureExpression() {
		FeatureExpression conjunction = new Conjunction();

		for (int i = 0; i < sequence.getSize(); i++) {
			Vertex vertex = sequence.getElement(i);
			((Conjunction) conjunction).addOperand(((VertexRefinedByFeatureExpression) vertex).getFeatureExpression());
		}
		this.featureExpression = conjunction;
	}

	public void setFeatureExpression(FeatureExpression featureExpression) {
		this.featureExpression = featureExpression;
	}

	public boolean isPseudoStartVertex() {
		return (sequence.getSize() == 1) && (sequence.getElement(0).isPseudoStartVertex());
	}

	public boolean isPseudoEndVertex() {
		return (sequence.getSize() == 1) && (sequence.getElement(0).isPseudoEndVertex());
	}

	public String getShape() {
		return "\", shape = ellipse";
	}

	public String getDotLanguageFormat() {
		return super.toString();
	}

	@Override
	public String getColor() {
		return "black";
	}
}
