package tr.edu.iyte.esgfx.mutationtesting.mutationoperators;

import java.util.LinkedHashMap;
import java.util.Map;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.Event;
import tr.edu.iyte.esg.model.EdgeSimple;
import tr.edu.iyte.esg.model.EventSimple;
import tr.edu.iyte.esg.model.Vertex;

import tr.edu.iyte.esg.model.validation.ESGValidator;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featuremodel.Feature;

public class EventInserter extends MutationOperator {

	private String eventName;
	private String featureName;
	private int mutantID;
	private Map<String,ESG> edgeMutantMap;

	public EventInserter() {
		super();
		name = "Event Inserter";
		eventName = "NewEvent";
		featureName = "NewFeature";
		edgeMutantMap = new LinkedHashMap<String,ESG>();
		mutantID = 0;
	}

	public EventInserter(String eventName,String featureName) {
		super();
		name = "Event Inserter";
		setEventName(eventName);
		setFeatureName(featureName);
		edgeMutantMap = new LinkedHashMap<String,ESG>();
		mutantID = 0;
	}
	
	public Map<String, ESG> getEdgeMutantMap() {
		return edgeMutantMap;
	}

	@Override
	public void generateMutantESGFxSets(ESG ESGFx) {

		ESG cloneESGFx = new ESGFx(ESGFx);

		for (Vertex source : cloneESGFx.getRealVertexList()) {
			for (Vertex target : cloneESGFx.getRealVertexList()) {
				insertEvent(cloneESGFx, eventName, source, target);
				
				
			}
		}
	}

	private ESG insertEvent(ESG ESGFx, String eventName, Vertex source, Vertex target) {

		ESG mutantESGFx = new ESGFx(ESGFx);
		((ESGFx)mutantESGFx).setID(++mutantID);

		Event newEvent = new EventSimple(mutantESGFx.getNextEventID(), eventName);
		mutantESGFx.addEvent(newEvent);

		Vertex newVertex = new VertexRefinedByFeatureExpression(mutantESGFx.getNextVertexID(), newEvent, new FeatureExpression(new Feature(featureName)));
		mutantESGFx.addVertex(newVertex);

		Edge edge1 = new EdgeSimple(mutantESGFx.getNextEdgeID(), source, newVertex);
		Edge edge2 = new EdgeSimple(mutantESGFx.getNextEdgeID(), newVertex, target);
		
		String edgeStr1 = source.toString() + "-" + newVertex.toString();
		String edgeStr2 = newVertex.toString() + "-" + target.toString();
		edgeMutantMap.put(edgeStr1 + "," + edgeStr2, mutantESGFx);
		
		mutantESGFx.addEdge(edge1);
		mutantESGFx.addEdge(edge2);

		ESGValidator ESGValidator = new ESGValidator();
		if (ESGValidator.isValid(mutantESGFx))
			getValidMutantESGFxSet().add(mutantESGFx);
		else
			getInvalidMutantESGFxSet().add(mutantESGFx);

//		System.out.println("Mutant ESGFx:" + mutantESGFx.toString());
		return mutantESGFx;
	}
	

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	public String getFeatureName() {
		return featureName;
	}

	public void setFeatureName(String featureName) {
		this.featureName = featureName;
	}

}
