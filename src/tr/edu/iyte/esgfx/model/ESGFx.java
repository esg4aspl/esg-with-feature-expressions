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

	private Map<String, Vertex> nameToVertexCache;

	public ESGFx(int ID, String name) {
		super(ID, name);
		adjacencyMap = new LinkedHashMap<>();
		nameToVertexCache = new LinkedHashMap<>();

	}

	public ESGFx(ESG ESG) {
		super(ESG);
		adjacencyMap = new LinkedHashMap<>();
		nameToVertexCache = new LinkedHashMap<>();

		Map<Vertex, List<Vertex>> originalMap = ((ESGFx) ESG).getAdjacencyMap();

		for (Map.Entry<Vertex, List<Vertex>> entry : originalMap.entrySet()) {

			adjacencyMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}

		for (Vertex v : this.getVertexList()) {
			nameToVertexCache.put(v.toString(), v);
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
		adjacencyMap.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge.getTarget());
	}

	@Override
	public void removeEdge(Edge edge) {
		super.removeEdge(edge);

		List<Vertex> targetList = adjacencyMap.get(edge.getSource());
		if (adjacencyMap.containsKey(edge.getSource())) {
			targetList.remove(edge.getTarget());
		}
	}

	@Override
	public Vertex getVertexByEventName(String eventName) {

		Vertex v = nameToVertexCache.get(eventName);
		if (v != null)
			return v;

		for (Vertex vertex : getVertexList()) {
//			System.out.println("vertex.toString()_" + vertex.toString()+ "_" );
//			System.out.println(vertex.toString().contentEquals(eventName));
			if (vertex.toString().contentEquals(eventName)) {
				nameToVertexCache.put(eventName, vertex);
				return vertex;
			}
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
