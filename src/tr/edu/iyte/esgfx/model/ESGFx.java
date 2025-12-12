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

    // Optimization: Lookup vertex by name in O(1)
    private Map<String, Vertex> vertexNameMap;

    public ESGFx(int ID, String name) {
        super(ID, name);
        adjacencyMap = new LinkedHashMap<>();
        vertexNameMap = new LinkedHashMap<>();
    }

    public ESGFx(ESG esg) {
        super(esg);
        adjacencyMap = new LinkedHashMap<>();
        vertexNameMap = new LinkedHashMap<>();

        // Populate adjacencyMap based on the input type
        if (esg instanceof ESGFx) {
            // If it is already an ESGFx, copy the List-based map directly (Deep Copy)
            Map<Vertex, List<Vertex>> originalMap = ((ESGFx) esg).getAdjacencyMap();
            for (Map.Entry<Vertex, List<Vertex>> entry : originalMap.entrySet()) {
                adjacencyMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        } else {
            // If it is a standard ESG, convert Set-based map to List-based map
            for (Map.Entry<Vertex, Set<Vertex>> entry : esg.getVertexMap().entrySet()) {
                adjacencyMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }

        // Initialize Cache
        for (Vertex v : this.getVertexList()) {
            vertexNameMap.put(v.toString(), v);
        }
    }

    public Map<Vertex, List<Vertex>> getAdjacencyMap() {
        return adjacencyMap;
    }

    public List<Vertex> getAdjacencyList(Vertex vertex) {
        // Directly fetch from map. 
        // Requires Vertex.hashCode() and equals() to be ID-based or correctly implemented.
        return adjacencyMap.getOrDefault(vertex, new ArrayList<>());
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    @Override
    public void addEdge(Edge edge) {
        super.addEdge(edge);
        // computeIfAbsent: If key exists, get list; if not, create new list, then add target
        adjacencyMap.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge.getTarget());
    }

    @Override
    public void removeEdge(Edge edge) {
        super.removeEdge(edge);

        List<Vertex> targetList = adjacencyMap.get(edge.getSource());
        if (targetList != null) {
            targetList.remove(edge.getTarget());
        }
    }

    @Override
    public Vertex getVertexByEventName(String eventName) {
        // 1. Check Cache first (Fast)
        Vertex v = vertexNameMap.get(eventName);
        if (v != null) return v;

        // 2. Fallback: Iterate list (Slower, but necessary if cache missed)
        for (Vertex vertex : getVertexList()) {
            if (vertex.toString().contentEquals(eventName)) {
                vertexNameMap.put(eventName, vertex); // Update cache
                return vertex;
            }
        }
        return null;
    }

    // --- StringBuilder Optimizations (Memory Efficient) ---

    private String vertexListToString() {
        StringBuilder sb = new StringBuilder("Vertex List as (ID)Event: \n");
        for (Vertex vertex : getVertexList()) {
            sb.append("(").append(vertex.getID()).append(")").append(vertex.toString()).append(", ");
        }
        sb.append("\n");
        return sb.toString();
    }

    private String edgeListToString() {
        StringBuilder sb = new StringBuilder("Edge List as (ID): \n");
        for (Edge edge : getEdgeList()) {
            sb.append("(").append(edge.getID()).append(")<")
              .append(edge.getSource().toString()).append("-")
              .append(edge.getTarget().toString()).append(">, ");
        }
        sb.append("\n");
        return sb.toString();
    }

    public String vertexMapToString() {
        StringBuilder sb = new StringBuilder("Vertex Map as <(ID)Event, (ID)Event>:\n");
        
        // Using super.getVertexMap().keySet() as requested
        Set<Vertex> keySet = getVertexMap().keySet();

        for (Vertex key : keySet) {
            sb.append("<(").append(key.getID()).append(")").append(key.toString()).append(" -> ");

            // Fetch values from local adjacencyMap
            List<Vertex> targetList = adjacencyMap.get(key);
            
            if (targetList != null) {
                for (Vertex target : targetList) {
                    sb.append("(").append(target.getID()).append(")").append(target.toString()).append(", ");
                }
                // Remove trailing comma if list is not empty
                if (!targetList.isEmpty()) {
                    sb.setLength(sb.length() - 2);
                }
            }
            sb.append(">\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "ESGFx " + getID() + ", " + getName() + "\n" +
               vertexListToString() +
               edgeListToString() +
               vertexMapToString();
    }
}