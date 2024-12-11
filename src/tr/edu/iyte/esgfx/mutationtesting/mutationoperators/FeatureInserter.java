package tr.edu.iyte.esgfx.mutationtesting.mutationoperators;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.EdgeSimple;
import tr.edu.iyte.esg.model.Event;
import tr.edu.iyte.esg.model.EventSimple;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esg.model.validation.ESGValidator;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featuremodel.Feature;

public class FeatureInserter extends MutationOperator {

	private Set<ESG> featureESGSet;
	private Map<String, FeatureExpression> featureExpressionMap;
	private int mutantID;
	private Map<String,ESG> featureNameMutantMap;

	public FeatureInserter() {
		super();
		name = "Feature Inserter";
		featureESGSet = new LinkedHashSet<ESG>();
		featureNameMutantMap = new LinkedHashMap<String,ESG>();
		mutantID = 0;
	}

	public FeatureInserter(Set<ESG> featureSet) {
		super();
		name = "Feature Inserter";
		setFeatureESGSet(featureSet);
		featureNameMutantMap = new LinkedHashMap<String,ESG>();
		mutantID = 0;
	}

	@Override
	public void generateMutantESGFxSets(ESG ESGFx) {
//		System.out.println("ESGFx: " + ESGFx.toString());

		ESG cloneESGFx = new ESGFx(ESGFx);

//		System.out.println("Clone: " + cloneESGFx.toString());
		Iterator<ESG> featureSetIterator = featureESGSet.iterator();
		while (featureSetIterator.hasNext()) {
			ESG feature = featureSetIterator.next();
//			System.out.println("Inserted Feature: " + feature.toString());
			ESG mutantESGFx = insertFeature(cloneESGFx, feature);
			System.out.println("Mutant: " + mutantESGFx.toString());
			featureNameMutantMap.put(feature.getName().substring(0, 1), mutantESGFx);
		}
	}

	public ESG insertFeature(ESG cloneESGFx, ESG feature) {
		ESG mutantESGFx = new ESGFx(cloneESGFx);
		((ESGFx)mutantESGFx).setID(++mutantID);

		vertexSetHandler(mutantESGFx, feature);
		System.out.println("Mutant: " + mutantESGFx.getVertexList().toString());
		edgeSetHandler(mutantESGFx, feature);

//		System.out.println("Mutant: " + mutantESGFx.toString());

		ESGValidator ESGValidator = new ESGValidator();
//		ValidationResult validationResult = ESGValidator.validate(mutantESGFx);
		if (ESGValidator.isValid(mutantESGFx))
			getValidMutantESGFxSet().add(mutantESGFx);
		else
			getInvalidMutantESGFxSet().add(mutantESGFx);

		return mutantESGFx;
	}

	private void vertexSetHandler(ESG mutantESGFx, ESG feature) {
		Set<Vertex> featureVertexSet = new LinkedHashSet<Vertex>();
		featureVertexSet.addAll(feature.getRealVertexList());

		Iterator<Vertex> featureVertexSetIterator = featureVertexSet.iterator();

		while (featureVertexSetIterator.hasNext()) {
			Vertex featureVertex = featureVertexSetIterator.next();
			if (!isConnectionPoint(featureVertex.getEvent().getName())) {
				Event newEvent = new EventSimple(mutantESGFx.getNextEventID(), featureVertex.getEvent().getName());
				mutantESGFx.addEvent(newEvent);
				Vertex newVertex = new VertexRefinedByFeatureExpression(mutantESGFx.getNextVertexID(), newEvent,
						new FeatureExpression(new Feature(feature.getName().substring(0, 1)), true));
				mutantESGFx.addVertex(newVertex);
			}
		}

		for (Vertex vertex : mutantESGFx.getVertexList()) {
			if (!vertex.isPseudoStartVertex() && !vertex.isPseudoEndVertex()) {
				//System.out.println("mutantESGFx Vertex: " + ((VertexRefinedByFeatureExpression) vertex).toString());
			}

		}
	}

	private void edgeSetHandler(ESG mutantESGFx, ESG feature) {
		Set<Edge> featureEdgeSet = new LinkedHashSet<Edge>();
		featureEdgeSet.addAll(feature.getRealEdgeList());
		Iterator<Edge> featureEdgeSetIterator = featureEdgeSet.iterator();

		while (featureEdgeSetIterator.hasNext()) {
			Edge featureEdge = featureEdgeSetIterator.next();
			Vertex source = featureEdge.getSource();
			Vertex target = featureEdge.getTarget();

			System.out.println("Source: " + source.toString());
			System.out.println("Target: " + target.toString());

			if (isConnectionPoint(source.getEvent().getName())) {
				System.out.println("Source is connection point");
				String featureName = getFeatureNameInConnectionPoint(source.getEvent().getName()).trim();
				String eventName = getEventNameInConnectionPoint(source.getEvent().getName()).trim();

//				System.out.println("Feature Name: " + featureName);
//				System.out.println("Event Name: " + eventName);

				if (featureExpressionMap.get(featureName).evaluate()) {
					Vertex ESGFxSource = getVertexByEventNameAndFeatureName(mutantESGFx, eventName, featureName);
					System.out.println("ESGFxSource: " + ESGFxSource.toString());
					String targetSearchEventName = target.getEvent().getName() + "/" + feature.getName().substring(0, 1);
					
					Vertex ESGFxTarget = ((ESGFx)mutantESGFx).getVertexByEventName(targetSearchEventName);
					System.out.println("ESGFxTarget: " + ESGFxTarget.toString());
					Edge newEdge = new EdgeSimple(mutantESGFx.getNextEdgeID(), ESGFxSource, ESGFxTarget);

					mutantESGFx.addEdge(newEdge);
					System.out.println("Source is connection point New Edge: " + newEdge.toString());
				}
			} else if (isConnectionPoint(target.getEvent().getName())) {
				System.out.println("Target is connection point");
				String featureName = getFeatureNameInConnectionPoint(target.getEvent().getName());
				String eventName = getEventNameInConnectionPoint(target.getEvent().getName());

//				System.out.println("Feature Name: " + featureName);
//				System.out.println("Event Name: " + eventName);

				if (featureExpressionMap.get(featureName.trim()).evaluate()) {
					String sourceSearchEventName = source.getEvent().getName() + "/" + feature.getName().substring(0, 1);
					Vertex ESGFxSource = ((ESGFx)mutantESGFx).getVertexByEventName(sourceSearchEventName);
//					System.out.println("ESGFxSource: " + ESGFxSource.toString());
					Vertex ESGFxTarget = getVertexByEventNameAndFeatureName(mutantESGFx, eventName, featureName.trim());
//					System.out.println("ESGFxTarget: " + ESGFxTarget.toString());
					Edge newEdge = new EdgeSimple(mutantESGFx.getNextEdgeID(), ESGFxSource, ESGFxTarget);
					mutantESGFx.addEdge(newEdge);
//					System.out.println("Target is connection point New Edge: " + newEdge.toString());
				}

			} else {
				System.out.println("No vertex is connection point");
				String sourceSearchEventName = source.getEvent().getName() + "/" + feature.getName().substring(0, 1);
				String targetSearchEventName = target.getEvent().getName() + "/" + feature.getName().substring(0, 1);
				System.out.println("Source Search Event Name: " + sourceSearchEventName);
				System.out.println("Target Search Event Name: " + targetSearchEventName);
				Vertex ESGFxSource = mutantESGFx.getVertexByEventName(sourceSearchEventName);
				Vertex ESGFxTarget = mutantESGFx.getVertexByEventName(targetSearchEventName);
				Edge newEdge = new EdgeSimple(mutantESGFx.getNextEdgeID(), ESGFxSource, ESGFxTarget);
				mutantESGFx.addEdge(newEdge);
				System.out.println("New Edge: " + newEdge.toString());
			}
		}
	}

	public Vertex getVertexByEventNameAndFeatureName(ESG mutantESGFx, String eventName, String featureName) {

		String searchEventName = eventName + "/" + featureName;
		if (eventName.equals("[") || eventName.equals("]")) {
			searchEventName = eventName;
		}

		for (Vertex vertex : mutantESGFx.getVertexList()) {
//			System.out.println("Vertex: " + vertex.toString());
//			boolean b = vertex.toString().equals(searchEventName);
//			System.out.println(b);
			if (vertex.toString().equals(searchEventName)) {
//				System.out.println("HERE ");
				return vertex;
			}
		}
		return null;

	}

	private boolean isConnectionPoint(String eventName) {
		return eventName.startsWith("(") && eventName.endsWith(")") && eventName.contains(",");
	}

	private String getEventNameInConnectionPoint(String eventName) {
		int commaIndex = eventName.indexOf(",");
		String esg = eventName.substring(1, commaIndex);
		String event = eventName.substring(commaIndex + 1, eventName.length() - 1);
		String[] esgEvent = new String[2];
		esgEvent[0] = esg;
		esgEvent[1] = event;
		return event;
	}

	private String getFeatureNameInConnectionPoint(String eventName) {
		int commaIndex = eventName.indexOf(",");
		String esg = eventName.substring(1, commaIndex);
		String event = eventName.substring(commaIndex + 1, eventName.length() - 1);
		String[] esgEvent = new String[2];
		esgEvent[0] = esg;
		esgEvent[1] = event;
		return esg;
	}

	public Set<ESG> getFeatureESGSet() {
		return featureESGSet;
	}

	public void setFeatureESGSet(Set<ESG> featureSet) {
		this.featureESGSet = featureSet;
	}

	public void addToFeatureESGSet(ESG feature) {
		this.featureESGSet.add(feature);
	}

	public Map<String, FeatureExpression> getFeatureExpressionMap() {
		return featureExpressionMap;
	}

	public void setFeatureExpressionMap(Map<String, FeatureExpression> featureExpressionMap) {
		this.featureExpressionMap = featureExpressionMap;
	}

	public Map<String,ESG> getFeatureNameMutantMap() {
		return featureNameMutantMap;
	}

}
