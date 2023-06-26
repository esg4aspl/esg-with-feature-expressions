package tr.edu.iyte.esgfx.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
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
		Set<Vertex> keySet = ((ESGFx)ESG).getAdjacencyMap().keySet();
		for (Vertex key : keySet) {
			List<Vertex> targetList = new ArrayList<Vertex>();
			for (Vertex value : ((ESGFx)ESG).getAdjacencyMap().get(key))
				targetList.add(value);
			adjacencyMap.put(key, targetList);
		}
	}

	public Map<Vertex, List<Vertex>> getAdjacencyMap() {
		return adjacencyMap;
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

}
