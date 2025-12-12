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
        this.sequence = (sequence != null) ? sequence : new Sequence<Vertex>();
    }

    public VertexRefinedBySequence(int ID, Event event, FeatureExpression featureExpression) {
        super(ID, event, featureExpression);
        this.sequence = new Sequence<Vertex>();
    }

    public VertexRefinedBySequence(int ID, Event event, FeatureExpression featureExpression,
            Sequence<Vertex> sequence) {
        super(ID, event, featureExpression);
        this.sequence = (sequence != null) ? sequence : new Sequence<Vertex>();
    }

    public Sequence<Vertex> getSequence() {
        return sequence;
    }

    public void setFeatureExpression() {
        // Optimization: Direct Conjunction creation
        Conjunction conjunction = new Conjunction();

        if (sequence != null) {
            for (int i = 0; i < sequence.getSize(); i++) {
                Vertex vertex = sequence.getElement(i);
                if (vertex instanceof VertexRefinedByFeatureExpression) {
                    conjunction.addOperand(((VertexRefinedByFeatureExpression) vertex).getFeatureExpression());
                }
            }
        }
        this.featureExpression = conjunction;
    }

    public void setFeatureExpression(FeatureExpression featureExpression) {
        this.featureExpression = featureExpression;
    }

    public boolean isPseudoStartVertex() {
        return (sequence != null) && (sequence.getSize() == 1) && (sequence.getElement(0).isPseudoStartVertex());
    }

    public boolean isPseudoEndVertex() {
        return (sequence != null) && (sequence.getSize() == 1) && (sequence.getElement(0).isPseudoEndVertex());
    }

    @Override
    public String getShape() {
        return "\", shape = ellipse";
    }

    @Override
    public String getDotLanguageFormat() {
        return this.toString();
    }

    @Override
    public String getColor() {
        return "black";
    }

    // --- OPTIMIZED STRING REPRESENTATION ---
    // Uses StringBuilder for efficient memory usage during logging/debugging
    @Override
    public String toString() {
        if (sequence == null || sequence.getSize() == 0) {
            return super.toString();
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < sequence.getSize(); i++) {
             Vertex v = sequence.getElement(i);
             sb.append(v.getEvent().getName());
             if (i < sequence.getSize() - 1) {
                 sb.append(" -> ");
             }
        }
        sb.append("]");
        
        // Append feature expression if exists
        if (this.featureExpression != null) {
             sb.append("/").append(this.featureExpression.toString());
        }
        
        return sb.toString();
    }

    // NOTE: We do NOT override hashCode() or equals() here.
    // We rely on the superclass (Vertex) implementation which uses the unique ID (O(1)).
}