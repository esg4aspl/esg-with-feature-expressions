package tr.edu.iyte.esgfx.testgeneration.randomwalktesting;

import java.util.*;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.Vertex;

/**
 * RandomWalkTestGenerator
 *
 * Generates test sequences on an ESG-Fx model using a uniform random-walk.
 * Pseudo-finish vertex "]" is never included in the produced sequences.
 */
public  class RandomWalkTestGenerator {
	    
	    private ESG esg;
	    private Set<Edge> uncoveredEdges;
	    private Set<EventSequence> testSequences;
	    private Map<Vertex, List<Edge>> adjacencyMap;
	    
	    public RandomWalkTestGenerator(ESG esg) {
	        this.esg = esg;
	        this.uncoveredEdges = new LinkedHashSet<>(esg.getRealEdgeList());
	        this.testSequences = new LinkedHashSet<>();
	        this.adjacencyMap = new HashMap<>();
	        buildAdjacencyMap();
	    }
	    
	    /**
	     * Adjacency map oluştur - her vertex'ten çıkan edge'leri tut
	     */
	    private void buildAdjacencyMap() {
	        for (Edge edge : esg.getRealEdgeList()) {
	            Vertex source = edge.getSource();
	            if (!adjacencyMap.containsKey(source)) {
	                adjacencyMap.put(source, new ArrayList<>());
	            }
	            adjacencyMap.get(source).add(edge);
	        }
	    }
	    
	    /**
	     * DFS ile tüm edge'leri cover eden test sequence'leri üret
	     */
	    public void generateTestSequences() {
	        while (!uncoveredEdges.isEmpty()) {
	            // Yeni bir test sequence başlat
	            EventSequence eventSequence = new EventSequence();
	            List<Vertex> currentPath = new ArrayList<>();
	            Set<Edge> pathEdges = new LinkedHashSet<>();
	            
	            // Entry vertex'lerden birinden başla
	            Vertex start = selectStartVertex();
	            currentPath.add(start);
	            
	            // DFS ile path oluştur
	            dfsTraversal(start, currentPath, pathEdges);
	            
	            // EventSequence nesnesine dönüştür
	            if (currentPath.size() > 1) {
	                for (Vertex vertex : currentPath) {
	                    eventSequence.getEventSequence().add(vertex);
	                }
	                testSequences.add(eventSequence);
	                
	                // Bu sequence'te cover edilen edge'leri işaretle
	                uncoveredEdges.removeAll(pathEdges);
	            }
	        }
	    }
	    
	    /**
	     * DFS traversal - uncovered edge'leri önceliklendir
	     */
	    private void dfsTraversal(Vertex current, List<Vertex> path, Set<Edge> pathEdges) {
	        // Exit vertex'e ulaştıysak dur
	        if (esg.getExitVertexSet().contains(current)) {
	            return;
	        }
	        
	        // Current vertex'ten çıkan edge'leri al
	        List<Edge> outgoingEdges = adjacencyMap.getOrDefault(current, new ArrayList<>());
	        
	        // Önce uncovered edge'leri, sonra covered edge'leri ayır
	        List<Edge> uncovered = new ArrayList<>();
	        List<Edge> covered = new ArrayList<>();
	        
	        for (Edge edge : outgoingEdges) {
	            if (!edge.getTarget().isPseudoEndVertex()) {
	                if (uncoveredEdges.contains(edge)) {
	                    uncovered.add(edge);
	                } else {
	                    covered.add(edge);
	                }
	            }
	        }
	        
	        // Önce uncovered edge'lerden devam et
	        for (Edge edge : uncovered) {
	            path.add(edge.getTarget());
	            pathEdges.add(edge);
	            dfsTraversal(edge.getTarget(), path, pathEdges);
	            return; // İlk uncovered edge'i takip et ve geri dön
	        }
	        
	        // Uncovered edge kalmadıysa, exit'e giden bir edge varsa oraya git
	        for (Edge edge : outgoingEdges) {
	            if (edge.getTarget().isPseudoEndVertex()) {
	                path.add(edge.getTarget());
	                pathEdges.add(edge);
	                return;
	            }
	        }
	        
	        // Exit'e direkt yol yoksa, covered edge'lerden devam et
	        if (!covered.isEmpty()) {
	            Edge nextEdge = covered.get(0);
	            path.add(nextEdge.getTarget());
	            pathEdges.add(nextEdge);
	            dfsTraversal(nextEdge.getTarget(), path, pathEdges);
	        }
	    }
	    
	    /**
	     * Başlangıç vertex'i seç - uncovered edge'i olan bir entry vertex
	     */
	    private Vertex selectStartVertex() {
	        // Önce uncovered edge'i olan entry vertex'lere bak
	        for (Vertex entry : esg.getEntryVertexSet()) {
	            List<Edge> edges = adjacencyMap.getOrDefault(entry, new ArrayList<>());
	            for (Edge edge : edges) {
	                if (uncoveredEdges.contains(edge)) {
	                    return entry;
	                }
	            }
	        }
	        
	        // Yoksa herhangi bir entry vertex döndür
	        return esg.getEntryVertexSet().iterator().next();
	    }
	    
	    /**
	     * Alternatif metod: Tek bir uzun sequence üret (tüm edge'leri içeren)
	     */
	    public EventSequence generateSingleSequence() {
	        EventSequence eventSequence = new EventSequence();
	        List<Vertex> path = new ArrayList<>();
	        Set<Edge> visited = new LinkedHashSet<>();
	        Set<Edge> targetEdges = new LinkedHashSet<>(esg.getRealEdgeList());
	        
	        // Entry vertex'lerden birinden başla
	        Vertex current = esg.getEntryVertexSet().iterator().next();
	        path.add(current);
	        
	        while (visited.size() < targetEdges.size()) {
	            List<Edge> outgoing = adjacencyMap.getOrDefault(current, new ArrayList<>());
	            Edge nextEdge = null;
	            
	            // Önce unvisited edge bul
	            for (Edge edge : outgoing) {
	                if (!edge.getTarget().isPseudoEndVertex() && 
	                    targetEdges.contains(edge) && !visited.contains(edge)) {
	                    nextEdge = edge;
	                    break;
	                }
	            }
	            
	            // Unvisited edge yoksa, başka bir vertex'e geç (backtracking)
	            if (nextEdge == null) {
	                Vertex backtrack = findVertexWithUnvisitedEdge(targetEdges, visited);
	                if (backtrack == null) break;
	                
	                // Backtrack vertex'e en kısa yolu bul ve ekle
	                List<Vertex> shortestPath = findShortestPath(current, backtrack, visited);
	                if (shortestPath != null && shortestPath.size() > 1) {
	                    path.addAll(shortestPath.subList(1, shortestPath.size()));
	                    current = backtrack;
	                    continue;
	                } else {
	                    break;
	                }
	            }
	            
	            // Next edge'i takip et
	            visited.add(nextEdge);
	            current = nextEdge.getTarget();
	            path.add(current);
	        }
	        
	        // Exit vertex'e git
	        if (!current.isPseudoEndVertex()) {
	            Vertex exit = esg.getPseudoEndVertex();
	            List<Vertex> exitPath = findShortestPath(current, exit, new HashSet<>());
	            if (exitPath != null && exitPath.size() > 1) {
	                path.addAll(exitPath.subList(1, exitPath.size()));
	            }
	        }
	        
	        // EventSequence nesnesine dönüştür
	        for (Vertex vertex : path) {
	            eventSequence.getEventSequence().add(vertex);
	        }
	        
	        return eventSequence;
	    }
	    
	    /**
	     * Unvisited edge'i olan bir vertex bul
	     */
	    private Vertex findVertexWithUnvisitedEdge(Set<Edge> targetEdges, Set<Edge> visited) {
	        for (Vertex v : esg.getRealVertexList()) {
	            List<Edge> edges = adjacencyMap.getOrDefault(v, new ArrayList<>());
	            for (Edge edge : edges) {
	                if (targetEdges.contains(edge) && !visited.contains(edge)) {
	                    return v;
	                }
	            }
	        }
	        return null;
	    }
	    
	    /**
	     * BFS ile iki vertex arasında en kısa yolu bul
	     */
	    private List<Vertex> findShortestPath(Vertex start, Vertex target, Set<Edge> avoidEdges) {
	        Queue<Vertex> queue = new LinkedList<>();
	        Map<Vertex, Vertex> parent = new HashMap<>();
	        Set<Vertex> visited = new HashSet<>();
	        
	        queue.offer(start);
	        visited.add(start);
	        parent.put(start, null);
	        
	        while (!queue.isEmpty()) {
	            Vertex current = queue.poll();
	            
	            if (current.equals(target)) {
	                // Path'i reconstruct et
	                List<Vertex> path = new ArrayList<>();
	                Vertex node = target;
	                while (node != null) {
	                    path.add(0, node);
	                    node = parent.get(node);
	                }
	                return path;
	            }
	            
	            List<Edge> outgoing = adjacencyMap.getOrDefault(current, new ArrayList<>());
	            for (Edge edge : outgoing) {
	                Vertex next = edge.getTarget();
	                if (!visited.contains(next) && !avoidEdges.contains(edge)) {
	                    visited.add(next);
	                    parent.put(next, current);
	                    queue.offer(next);
	                }
	            }
	        }
	        
	        return null;
	    }
	    	    	    
	    private Edge findEdgeBetween(Vertex source, Vertex target) {
	        for (Edge edge : esg.getEdgeList()) {
	            if (edge.getSource().equals(source) && edge.getTarget().equals(target)) {
	                return edge;
	            }
	        }
	        return null;
	    }
	    
	    public Set<EventSequence> getTestSequences() {
	        return new LinkedHashSet<>(testSequences);
	    }
	  
	}
