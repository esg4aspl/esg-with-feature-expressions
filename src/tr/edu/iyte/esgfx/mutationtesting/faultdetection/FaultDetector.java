package tr.edu.iyte.esgfx.mutationtesting.faultdetection;

import java.util.Set;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;

import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;

public class FaultDetector {

	private Set<EventSequence> CESsOfESG;

	private Set<String> visitedEventsOnMutant;
	private Set<String> visitedEdgesOnMutant;

	private Set<String> eventsOnEventSequences;
	private Set<String> edgesOnEventSequences;

	private Set<String> insertedEventSet;
	private Set<String> insertedEdgeSet;

	private Set<String> omittedEventSet;
	private Set<String> omittedEdgeSet;

	public FaultDetector() {
		CESsOfESG = new LinkedHashSet<EventSequence>();
	}

	public FaultDetector(Set<EventSequence> CESsOfESG) {
		setCESsOfESG(CESsOfESG);
	}

	public Set<EventSequence> getCESsOfESG() {
		return CESsOfESG;
	}

	public void setCESsOfESG(Set<EventSequence> cESsOfESG) {
		CESsOfESG = cESsOfESG;
	}

	private void initializer() {
		visitedEventsOnMutant = new LinkedHashSet<String>();
		visitedEdgesOnMutant = new LinkedHashSet<String>();

		insertedEventSet = new LinkedHashSet<String>();
		insertedEdgeSet = new LinkedHashSet<String>();

		omittedEventSet = new LinkedHashSet<String>();
		omittedEdgeSet = new LinkedHashSet<String>();

		setVerticesOnEventSequences();
		setEdgesOnEventSequences();

	}

	public void setVerticesOnEventSequences() {
		eventsOnEventSequences = new LinkedHashSet<String>();

		for (EventSequence eventSequence : CESsOfESG) {
			for (Vertex vertex : eventSequence.getEventSequence()) {
				String eventName = vertex.toString();
				eventsOnEventSequences.add(eventName);
			}
		}
	}

	public void setEdgesOnEventSequences() {
		edgesOnEventSequences = new LinkedHashSet<String>();

		for (EventSequence eventSequence : CESsOfESG) {
			for (Vertex vertex : eventSequence.getEventSequence()) {
				if (!vertex.equals(eventSequence.getEndVertex())) {
					List<Vertex> successors = eventSequence.successors(vertex);
					String edge = "<" + vertex.toString() + ", " + successors.get(0).toString() + ">";
					edgesOnEventSequences.add(edge);
				}
			}
		}
	}

	public boolean isAllEventsOnTheSequenceAreOmitted(ESG mutantESGFx, EventSequence eventSequence) {

		boolean isAllEventsOnTheSequenceAreOmitted = true;
		for (Vertex vertex : eventSequence.getEventSequence()) {
//			////System.out.println("(mutantESGFx.getVertexByEventName(vertex.toString()) == null " + (mutantESGFx.getVertexByEventName(vertex.toString()) == null));
//			////System.out.println("vertex.toString()" + vertex.toString());
			isAllEventsOnTheSequenceAreOmitted = isAllEventsOnTheSequenceAreOmitted
					&& (mutantESGFx.getVertexByEventName(vertex.toString()) == null);

		}
		////System.out.println("isAllEventsOnTheSequenceAreOmitted: " + isAllEventsOnTheSequenceAreOmitted);
		return isAllEventsOnTheSequenceAreOmitted;
	}

	public boolean isFaultDetected(ESG mutantESGFx) {
		initializer();

		//System.out.println(CESsOfESG.size());
		
		for (EventSequence eventSequence : CESsOfESG) {
			//System.out.println("Current Event Sequence: " + eventSequence.toString());
			if(isAllEventsOnTheSequenceAreOmitted(mutantESGFx, eventSequence)){
				//System.out.println("All Events on the Sequence are Omitted");
				continue;
			}else {
				//System.out.print("The mutant  traversal according to the event sequence");
				traverseESGForEventSequence(mutantESGFx, eventSequence);
			}
		}
		
		//System.out.println("Visited Vertices: " + visitedEventsOnMutant.toString());
		//System.out.println("Inserted Vertices: " + insertedEventSet.toString());
		//System.out.println("Omitted Vertices: " + omittedEventSet.toString());
		
		//System.out.println("Visited Edges: " + visitedEdgesOnMutant.toString());
		//System.out.println("Inserted Edges: " + insertedEdgeSet.toString());
		//System.out.println("Omitted Edges: " + omittedEdgeSet.toString());

		boolean isEdgeInserted = isEdgeInserted();
		boolean isEventInserted = false;
//		boolean isFeatureInserted = false;
		boolean isEventOmitted = false;
//		boolean isFeatureOmitted = false;

		if (isEdgeInserted) {
			isEventInserted = isEventInserted();
			if (isEventInserted) {
				////System.out.println("Fault Detected: Event Inserted");
				////System.out.println("Event Inserted: " + insertedEventSet.toString());
			} else {
				////System.out.println("Fault Detected: Edge Inserted");
				////System.out.println("Edge Inserted: " + insertedEdgeSet.toString());
			}
		}

		boolean isEdgeOmitted = isEdgeOmitted();
		if (isEdgeOmitted) {
			isEventOmitted = isEventOmitted();
			if (isEventOmitted) {
				////System.out.println("Fault Detected: Event Omitted");
				////System.out.println("Event Omitted: " + omittedEventSet.toString());
			} else {
				////System.out.println("Fault Detected: Edge Omitted");
				////System.out.println("Edge Omitted: " + omittedEdgeSet.toString());
			}
		}

		boolean isFaultDetected = isEdgeInserted || isEdgeOmitted || isEventInserted || isEventOmitted;
//				|| isFeatureInserted || isFeatureOmitted;

		return isFaultDetected;
	}

	public boolean isEdgeInserted() {

		boolean allVisitedEdgesOnEventSequences = true;
		for (String visitedEdge : visitedEdgesOnMutant) {
			String modifiedVisitedEdge = visitedEdge.replace("<", "").replace(">", "").trim();
			boolean isVisitedEdgeOnEventSequences = isVisitedEdgeOnEventSequences(modifiedVisitedEdge);
			if (!isVisitedEdgeOnEventSequences) {
				insertedEdgeSet.add(visitedEdge);
			}
			allVisitedEdgesOnEventSequences = allVisitedEdgesOnEventSequences && isVisitedEdgeOnEventSequences;
		}
		boolean isEdgeInserted = !allVisitedEdgesOnEventSequences;
		return isEdgeInserted;
	}

	private boolean isVisitedEdgeOnEventSequences(String visitedEdge) {
		for (EventSequence eventSequence : CESsOfESG) {
			////System.out.println("Visited Edge:" + visitedEdge +".");
			////System.out.println("Event Sequence: " + eventSequence.toString());
			if (eventSequence.toString().contains(visitedEdge)) {
				return true;
			}
		}
		return false;
	}

	public boolean isEdgeOmitted() {
		boolean allEdgesOnEventSequenceVisited = true;
		for (String edgeOnEventSequences : edgesOnEventSequences) {
			boolean isEdgeVisited = visitedEdgesOnMutant.contains(edgeOnEventSequences);
			if (!isEdgeVisited) {
				omittedEdgeSet.add(edgeOnEventSequences);
			}
			allEdgesOnEventSequenceVisited = allEdgesOnEventSequenceVisited && isEdgeVisited;
		}
		boolean isEdgeOmitted = !allEdgesOnEventSequenceVisited;
		return isEdgeOmitted;

	}

	public boolean isEventInserted() {
		boolean allVisitedVerticesOnEventSequences = true;
		for (String visitedVertex : visitedEventsOnMutant) {
			// String modifiedVisitedVertex = visitedVertex.split("/")[0].trim();
			boolean isVisitedVertexOnEventSequences = isVisitedVertexOnEventSequences(visitedVertex.toString());
			if (!isVisitedVertexOnEventSequences) {
				insertedEventSet.add(visitedVertex);
			}
			allVisitedVerticesOnEventSequences = allVisitedVerticesOnEventSequences && isVisitedVertexOnEventSequences;
		}
		boolean isVertexInserted = !allVisitedVerticesOnEventSequences;
		return isVertexInserted;
	}

	public boolean isVisitedVertexOnEventSequences(String visitedVertex) {
		////System.out.println("Visited Vertex: " + visitedVertex.toString());
		for (EventSequence eventSequence : CESsOfESG) {
			////System.out.println("Event Sequence: " + eventSequence.toString());
			if (eventSequence.toString().contains(visitedVertex)) {
				return true;
			}
		}
		return false;
	}

	public boolean isEventOmitted() {
		boolean allEventsOnEventSequenceVisited = true;
		for (String eventOnEventSequences : eventsOnEventSequences) {
			boolean isEventOnEventSequenceVisited = isEventOnEventSequenceVisited(eventOnEventSequences);
			if (!isEventOnEventSequenceVisited) {
				omittedEventSet.add(eventOnEventSequences);
			}
			allEventsOnEventSequenceVisited = allEventsOnEventSequenceVisited && isEventOnEventSequenceVisited;
		}
		boolean isEventOmitted = !allEventsOnEventSequenceVisited;
		return isEventOmitted;
	}

	private boolean isEventOnEventSequenceVisited(String vertexOnEventSequences) {
		for (String visitedEvent : visitedEventsOnMutant) {
			if (visitedEvent.equals(vertexOnEventSequences)) {
				return true;
			}
		}
		return false;
	}

	public boolean isFeatureInserted() {
		if (isEdgeInserted()) {
//			if (insertedEdgeSet.size() >= 2) {
//				////System.out.println("insertedEdgeSet: " + insertedEdgeSet.toString());
			if (isEventInserted()) {
				if (insertedEventSet.size() >= 2) {
//						////System.out.println("insertedEventSet: " + insertedEventSet.toString());
					return true;
				}
			}
//			}
		}
		return false;
	}

	public boolean isFeatureOmitted() {
		if (isEdgeOmitted()) {
//			if (omittedEdgeSet.size() >= 2) {
			if (isEventOmitted()) {
				if (omittedEventSet.size() >= 2) {
					return true;
				}
			}
//			}
		}
		return false;
	}

	public boolean traverseESGForEventSequence(ESG mutantESGFx, EventSequence eventSequence) {

		//System.out.println("event sequence 1111" + eventSequence.length());
		//System.out.println(eventSequence.toString());

		Vertex startVertex = eventSequence.getStartVertex();
		Vertex startVertexOnMutantESGFx = mutantESGFx.getVertexByEventName(startVertex.toString());

		int start = 0;
		while (startVertexOnMutantESGFx == null) {
			start++;
			////System.out.println("Omitted Event: 1");
			omittedEventSet.add(startVertex.toString());
			startVertex = eventSequence.getEventSequence().get(start);
			startVertexOnMutantESGFx = (VertexRefinedByFeatureExpression) mutantESGFx
					.getVertexByEventName(startVertex.toString());
		}

		//System.out.println("startVertexOnMutantESGFx " + startVertexOnMutantESGFx);

		boolean isStartVertexReachingAllVertices = true;

		Vertex endVertex = eventSequence.getEndVertex();
		Vertex endVertexOnMutantESGFx = mutantESGFx.getVertexByEventName(endVertex.toString());
		
		int eventSequenceLength = eventSequence.length();
		//System.out.println("event sequence length " + eventSequence.length());
		while (endVertexOnMutantESGFx == null) {
			eventSequenceLength--;
			//System.out.println("Omitted Event: 2");
			omittedEventSet.add(endVertex.toString());
			
			endVertex = eventSequence.getEventSequence().get(eventSequenceLength-1);
			//System.out.println("endVertex " + endVertex.toString());
			
			endVertexOnMutantESGFx = (VertexRefinedByFeatureExpression) mutantESGFx
					.getVertexByEventName(endVertex.toString());
		}

		//System.out.println("endVertexOnMutantESGFx " + endVertexOnMutantESGFx);

		boolean isAllVerticesReachingEndVertex = true;
		//System.out.println("Event Sequence " + eventSequence);
		for (Vertex vertex : eventSequence.getEventSequence()) {
			
			
			if (!vertex.equals(startVertex)) {
				traverseESGFromSourceToTarget(mutantESGFx, startVertexOnMutantESGFx, vertex);
				visitedEventsOnMutant.add(startVertexOnMutantESGFx.toString());
			}

			if (!vertex.equals(endVertex) && !vertex.equals(startVertex)) {
				traverseESGFromSourceToTarget(mutantESGFx, vertex, endVertexOnMutantESGFx);
				visitedEventsOnMutant.add(endVertexOnMutantESGFx.toString());
			}
		}
		//System.out.println("Visited Vertices: " + visitedEventsOnMutant.toString());
		//System.out.println("Visited Edges: " + visitedEdgesOnMutant.toString());

		return isStartVertexReachingAllVertices && isAllVerticesReachingEndVertex;

	}

	public void traverseESGFromSourceToTarget(ESG mutantESGFx, Vertex source, Vertex target) {

		//System.out.println("start checkSourceVertexReachingTargetVertex".toUpperCase());
		//System.out.println("source " + source.toString());
		//System.out.println("target " + target.toString());

		Set<Vertex> visitedVertices = new LinkedHashSet<Vertex>();
		Queue<Vertex> queue = new LinkedList<Vertex>();
		visitedVertices.add(source);
		queue.add(source);
		//System.out.println("source " + source.toString());
		Vertex sourceOnMutantESGFx = ((ESGFx) mutantESGFx).getVertexByEventName(source.toString());

		if (sourceOnMutantESGFx == null) {
			//System.out.println("sourceOnMutantESGFx is null");
			//System.out.println("Omitted Event: 3");
			omittedEventSet.add(source.toString());
		} else {
			visitedEventsOnMutant.add(sourceOnMutantESGFx.toString());
		}

		while (queue.size() != 0) {
			source = queue.poll();
			
			
//			List<Vertex> adjacencyList = ((ESGFx) mutantESGFx).getAdjacencyList(source);
			
			List<Vertex> adjacencyListFromEventSequences = getAdjacentVerticesOfVertex(CESsOfESG, source);
			//System.out.println((source.toString() + " adjacencyListFromEventSequences " + adjacencyListFromEventSequences.toString()));
			List<Vertex> adjacencyList = ((ESGFx) mutantESGFx).getAdjacencyList(source);
			adjacencyList.retainAll(adjacencyListFromEventSequences);
			//System.out.println(source.toString() + " adjacencyList " + adjacencyList.toString());

			for (Vertex adjacent : adjacencyList) {
				if (!adjacent.isPseudoEndVertex()) {
					////System.out.println("Adjacent " + adjacent.toString());
//					 If the adjacent vertex has not been visited, add it to the queue
					if (!visitedVertices.contains(adjacent)) {
						visitedVertices.add(adjacent);
						queue.add(adjacent);
						String edgeString = "<" + source.toString() + ", " + adjacent.toString() + ">";
						////System.out.println("edgeString " + edgeString);

						Vertex vertexOnMutant = mutantESGFx.getVertexByEventName(adjacent.toString());

						boolean isVertexNull = vertexOnMutant == null;
						////System.out.println("isVertexNull " + isVertexNull);
						if (isVertexNull) {
							////System.out.println("Omitted Event: 4");
							omittedEventSet.add(adjacent.toString());
						} else {
							////System.out.println("Visited Event: " + adjacent.toString());
							visitedEventsOnMutant.add(adjacent.toString());
						}

						visitedEdgesOnMutant.add(edgeString);

					}
				}
			}

		}
	}
	
	public  List<Vertex> getAdjacentVerticesOfVertex(Set<EventSequence> CESsOfESG, Vertex vertex) {

		List<Vertex> adjacencyList = new LinkedList<Vertex>();
		Iterator<EventSequence> CESsOfESGIterator = CESsOfESG.iterator();

		while (CESsOfESGIterator.hasNext()) {
			EventSequence eventSequence = CESsOfESGIterator.next();
			List<Vertex> eventSequenceAL = new ArrayList<Vertex>(eventSequence.getEventSequence());

			int index = eventSequenceAL.indexOf(vertex);
//			//System.out.println(vertex.toString() + " " + index);

			if (index > -1 && index < (eventSequenceAL.size() - 1)) {
				Vertex adjacent = eventSequenceAL.get(index + 1);
				adjacencyList.add(adjacent);

			}
		}

		
		return adjacencyList;

	}
}