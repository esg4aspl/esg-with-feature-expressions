package tr.edu.iyte.esgfx.model.sequenceesgfx;

import tr.edu.iyte.esg.model.ESG;

import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.EdgeSimple;
import tr.edu.iyte.esg.model.Event;
import tr.edu.iyte.esg.model.EventSimple;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esg.model.comparators.SequenceComparator;
import tr.edu.iyte.esg.model.comparators.VertexComparator;
import tr.edu.iyte.esg.model.sequenceesg.Sequence;

import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

public class ESGFxtoSequenceESGFxConverter {
	
	public ESGFxtoSequenceESGFxConverter() {
		
	}
	
	/**
	 - ID and name for ESG
	   -- ID of the sequence ESG is obtained by incrementing the ID of the input ESG.
	   -- Name of the (sequence ESG is obtained by appending "s" to the name of the input ESG.
	 - Events in the event list
	   -- Event list of the sequence ESG contains the event instances in the event list of the input ESG.
	 - Vertices in sequences in sequence vertices
	   -- Sequence ESG uses existing vertex instances from the input ESG to construct sequences and sequence vertices.
	 - Avoiding the creation of redundant sequence vertices
	   -- A new sequence vertex instance is not created if there is a sequence vertex instance containing an equivalent sequence.
	   -- Instances are looked up from the sequence vertex list with respect to the sequences.
	 - A sequence vertex, its event and its vertex sequence
	   -- An event of a sequence vertex is not an actual event; therefore, it is not included in the event list.
	   -- Name of a sequence vertex event is the string form of the sequence constructed using contexted string forms of the events in the vertices of the sequence.
	 */
	public ESG convert(ESG ESGFx) {
		ESG sESGFx = new ESGFx(ESGFx.getID()+1, ESGFx.getName()+"s"); //!!! esg id and name
//		System.out.println("Converting ESGFx to SequenceESGFx");
		for(Event event : ESGFx.getEventList()) { //!!! actual events
			sESGFx.addEvent(event);
		}
//		System.out.println("DONE");
		for(Vertex vertex : ESGFx.getVertexList()) {
			Sequence<Vertex> sequence = new Sequence<Vertex>();
			sequence.addElement(vertex); //!!! existing vertex instances
	
			Event event = new EventSimple(sESGFx.getNextEventID(), VertexSequenceUtilities.getStringFormWithContextedEvents(sequence)); //!!!name for the event of the sequence vertex
			FeatureExpression featureExpression = ((VertexRefinedByFeatureExpression)vertex).getFeatureExpression();
			
//			System.out.println("Reached HERE 1");
			VertexRefinedBySequence vertexRefinedBySequence = new VertexRefinedBySequence(sESGFx.getNextVertexID(), event, featureExpression, sequence);
//			System.out.println("Reached HERE 2");
			sESGFx.addVertex(vertexRefinedBySequence);
		}
		VertexComparator vc = new VertexComparator();
		SequenceComparator<Vertex> sc = new SequenceComparator<Vertex>(vc);
		for(Edge e : ESGFx.getEdgeList()) {
			Sequence<Vertex> s = new Sequence<Vertex>();
			s.addElement(e.getSource());
			VertexRefinedBySequence v = SequenceESGUtilities.getVertexByVertexSequence(sESGFx, s, sc); //!!! look up to avoid using redundant instances (performance decrease)
			
			Sequence<Vertex> t = new Sequence<Vertex>();
			t.addElement(e.getTarget());
			VertexRefinedBySequence w = SequenceESGUtilities.getVertexByVertexSequence(sESGFx, t, sc); //!!! look up to avoid using redundant instances (performance decrease)
			
			sESGFx.addEdge(new EdgeSimple(sESGFx.getNextEdgeID(), v, w));
		}
		
//		System.out.println("Converted ESGFx to SequenceESGFx");
//		System.out.println(sESGFx.toString());
		return sESGFx;
	}
}
