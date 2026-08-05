package tr.edu.iyte.esgfx.testgeneration.util;

import org.jgrapht.Graph;

import tr.edu.iyte.esg.esgbalancing.StronglyConnectedBalancedESGGenerator;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.ESGFx;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class StronglyConnectedBalancedESGFxGeneration {

	public static ESG getStronglyConnectedBalancedESGFxGeneration(ESG ESG) {
		
		ESG ESGFx = new ESGFx(ESG);
		StronglyConnectedBalancedESGGenerator balancedESGGenerator = new StronglyConnectedBalancedESGGenerator();

		Graph<Vertex, Edge> balancedAndStronglyConnectedESG = balancedESGGenerator
				.generateBalancedAndStronglyConnectedESG(ESGFx);
		
		ESGFx = convertJgraphToESGFx(balancedAndStronglyConnectedESG,ESGFx);
		
		for(Vertex vertex : ESGFx.getVertexList()) {
			if(vertex.getDegree() != 0) {
//				System.out.println("Strongly Connected&Balanced Product ESGFX  " + vertex.getID() + " " + vertex + " " + vertex.getDegree());
				String eventName = vertex.toString();
				Set<Edge> edgeSet = ESGFx.getEdgesByEventName(eventName);
//				edgeSet.forEach(e -> System.out.println("   Edge: " + e.toString()));
			}
		}
//		balancedAndStronglyConnectedESG.edgeSet().forEach(e -> System.out.println("ESGFX " + e.getSource() + "->" + e.getTarget()));
//		System.out.println("Strongly Connected Balanced ESGFx is generated");
//		System.out.println("------------------------------------------------------------");
		
		return ESGFx;

	}

	private static ESG convertJgraphToESGFx(Graph<Vertex, Edge> balancedAndStronglyConnectedESG, ESG ESG) {
		ESG ESGFx = new ESGFx(ESG.getID(), ESG.getName());
		//System.out.println(balancedAndStronglyConnectedESG.vertexSet().size());
		Iterator<Vertex> vertexSetIterator = balancedAndStronglyConnectedESG.vertexSet().iterator();
		
		while(vertexSetIterator.hasNext()) {
			Vertex vertex = vertexSetIterator.next();
			
			ESGFx.addVertex(vertex);
		}
		
		//System.out.println(balancedAndStronglyConnectedESG.edgeSet().size());
		// The balancing step emits the edges it adds in an order that varies
		// between runs. That order reaches the adjacency map, and the Euler
		// cycle generators take the head of each adjacency list, so leaving it
		// alone makes the generated test suite differ run to run on an
		// unchanged model.
		//
		// Only the added edges are reordered, and they keep their place behind
		// the model's own edges: the generators walk the original edges first,
		// and disturbing that ordering costs coverage. Added edges are keyed on
		// event names rather than IDs so that a model read back from a DOT file
		// and one built in memory, which number their vertices differently,
		// still agree.
		Set<Edge> balancedEdgeSet = balancedAndStronglyConnectedESG.edgeSet();
		Set<Edge> originalEdgeSet = new LinkedHashSet<>(ESG.getEdgeList());

		List<Edge> orderedEdgeList = new ArrayList<>(balancedEdgeSet.size());
		List<Edge> addedEdgeList = new ArrayList<>();

		for(Edge edge : balancedEdgeSet) {
			if(originalEdgeSet.contains(edge)) {
				orderedEdgeList.add(edge);
			} else {
				addedEdgeList.add(edge);
			}
		}

		addedEdgeList.sort(Comparator
				.comparing((Edge edge) -> edge.getSource().getEvent().getName())
				.thenComparing(edge -> edge.getTarget().getEvent().getName()));
		orderedEdgeList.addAll(addedEdgeList);

		for(Edge edge : orderedEdgeList) {
			((ESGFx)ESGFx).addEdge(edge);
		}
		
//		System.out.println(((ESGFx)ESGFx).getAdjacencyMap());
		//System.out.println(ESGFx);
		return ESGFx;
	}

}