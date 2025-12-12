package tr.edu.iyte.esgfx.mutationtesting.mutationoperators;

import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Iterator;
import java.util.LinkedHashMap;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;

import tr.edu.iyte.esg.model.validation.ESGValidator;

import tr.edu.iyte.esgfx.model.ESGFx;


public class EdgeOmitter extends MutationOperator {
	
	private int mutantID;
	private Map<String,ESG> edgeMutantMap;

	public EdgeOmitter() {
		super();
		name = "Edge Omitter";
		edgeMutantMap = new LinkedHashMap<String,ESG>();
		mutantID = 0;
	}
	
	public Map<String, ESG> getEdgeMutantMap() {
		return edgeMutantMap;
	}

	@Override
	public void generateMutantESGFxSets(ESG ESGFx) {

		ESG cloneESGFx = new ESGFx(ESGFx);

	
		Set<Edge> edgeSet = new LinkedHashSet<Edge>();
		edgeSet.addAll(cloneESGFx.getRealEdgeList());

		Iterator<Edge> edgeIterator = edgeSet.iterator();

		while (edgeIterator.hasNext()) {
			Edge edge = edgeIterator.next();
			String edgeStr = edge.getSource().toString() + "-" + edge.getTarget().toString();
			//System.out.println("Omitted Edge: " + edge.toString());
			ESG mutantESGFx = omitEdge(cloneESGFx, edge);
			edgeMutantMap.put(edgeStr, mutantESGFx);
		}
	}

	private ESG omitEdge(ESG cloneESGFx, Edge edge) {

		ESG mutantESGFx = new ESGFx(cloneESGFx);
		((ESGFx)mutantESGFx).setID(++mutantID);
		mutantESGFx.removeEdge(edge);

		ESGValidator ESGValidator = new ESGValidator();
		if (ESGValidator.isValid(mutantESGFx))
			getValidMutantESGFxSet().add(mutantESGFx);
		else
			getInvalidMutantESGFxSet().add(mutantESGFx);

//		System.out.println("Mutant ESGFx: " + mutantESGFx.toString());
		return mutantESGFx;
	}
	
	public ESG createSingleMutant(ESG originalESGFx, Edge edgeToOmit, int currentMutantID) {

        
//		System.out.println("reached clone");
		ESG mutantESGFx = new ESGFx(originalESGFx);
        
//        System.out.println("clone is generated");
        
        ((ESGFx)mutantESGFx).setID(currentMutantID);        
        mutantESGFx.removeEdge(edgeToOmit); 
        
//		ESGValidator ESGValidator = new ESGValidator();
//		if (ESGValidator.isValid(mutantESGFx))
//			getValidMutantESGFxSet().add(mutantESGFx);
//		else
//			getInvalidMutantESGFxSet().add(mutantESGFx);

        return mutantESGFx;
    }

}
