package tr.edu.iyte.esgfx.mutationtesting.mutationoperators;

import java.util.Iterator;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import java.util.Map;
import java.util.Set;

import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.EdgeSimple;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esg.model.validation.ESGValidator;
import tr.edu.iyte.esgfx.model.ESGFx;

public class EdgeRedirector extends MutationOperator {

	private int mutantID;
	private Map<String, ESG> edgeMutantMap;

	public EdgeRedirector() {
		edgeMutantMap = new LinkedHashMap<String, ESG>();
	}

	public Map<String, ESG> getEdgeMutantMap() {
		return edgeMutantMap;
	}

	@Override
	public void generateMutantESGFxSets(ESG ESGFx) {
		// Get all edges from the original graph
		ESG cloneESGFx = new ESGFx(ESGFx);
		Set<Edge> edgeSet = new LinkedHashSet<Edge>();
		edgeSet.addAll(cloneESGFx.getRealEdgeList());

		Iterator<Edge> edgeIterator = edgeSet.iterator();

		while (edgeIterator.hasNext()) {
			Edge edge = edgeIterator.next();

			Vertex source = edge.getSource();
			Vertex oldTarget = edge.getTarget();

			Set<Vertex> vertexSet = new LinkedHashSet<Vertex>();
			vertexSet.addAll(cloneESGFx.getRealVertexList());

			Iterator<Vertex> vertexSetIterator = vertexSet.iterator();

			// Find candidate vertices for redirection (Exclude itself and the current
			// target)
			// Also ensure there isn't already an edge to the new target (Avoid duplicate
			// edges)
			while (vertexSetIterator.hasNext()) {
				Vertex newTarget = vertexSetIterator.next();
				if (!newTarget.equals(oldTarget) && !newTarget.equals(source)) {
					Edge edgeToRedirect = cloneESGFx.getEdgeBySourceEventNameTargetEventName(
							source.getEvent().getName(), newTarget.getEvent().getName());

					if (edgeToRedirect == null) {
						@SuppressWarnings("null")
						String key = source.getEvent().getName() + "-/>" + oldTarget.getEvent().getName() + "->"
								+ newTarget.getEvent().getName();
						ESG mutantESGFx = redirectEdge(cloneESGFx, source, oldTarget, newTarget);
						edgeMutantMap.put(key, mutantESGFx);
					}
				}
			}
		}
	}

	public ESG createSingleMutant(ESG originalESG, Edge edgeToOmit, int currentMutantID) {

		ESG mutantESGFx = new ESGFx(originalESG);

		((ESGFx) mutantESGFx).setID(currentMutantID);
		mutantESGFx.removeEdge(edgeToOmit);

		return mutantESGFx;
	}

//	public ESG createSingleMutant(ESG originalESGFx, Vertex source, Vertex oldTarget, Vertex newTarget,
//			int currentMutantID) {
//		ESG mutantESGFx = new ESGFx(originalESGFx);
//		((ESGFx) mutantESGFx).setID(currentMutantID);
//
//		Edge edgeToRemove = mutantESGFx.getEdgeBySourceEventNameTargetEventName(source.getEvent().getName(),
//				oldTarget.getEvent().getName());
//
//		if (edgeToRemove != null) {
//
//			int ID = edgeToRemove.getID();
//			mutantESGFx.removeEdge(edgeToRemove);
//
//			// 2. Add the new edge (Redirection)
//			mutantESGFx.addEdge(new EdgeSimple(ID, source, newTarget));
//
////				ESGValidator ESGValidator = new ESGValidator();
////				if (ESGValidator.isValid(mutantESGFx))
////					getValidMutantESGFxSet().add(mutantESGFx);
////				else
////					getInvalidMutantESGFxSet().add(mutantESGFx);
//		}
//
//		return mutantESGFx;
//	}

	public ESG createSingleMutant(ESG originalESGFx, int edgeToIndex, int newTargetIndex, int currentMutantID) {

		ESG mutantESGFx = new ESGFx(originalESGFx);
		((ESGFx) mutantESGFx).setID(currentMutantID);

		Edge edgeToRemove = mutantESGFx.getRealEdgeList().get(edgeToIndex);

		Vertex newTarget = mutantESGFx.getRealVertexList().get(newTargetIndex);

		Vertex source = edgeToRemove.getSource();
		int ID = edgeToRemove.getID();

		mutantESGFx.removeEdge(edgeToRemove);
		mutantESGFx.addEdge(new EdgeSimple(ID, source, newTarget));

		return mutantESGFx;
	}

	private ESG redirectEdge(ESG cloneESGFx, Vertex source, Vertex oldTarget, Vertex newTarget) {
		ESG mutantESGFx = new ESGFx(cloneESGFx);
		((ESGFx) mutantESGFx).setID(++mutantID);

		// System.out.println(source.getEvent().getName());

		// After cloning, we must retrieve references to vertices from the mutant graph
		Vertex mutantSource = mutantESGFx.getVertexByEventName(source.toString());
		Vertex mutantOldTarget = mutantESGFx.getVertexByEventName(oldTarget.toString());
		Vertex mutantNewTarget = mutantESGFx.getVertexByEventName(newTarget.toString());

		if (mutantSource != null && mutantOldTarget != null && mutantNewTarget != null) {
			// 1. Remove the old edge
			Edge edgeToRemove = mutantESGFx.getEdgeBySourceEventNameTargetEventName(mutantSource.getEvent().getName(),
					mutantOldTarget.getEvent().getName());
			if (edgeToRemove != null) {

				int ID = edgeToRemove.getID();
				mutantESGFx.removeEdge(edgeToRemove);

				// 2. Add the new edge (Redirection)
				mutantESGFx.addEdge(new EdgeSimple(ID, mutantSource, mutantNewTarget));

				ESGValidator ESGValidator = new ESGValidator();
				if (ESGValidator.isValid(mutantESGFx))
					getValidMutantESGFxSet().add(mutantESGFx);
				else
					getInvalidMutantESGFxSet().add(mutantESGFx);
			}
		}

		return mutantESGFx;
	}
}