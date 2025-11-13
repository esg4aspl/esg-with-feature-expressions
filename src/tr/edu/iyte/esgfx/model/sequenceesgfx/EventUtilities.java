package tr.edu.iyte.esgfx.model.sequenceesgfx;

import tr.edu.iyte.esg.model.Event;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

public class EventUtilities {
	
	public static final String CDELIM = "_";
	
	/**
	 - Contexted string form of an event is constructed by appending the name and the id of the event.
	 - "_" is used as delimiter.
	 */
	public static String getContextedStringForm(Event event) {
		String str = event.getName() + CDELIM + event.getID();
//		System.out.println("EventUtilities method1 " + str);
		return str;
	}
	
	public static String getContextedStringForm(Event event, FeatureExpression featureExpression) {
		String str = event.getName() /*+ "/" +featureExpression.toString()*/ + CDELIM + event.getID();
//		System.out.println("EventUtilities method 2 " + str);
		return str;
	}
}
