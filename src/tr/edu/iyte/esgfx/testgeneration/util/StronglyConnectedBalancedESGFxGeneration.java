package tr.edu.iyte.esgfx.testgeneration.util;

import org.jgrapht.Graph;

import tr.edu.iyte.esg.esgbalancing.StronglyConnectedBalancedESGGenerator;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.ESGFx;

import java.util.Iterator;
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
				System.out.println("Strongly Connected&Balanced Product ESGFX  " + vertex.getID() + " " + vertex + " " + vertex.getDegree());
				String eventName = vertex.toString();
				Set<Edge> edgeSet = ESGFx.getEdgesByEventName(eventName);
				edgeSet.forEach(e -> System.out.println("   Edge: " + e.toString()));
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
		Iterator<Edge> edgeSetIterator = balancedAndStronglyConnectedESG.edgeSet().iterator();
		
		while(edgeSetIterator.hasNext()) {
			Edge edge = edgeSetIterator.next();
			
			((ESGFx)ESGFx).addEdge(edge);
		}
		
//		System.out.println(((ESGFx)ESGFx).getAdjacencyMap());
		//System.out.println(ESGFx);
		return ESGFx;
	}

}