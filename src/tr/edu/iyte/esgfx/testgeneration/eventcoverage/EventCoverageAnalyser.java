package tr.edu.iyte.esgfx.testgeneration.eventcoverage;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.util.Set;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;

import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

public class EventCoverageAnalyser {
	
	public  void esgEventSequenceSetPrinter(Set<EventSequence> composedSequences) {
		for (EventSequence es : composedSequences) {
			System.out.println(/* es.length() + " - " + */es);
		}
		System.out.println();
	}

	public double analyseEventCoverage(ESG ESGFx, Set<EventSequence> CESsOfESGFx,
			Map<String, FeatureExpression> featureExpressionMap) {

		List<String> mustCoveredEvents = detectMustCoveredEvents(ESGFx);
//		System.out.println("mustCoveredEvents " + mustCoveredEvents);
		
		List<String> coveredEvents = detectCoveredEvents(CESsOfESGFx);
//		System.out.println("coveredEvents " + coveredEvents);
		
		List<String> uncoveredEvents = detectUncoveredEvents(mustCoveredEvents, coveredEvents);
//		System.out.println("uncoveredEvents " + uncoveredEvents);
		
		double coverage = percentageOfCoverage(coveredEvents, uncoveredEvents);
//		System.out.println("coverage " + coverage);
		
		return coverage;

	}
	
	private List<String> detectUncoveredEvents(List<String> mustCoveredEvents, List<String> coveredEventList){
		
		List<String> uncoveredEventList = new LinkedList<String>();
		if(mustCoveredEvents.size() > coveredEventList.size()) {
			for(String event : mustCoveredEvents) {
				if(!coveredEventList.contains(event)) {
					uncoveredEventList.add(event);
				}
			}
		}else if(mustCoveredEvents.size() == coveredEventList.size()) {
			return uncoveredEventList;
		}
			
		return uncoveredEventList;
	}

	private List<String> detectMustCoveredEvents(ESG ESGFx) {
//		System.out.println("detectMustCoveredEvents ");
		List<String> mustCoveredEvents = new LinkedList<>();
		Iterator<Vertex> vertexListItearator = ESGFx.getVertexList().iterator();
//		System.out.println(eventListItearator.hasNext());

		while (vertexListItearator.hasNext()) {
			Vertex vertex = vertexListItearator.next();

			if (!vertex.isPseudoStartVertex() && !vertex.isPseudoEndVertex()) {
				VertexRefinedByFeatureExpression vertexRefinedByFeatureExpression = (VertexRefinedByFeatureExpression) vertex;
				FeatureExpression featureExpression = vertexRefinedByFeatureExpression.getFeatureExpression();

				if (featureExpression.evaluate()) {
					mustCoveredEvents.add(vertexRefinedByFeatureExpression.getEvent().getName().trim());
				}
//				System.out.println(vertexRefinedByFeatureExpression.getEvent().getName() + " " + featureExpression.evaluate());
			}
		}
		return mustCoveredEvents;
	}

	private List<String> detectCoveredEvents(Set<EventSequence> CESsOfESGFx) {
		List<String> coveredEventList = new LinkedList<>();

		Iterator<EventSequence> CESsOfESGFxIterator = CESsOfESGFx.iterator();

		while (CESsOfESGFxIterator.hasNext()) {
			EventSequence eventSequence = CESsOfESGFxIterator.next();
			List<Vertex> vertexList = eventSequence.getEventSequence();

			Iterator<Vertex> vertexListItearator = vertexList.iterator();
			while (vertexListItearator.hasNext()) {
				Vertex vertex = vertexListItearator.next();
				coveredEventList.add(vertex.getEvent().getName().trim());
			}
		}
		return coveredEventList;
	}

	private static double percentageOfCoverage(List<String> coveredEventList, List<String> uncoveredEventList) {

		double coverage = ((double) uncoveredEventList.size()) / ((double) coveredEventList.size()) * 100.0;

		if (uncoveredEventList.size() == 0) {
			return 100.0;
		} else {
//			System.out.printf("Coverage %.2f %s\n", 100.0 - coverage, "%");
			return 100.0 - coverage;
		}

	}

}
