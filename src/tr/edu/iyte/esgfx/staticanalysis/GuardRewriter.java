package tr.edu.iyte.esgfx.staticanalysis;

import java.util.HashMap;
import java.util.Map;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.EdgeSimple;
import tr.edu.iyte.esg.model.Event;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

/**
 * Copies an ESG-Fx model in memory with some vertex guards replaced. Vertex,
 * edge and event IDs and the order of the vertex and edge lists are kept, and
 * the events are shared with the original. Nothing is written to disk.
 */
public final class GuardRewriter {

	private GuardRewriter() {
	}

	/** @param guards replacement guard per vertex ID; vertices not in the map keep their guard */
	public static ESG withGuards(ESG original, Map<Integer, FeatureExpression> guards) {
		ESGFx copy = new ESGFx(original.getID(), original.getName());
		for (Event event : original.getEventList()) {
			copy.addEvent(event);
		}
		Map<Integer, Vertex> copies = new HashMap<Integer, Vertex>();
		for (Vertex vertex : original.getVertexList()) {
			FeatureExpression guard = guards.containsKey(vertex.getID()) ? guards.get(vertex.getID())
					: ((VertexRefinedByFeatureExpression) vertex).getFeatureExpression();
			Vertex vertexCopy = new VertexRefinedByFeatureExpression(vertex.getID(), vertex.getEvent(), guard);
			copy.addVertex(vertexCopy);
			copies.put(vertex.getID(), vertexCopy);
		}
		for (Edge edge : original.getEdgeList()) {
			copy.addEdge(new EdgeSimple(edge.getID(), copies.get(edge.getSource().getID()),
					copies.get(edge.getTarget().getID())));
		}
		return copy;
	}
}
