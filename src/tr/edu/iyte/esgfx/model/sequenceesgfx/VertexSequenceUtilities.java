package tr.edu.iyte.esgfx.model.sequenceesgfx;

import java.util.Iterator;

import tr.edu.iyte.esg.model.Event;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esg.model.sequenceesg.Sequence;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;

public class VertexSequenceUtilities {
	public static final String CSTRDELIM = ":";

	/**
	 * - String form of a sequence is constructed by appending contexted string
	 * forms of the events in the vertices in the sequence. - ":" is used as
	 * delimiter.
	 */
	public static String getStringFormWithContextedEvents(Sequence<Vertex> sequence) {
		StringBuilder sb = new StringBuilder();
		if (sequence.getSize() > 0) {
			Iterator<Vertex> iterator = sequence.iterator();
			Vertex vertex = iterator.next();
			sb.append(getVertexStringForm(vertex));
			while (iterator.hasNext()) {
				sb.append(CSTRDELIM);
				vertex = iterator.next();
				sb.append(getVertexStringForm(vertex));
			}
		}
//		System.out.println("VertexSequenceUtilities " + sb.toString());
		return sb.toString();
	}

	private static String getVertexStringForm(Vertex vertex) {
		Event event = vertex.getEvent();
//		System.out.println("Vertex is pseudo " + vertex.isPseudoStartVertex() + " " + vertex.isPseudoEndVertex());
//		System.out.println("Event " + event);
//		System.out.println(EventUtilities.getContextedStringForm(event,
//				((VertexRefinedByFeatureExpression) vertex).getFeatureExpression()));

		return EventUtilities.getContextedStringForm(event,
				((VertexRefinedByFeatureExpression) vertex).getFeatureExpression());

	}
}
