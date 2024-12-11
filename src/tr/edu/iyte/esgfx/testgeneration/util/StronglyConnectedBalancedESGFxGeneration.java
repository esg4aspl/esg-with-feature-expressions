package tr.edu.iyte.esgfx.testgeneration.util;

import org.jgrapht.Graph;

import tr.edu.iyte.esg.esgbalancing.StronglyConnectedBalancedESGGenerator;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.ESGFx;

import java.util.Iterator;

public class StronglyConnectedBalancedESGFxGeneration {

	public static ESG getStronglyConnectedBalancedESGFxGeneration(ESG ESG) {
		
		ESG ESGFx = new ESGFx(ESG);
		StronglyConnectedBalancedESGGenerator balancedESGGenerator = new StronglyConnectedBalancedESGGenerator();

		Graph<Vertex, Edge> balancedAndStronglyConnectedESG = balancedESGGenerator
				.generateBalancedAndStronglyConnectedESG(ESGFx);
		
		ESGFx = convertJgraphToESG(balancedAndStronglyConnectedESG, ESGFx);
		
		return ESGFx;

	}

	private static ESG convertJgraphToESG(Graph<Vertex, Edge> balancedAndStronglyConnectedESG, ESG ESG) {
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
