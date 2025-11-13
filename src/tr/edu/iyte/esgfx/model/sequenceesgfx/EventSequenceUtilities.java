package tr.edu.iyte.esgfx.model.sequenceesgfx;

import java.util.LinkedHashSet;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Iterator;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esg.model.sequenceesg.Sequence;

public class EventSequenceUtilities {
	
	
	public static Set<EventSequence> removeRepetitionsFromEventSequenceSet(int coverageLengt, Set<EventSequence> CESsOfESG) {
		
		Iterator<EventSequence> iterator = CESsOfESG.iterator();
		
		Set<EventSequence> newCESsOfESG = new LinkedHashSet<EventSequence>(); 
		
		while(iterator.hasNext()) {
			EventSequence es = iterator.next();
			EventSequence newES = removeRepetitionsFromEventSequence(coverageLengt,es);
			newCESsOfESG.add(newES);
		}
		
		return newCESsOfESG;
		
	}
	
	
	/**
	 * removes repeated sequences in a given event sequence
	 * 
	 * @param coverageLength
	 * @param eventSequence
	 * @return
	 */
	public static EventSequence removeRepetitionsFromEventSequence(int coverageLength, EventSequence eventSequence) {
		int numberOfTransformations = coverageLength - 2;
		EventSequence newEventSequence = new EventSequence();
		List<Vertex> eventSequenceList = new LinkedList<Vertex>();

		/*
		 * System.out.println("Event sequence " + eventSequence.length()); for (int i =
		 * 0; i < eventSequence.length(); i++) { Vertex vertex =
		 * eventSequence.getEventSequence().get(i); System.out.print(" " +
		 * vertex.toString()); } System.out.println();
		 */
		for (int i = 0; i < eventSequence.length(); i++) {

			Vertex vertex = eventSequence.getEventSequence().get(i);
			Sequence<Vertex> sequence = ((VertexRefinedBySequence) vertex).getSequence();
			/*
			 * System.out.println("Sequence " + sequence.getSize()); for (int d = 0; d <
			 * sequence.getSize(); d++) {
			 * 
			 * System.out.print(" " + sequence.getElement(d).getName()); }
			 * System.out.println();
			 */

			if (i == 0) {
				for (int j = 0; j < sequence.getSize(); j++) {
					// System.out.println("sequence j " + j + " " + sequence.getElement(j));
					eventSequenceList.add(sequence.getElement(j));
				}
			} else {
				if (sequence.getSize() == 1) {
					eventSequenceList.add(sequence.getElement(0));

				} else {
					for (int k = numberOfTransformations; k < sequence.getSize(); k++) {
						// System.out.println("sequence k " + k + " " + sequence.getElement(k));
						eventSequenceList.add(sequence.getElement(k));
					}
				}
			}
			// System.out.println();

		}
		newEventSequence.setEventSequence(eventSequenceList);

		return newEventSequence;
	}

}
