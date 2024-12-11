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
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

public class FeatureOmitter extends MutationOperator {
	private Set<ESG> featureESGSet;
	private Map<String, FeatureExpression> featureExpressionMap;
	private int mutantID;
	private Map<String,ESG> featureNameMutantMap;

	public FeatureOmitter() {
		super();
		name = "Feature Omitter";
		featureNameMutantMap = new LinkedHashMap<String,ESG>();
		mutantID = 0;
	}

	public FeatureOmitter(Set<ESG> featureSet) {
		super();
		name = "Feature Omitter";
		setFeatureESGSet(featureSet);
		featureNameMutantMap = new LinkedHashMap<String,ESG>();
		mutantID = 0;
	}
	
	public Map<String,ESG> getFeatureNameMutantMap() {
		return featureNameMutantMap;
	}

	@Override
	public void generateMutantESGFxSets(ESG ESGFx) {

		ESG cloneESGFx = new ESGFx(ESGFx);

		Iterator<ESG> featureSetIterator = featureESGSet.iterator();

		while (featureSetIterator.hasNext()) {
			ESG featureESG = featureSetIterator.next();
			String featureName = featureESG.getName();

			if (featureExpressionMap.get(featureName).evaluate()) {
//				System.out.println(featureESG.getName());
				ESG mutantESGFx = omitFeature(cloneESGFx, featureESG);
				
				featureNameMutantMap.put(featureESG.getName(), mutantESGFx);
			}
		}
	}

	private ESG omitFeature(ESG cloneESGFx, ESG feature) {

		ESG mutantESGFx = new ESGFx(cloneESGFx);
		((ESGFx)mutantESGFx).setID(++mutantID);

		edgeSetHandler(mutantESGFx, feature);
		vertexSetHandler(mutantESGFx, feature);

		ESGValidator ESGValidator = new ESGValidator();

		if (ESGValidator.isValid(mutantESGFx))
			getValidMutantESGFxSet().add(mutantESGFx);
		else
			getInvalidMutantESGFxSet().add(mutantESGFx);

//		System.out.println("Mutant ESGFx: " + mutantESGFx.toString() + " " + ESGValidator.isValid(mutantESGFx));
		return mutantESGFx;
	}

	private void vertexSetHandler(ESG mutantESGFx, ESG featureESG) {
		Set<Vertex> featureESGVertexSet = new LinkedHashSet<Vertex>();
		featureESGVertexSet.addAll(featureESG.getRealVertexList());
		Iterator<Vertex> featureESGVertexSetIterator = featureESGVertexSet.iterator();

		Set<Vertex> ESGFxVertexSet = new LinkedHashSet<Vertex>();
		ESGFxVertexSet.addAll(mutantESGFx.getRealVertexList());

		while (featureESGVertexSetIterator.hasNext()) {
			Vertex featureESGVertex = featureESGVertexSetIterator.next();
			String name = featureESGVertex.getEvent().getName();

			if (!isConnectionPoint(name)) {
				for (Vertex ESGFxVertex : ESGFxVertexSet) {
					if (ESGFxVertex.getEvent().getName().equals(name)) {
						mutantESGFx.removeVertex(ESGFxVertex);
						mutantESGFx.removeEvent(ESGFxVertex.getEvent());
//						System.out.println("Removed Vertex: " + ESGFxVertex.toString());
					}
				}
			}

		}
	}

	private void edgeSetHandler(ESG mutantESGFx, ESG featureESG) {
		Set<Edge> featureESGEdgeSet = new LinkedHashSet<Edge>();
		featureESGEdgeSet.addAll(featureESG.getRealEdgeList());
		Iterator<Edge> featureEdgeSetIterator = featureESGEdgeSet.iterator();

		Set<Edge> mutantESGFxEdgeSet = new LinkedHashSet<Edge>();
		mutantESGFxEdgeSet.addAll(mutantESGFx.getEdgeList());

//		for (Edge ESGFxEdge : mutantESGFxEdgeSet) {System.out.println("Mutant ESGFx Edge: " + ESGFxEdge.toString());}

		while (featureEdgeSetIterator.hasNext()) {
			Edge featureESGedge = featureEdgeSetIterator.next();
			Vertex featureESGsource = featureESGedge.getSource();
			Vertex featureESGtarget = featureESGedge.getTarget();
			
//			System.out.println("FeatureESGSource: " + featureESGsource.toString());
//			System.out.println("FeatureESGTarget: " + featureESGtarget.toString());

			if (isConnectionPoint(featureESGsource.getEvent().getName())) {
//				System.out.println("Source is connection point".toUpperCase());
				String featureName = getFeatureNameInConnectionPoint(featureESGsource.getEvent().getName());
				String eventName = getEventNameInConnectionPoint(featureESGsource.getEvent().getName());
//				System.out.println("FeatureName: " + featureName);
//				System.out.println("EventName: " + eventName);

				if (featureExpressionMap.get(featureName).evaluate()) {

					for (Edge ESGFxEdge : mutantESGFxEdgeSet) {
						Vertex ESGFxEdgeSource = ESGFxEdge.getSource();
						Vertex ESGFxEdgeTarget = ESGFxEdge.getTarget();
						String ESGFxEdgeSourceName = ESGFxEdgeSource.getEvent().getName().trim();
						String ESGFxEdgeTargetName = ESGFxEdgeTarget.getEvent().getName().trim();
						
						
//						System.out.println("ESGFxEdgeSourceName: " + ESGFxEdgeSourceName);
//						System.out.println("ESGFxEdgeTargetName: " + ESGFxEdgeTargetName);
//						System.out.println("---------------------------------------------");
						
						if (ESGFxEdgeSourceName.equals(eventName)
								&& ESGFxEdgeTargetName.equals(featureESGtarget.getEvent().getName())) {
							mutantESGFx.removeEdge(ESGFxEdge);
//							System.out.println("Removed Edge: " + ESGFxEdge.toString());
							
						}
					}
				}

			} else if (isConnectionPoint(featureESGtarget.getEvent().getName())) {
//				System.out.println("Target is connection point".toUpperCase());
				String featureName = getFeatureNameInConnectionPoint(featureESGtarget.getEvent().getName()).trim();
				String eventName = getEventNameInConnectionPoint(featureESGtarget.getEvent().getName()).trim();
				
//				System.out.println("FeatureName: " + featureName);
//				System.out.println("EventName: " + eventName);

				if (featureExpressionMap.get(featureName).evaluate()) {
					for (Edge ESGFxEdge : mutantESGFxEdgeSet) {

						Vertex ESGFxEdgeSource = ESGFxEdge.getSource();
						Vertex ESGFxEdgeTarget = ESGFxEdge.getTarget();
						
						String ESGFxEdgeSourceName = ESGFxEdgeSource.getEvent().getName().trim();
						String ESGFxEdgeTargetName = ESGFxEdgeTarget.getEvent().getName().trim();
//						System.out.println("ESGFxEdgeSourceName: " + ESGFxEdgeSourceName);
//						System.out.println("ESGFxEdgeTargetName: " + ESGFxEdgeTargetName);
//						System.out.println("---------------------------------------------");
						
						if (ESGFxEdgeSourceName.equals(featureESGsource.getEvent().getName().trim())
								&& ESGFxEdgeTargetName.equals(eventName)) {
							mutantESGFx.removeEdge(ESGFxEdge);
//							System.out.println("Removed Edge: " + ESGFxEdge.toString());
						}
					}
				}

			} else {
//				System.out.println("Source and Target is not connection point");
				for (Edge ESGFxEdge : mutantESGFxEdgeSet) {
					Vertex ESGFxEdgeSource = ESGFxEdge.getSource();
					Vertex ESGFxEdgeTarget = ESGFxEdge.getTarget();

					if (ESGFxEdgeSource.getEvent().getName().equals(featureESGsource.getEvent().getName())
							&& ESGFxEdgeTarget.getEvent().getName().equals(featureESGtarget.getEvent().getName())) {
						mutantESGFx.removeEdge(ESGFxEdge);
//						System.out.println("Removed Edge: " + ESGFxEdge.toString());
					}
				}

			}
		}

//		System.out.println("End of EdgeSetHandler");
//		for (Edge ESGFxEdge : mutantESGFx.getRealEdgeList()) {System.out.println("Mutant ESGFx Edge: " + ESGFxEdge.toString());}

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

}
