package tr.edu.iyte.esgfx.mutationtesting.mutationoperators;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.EdgeSimple;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esg.model.validation.ESGValidator;
import tr.edu.iyte.esgfx.model.ESGFx;


public class EdgeInserter extends MutationOperator {
	
	private int mutantID;
	private Map<String,ESG> edgeMutantMap;

	public EdgeInserter() {
		super();
		name = "Edge Inserter";
		edgeMutantMap = new LinkedHashMap<String,ESG>();
		mutantID = 0;
	}
	
	public Map<String, ESG> getEdgeMutantMap() {
		return edgeMutantMap;
	}

	@Override
	public void generateMutantESGFxSets(ESG ESGFx) {

		ESG cloneESGFx = new ESGFx(ESGFx);
		
		Set<Edge> edgeSet = new GenerateEdgeSetToBeInserted().generateEdgeSetToBeInserted(cloneESGFx);

		Iterator<Edge> edgeSetIterator = edgeSet.iterator();

		while (edgeSetIterator.hasNext()) {
			Edge edge = edgeSetIterator.next();
			String edgeStr = edge.getSource().toString() + "-" + edge.getTarget().toString();
			//System.out.println("Edge to be inserted: " + edgeStr);
			ESG mutantESGFx = insertEdge(cloneESGFx, edge);
			edgeMutantMap.put(edgeStr, (ESGFx)mutantESGFx);
		}
	}

	private ESG insertEdge(ESG ESGFxClone, Edge edge) {

		ESG mutantESGFx = new ESGFx(ESGFxClone);
		((ESGFx)mutantESGFx).setID(++mutantID);
		Vertex source = edge.getSource();
		Vertex target = edge.getTarget();

		if (mutantESGFx.getVertexList().contains(source) && mutantESGFx.getVertexList().contains(target)) {
			Edge newEdge = new EdgeSimple(mutantESGFx.getNextEdgeID(), source, target);
			mutantESGFx.addEdge(newEdge);

			ESGValidator ESGValidator = new ESGValidator();
			boolean isValid = ESGValidator.isValid(mutantESGFx);
			if (isValid)
				getValidMutantESGFxSet().add(mutantESGFx);
			else
				getInvalidMutantESGFxSet().add(mutantESGFx);
			//System.out.println("Mutant ESGFx: " + mutantESGFx.toString());
		}
		
		return mutantESGFx;
	}

	private class GenerateEdgeSetToBeInserted {
		public Set<Edge> generateEdgeSetToBeInserted(ESG productESGFx) {

			Set<Edge> edgeSetToBeInserted = new LinkedHashSet<Edge>();

			for (Vertex source : productESGFx.getRealVertexList()) {
				for (Vertex target : productESGFx.getRealVertexList()) {

					if (!isThereEdgeBetween(source, target, productESGFx)) {
						Edge edge = new EdgeSimple(productESGFx.getLastEdgeID(), source, target);
//						System.out.println("Edge to be inserted: " + edge.toString());
						edgeSetToBeInserted.add(edge);
					}
				}
			}
			return edgeSetToBeInserted;
		}

		private boolean isThereEdgeBetween(Vertex source, Vertex target, ESG productESGFx) {

			Iterator<Edge> edgeListIterator = productESGFx.getEdgeList().iterator();
			while (edgeListIterator.hasNext()) {
				Edge edge = edgeListIterator.next();
				if (edge.getSource().equals(source) && edge.getTarget().equals(target)) {
					return true;
				}
			}
			return false;

		}

	}

}
