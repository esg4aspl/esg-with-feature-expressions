package tr.edu.iyte.esgfx.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.Vertex;


public class ESGFx extends ESG {

	private Map<Vertex, List<Vertex>> adjacencyMap;

	public ESGFx(int ID, String name) {
		super(ID, name);
		adjacencyMap = new LinkedHashMap<>();

	}

	public ESGFx(ESG ESG) {
		super(ESG);
		adjacencyMap = new LinkedHashMap<>();
		Set<Vertex> keySet = ((ESGFx) ESG).getAdjacencyMap().keySet();
		for (Vertex key : keySet) {
			List<Vertex> targetList = new ArrayList<Vertex>();
			for (Vertex value : ((ESGFx) ESG).getAdjacencyMap().get(key))
				targetList.add(value);
			adjacencyMap.put(key, targetList);
		}
	}

	public Map<Vertex, List<Vertex>> getAdjacencyMap() {
		return adjacencyMap;
	}

	public List<Vertex> getAdjacencyList(Vertex vertex) {

		List<Vertex> adjacencyList = new ArrayList<Vertex>();
		Set<Vertex> keySet = getAdjacencyMap().keySet();
		for (Vertex key : keySet) {
			if (key.toString().equals(vertex.toString())) {
				adjacencyList = getAdjacencyMap().get(key);
			}
		}
		return adjacencyList;
	}

	public void setID(int ID) {
		this.ID = ID;
	}

	@Override
	public void addEdge(Edge edge) {
		super.addEdge(edge);

//		System.out.println("ESGFx addEdge");
		if (adjacencyMap.containsKey(edge.getSource())) {
			List<Vertex> targetList = adjacencyMap.get(edge.getSource());
			targetList.add(edge.getTarget());
		} else {
			List<Vertex> targetList = new ArrayList<Vertex>();
			targetList.add(edge.getTarget());
			adjacencyMap.put(edge.getSource(), targetList);
		}
	}

	@Override
	public void removeEdge(Edge edge) {
		super.removeEdge(edge);

		if (adjacencyMap.containsKey(edge.getSource())) {
			List<Vertex> targetList = adjacencyMap.get(edge.getSource());
			targetList.remove(edge.getTarget());
		}
	}

	@Override
	public Vertex getVertexByEventName(String eventName) {
		for (Vertex vertex : getVertexList()) {
			if (vertex.toString().equals(eventName))
				return vertex;
		}
		return null;
	}

	private String vertexListToString() {
		String vertexListToString = "Vertex List as (ID)Event: \n";
		for (Vertex vertex : getVertexList()) {
			vertexListToString += "(" + vertex.getID() + ")" + vertex.toString() + ", ";
		}
		vertexListToString += "\n";
		return vertexListToString;
	}

	private String edgeListToString() {
		String edgeListToString = "Edge List as (ID): \n";
		for (Edge edge : getEdgeList()) {
			Vertex source = edge.getSource();
			Vertex target = edge.getTarget();

			edgeListToString += "(" + edge.getID() + ")" + "<" + source.toString() + "-" + target.toString() + ">, ";

			edgeListToString += "\n";

		}
		return edgeListToString;
	}

	public String vertexMapToString() {
		Set<Vertex> keySet = getVertexMap().keySet();
		String edgeMapToString = "Vertex Map as <(ID)Event, (ID)Event>:\n";
		for (Vertex key : keySet) {

			edgeMapToString += "<" + "(" + key.getID() + ")" + key.toString() + " -> ";

			List<Vertex> targetList = adjacencyMap.get(key);
			for (Vertex target : targetList) {
				edgeMapToString += "(" + target.getID() + ")" + target.toString() + ", ";
			}
			edgeMapToString = edgeMapToString.substring(0, edgeMapToString.length() - 2);
			edgeMapToString += ">\n";
		}
		edgeMapToString += "\n";
		return edgeMapToString;
	}

	@Override
	public String toString() {
		String toString = "ESGFx " + getID() + ", " + getName() + "\n";
		toString += vertexListToString();
		toString += edgeListToString();
		toString += vertexMapToString();
		return toString;
	}
}
