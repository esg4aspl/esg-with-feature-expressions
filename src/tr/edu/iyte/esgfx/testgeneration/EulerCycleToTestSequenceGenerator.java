package tr.edu.iyte.esgfx.testgeneration;

import java.util.ArrayList;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
//import java.util.Map.Entry;
import java.util.Set;

import tr.edu.iyte.esg.eventsequence.EventSequence;

import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.Conjunction;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

public class EulerCycleToTestSequenceGenerator {

	private Map<EventSequence, FeatureExpression> eventSequenceFeatureExpressionMap;

	public EulerCycleToTestSequenceGenerator() {
		eventSequenceFeatureExpressionMap = new LinkedHashMap<EventSequence, FeatureExpression>();
	}

	public Map<EventSequence, FeatureExpression> getEventSequenceFeatureExpressionMap() {
		return eventSequenceFeatureExpressionMap;
	}

	@SuppressWarnings("null")
	public Set<EventSequence> CESgenerator(List<Vertex> eulerCycle) {

		Set<EventSequence> CESsOfESG = new LinkedHashSet<EventSequence>();

//		System.out.print("Euler cycle: ");eulerCycle.forEach(e -> System.out.print(e.getEvent().getName() + " "));System.out.println();

		List<Vertex> copyVertexList = new ArrayList<Vertex>();
		copyVertexList.addAll(eulerCycle);
		EventSequence completeEventSequence = null;
		FeatureExpression featureExpression = null;
		List<Vertex> eventSequence = null;

		for (int i = copyVertexList.size() - 1; i >= 0; i--) {
			Vertex vertex = copyVertexList.get(i);
//			System.out.println(i + " " + vertex);

			if (vertex.isPseudoStartVertex()) {
				eventSequence = new LinkedList<Vertex>();
				completeEventSequence = new EventSequence();
				featureExpression = new Conjunction();
			} else if (vertex.isPseudoEndVertex()) {
				completeEventSequence.setEventSequence(eventSequence);
				CESsOfESG.add(completeEventSequence);
				eventSequenceFeatureExpressionMap.put(completeEventSequence, featureExpression);

			} else {
				eventSequence.add(vertex);
				VertexRefinedByFeatureExpression vertexRefinedByFeatureExpression = (VertexRefinedByFeatureExpression) vertex;
				((Conjunction) featureExpression).addOperand(vertexRefinedByFeatureExpression.getFeatureExpression());
			}
		}

//		System.out.println("Event Sequence & Feature Expression Map: ");
//		for (Entry<EventSequence, FeatureExpression> entry : eventSequenceFeatureExpressionMap.entrySet()) {
//			System.out.print(entry.getKey() + " - " + ((Conjunction) entry.getValue()).toString() + "\n");
//		}
//		System.out.println("----------------------");

		return CESsOfESG;

	}

}
