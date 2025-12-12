package tr.edu.iyte.esgfx.model.sequenceesgfx;

import java.util.HashMap;
import java.util.Map;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.EdgeSimple;
import tr.edu.iyte.esg.model.Event;
import tr.edu.iyte.esg.model.EventSimple;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esg.model.sequenceesg.Sequence;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

public class ESGFxtoSequenceESGFxConverter {
    
    public ESGFxtoSequenceESGFxConverter() {
    }
    
    public ESG convert(ESG ESGFx) {
        ESG sESGFx = new ESGFx(ESGFx.getID() + 1, ESGFx.getName() + "s");

        // 1. Copy Events
        for(Event event : ESGFx.getEventList()) {
            sESGFx.addEvent(event);
        }

        // OPTIMIZATION: Map to link Old Vertex -> New Sequence Vertex
        // This replaces the slow SequenceESGUtilities loop.
        // Since we fixed Vertex.hashCode() to use ID, this is blazing fast.
        Map<Vertex, VertexRefinedBySequence> vertexCache = new HashMap<>();

        // 2. Create Vertices and Cache them
        for(Vertex vertex : ESGFx.getVertexList()) {
            Sequence<Vertex> sequence = new Sequence<Vertex>();
            sequence.addElement(vertex);
    
            // We still use your UTILITY to generate the correct name string
            String eventName = VertexSequenceUtilities.getStringFormWithContextedEvents(sequence);
            
            Event event = new EventSimple(sESGFx.getNextEventID(), eventName);
            FeatureExpression featureExpression = ((VertexRefinedByFeatureExpression)vertex).getFeatureExpression();
            
            VertexRefinedBySequence vertexRefinedBySequence = new VertexRefinedBySequence(sESGFx.getNextVertexID(), event, featureExpression, sequence);
            
            sESGFx.addVertex(vertexRefinedBySequence);
            
            // CACHE IT! 
            // "When you see 'vertex', use 'vertexRefinedBySequence'"
            vertexCache.put(vertex, vertexRefinedBySequence);
        }

        // 3. Create Edges using Cache (No more loops inside loops!)
        for(Edge e : ESGFx.getEdgeList()) {
            // DIRECT LOOKUP (O(1)) instead of SequenceESGUtilities.getVertexByVertexSequence (O(N))
            VertexRefinedBySequence v = vertexCache.get(e.getSource());
            VertexRefinedBySequence w = vertexCache.get(e.getTarget());
            
            // If map returns valid vertices, add the edge
            if (v != null && w != null) {
                sESGFx.addEdge(new EdgeSimple(sESGFx.getNextEdgeID(), v, w));
            }
        }
        
        return sESGFx;
    }
}