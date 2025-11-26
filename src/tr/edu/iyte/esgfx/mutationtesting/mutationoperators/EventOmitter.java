package tr.edu.iyte.esgfx.mutationtesting.mutationoperators;

import java.util.Iterator;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esg.model.validation.ESGValidator;
import tr.edu.iyte.esgfx.model.ESGFx;


public class EventOmitter extends MutationOperator {

	private Map<String,ESG> eventMutantMap;
	private int mutantID;
	
	public EventOmitter() {
		super();
		name = "Event Omitter";
		eventMutantMap = new LinkedHashMap<String,ESG>();
		mutantID = 0;
	}
	
	public Map<String, ESG> getEventMutantMap() {
		return eventMutantMap;
	}

	@Override
	public void generateMutantESGFxSets(ESG ESGFx) {

		ESG cloneESGFx = new ESGFx(ESGFx);

		Set<Vertex> vertexSet = new LinkedHashSet<Vertex>();
		vertexSet.addAll(cloneESGFx.getRealVertexList());

		Iterator<Vertex> vertexSetIterator = vertexSet.iterator();

		while (vertexSetIterator.hasNext()) {
			Vertex vertex = vertexSetIterator.next();
//			System.out.println("Omitted Vertex: " + vertex.toString());
			ESG mutantESGFx = omitEvent(cloneESGFx, vertex);
			eventMutantMap.put(vertex.toString(), mutantESGFx);
		}
	}

	private ESG omitEvent(ESG cloneESGFx, Vertex vertex) {

		ESG mutantESGFx = new ESGFx(cloneESGFx);
		((ESGFx)mutantESGFx).setID(++mutantID);
		ESGValidator ESGValidator = new ESGValidator();
		
		Iterator<Edge> edgeIterator = cloneESGFx.getEdgeList().iterator();

		while (edgeIterator.hasNext()) {
			Edge edge = edgeIterator.next();
			if (edge.getSource().equals(vertex) || edge.getTarget().equals(vertex)) {
				mutantESGFx.removeEdge(edge);
			}
		}
		mutantESGFx.removeVertex(vertex);
		mutantESGFx.removeEvent(vertex.getEvent());

//		System.out.println("Mutant " + mutantESGFx.toString());
		if (ESGValidator.isValid(mutantESGFx))
			getValidMutantESGFxSet().add(mutantESGFx);
		else
			getInvalidMutantESGFxSet().add(mutantESGFx);

		return mutantESGFx;
	}

}
