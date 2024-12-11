package tr.edu.iyte.esgfx.testgeneration;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esgfx.model.featureexpression.Conjunction;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

public class TestSequenceSelectionBasedOnProductConfiguration {

	public Set<EventSequence> selectTestSequences(
			Map<EventSequence, FeatureExpression> eventSequenceFeatureExpressionMap) {
		Set<EventSequence> CESsOfESG = new LinkedHashSet<EventSequence>();

		Iterator<Entry<EventSequence, FeatureExpression>> eventSequenceFeatureExpressionMapIterator = eventSequenceFeatureExpressionMap
				.entrySet().iterator();

//		System.out.println("Event Sequence & Feature Expression Map: ");
		while (eventSequenceFeatureExpressionMapIterator.hasNext()) {
			Entry<EventSequence, FeatureExpression> eventSequenceFeatureExpression = eventSequenceFeatureExpressionMapIterator
					.next();

//			System.out.print(eventSequenceFeatureExpression.getKey() + " - "
//					+ ((Conjunction) eventSequenceFeatureExpression.getValue()).toString() + " "
//					+ ((Conjunction) eventSequenceFeatureExpression.getValue()).evaluate() + "\n");

			if (((Conjunction) eventSequenceFeatureExpression.getValue()).evaluate()) {
				CESsOfESG.add(eventSequenceFeatureExpression.getKey());
			}

		}

//		System.out.println(CESsOfESG.size());
//		System.out.println(CESsOfESG);
		return CESsOfESG;
	}

}
