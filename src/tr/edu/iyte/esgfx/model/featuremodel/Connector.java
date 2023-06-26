package tr.edu.iyte.esgfx.model.featuremodel;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class Connector implements Implicant {
	
	private Set<Feature> featureSet;
	private final String operator;
	
	public Connector(String operator) {
		this.operator = operator;
		featureSet = new LinkedHashSet<Feature>();
	}

	public Set<Feature> getFeatureSet() {
		return featureSet;
	}

	public void addFeature(Feature feature) {
		this.featureSet.add(feature);
	}

	public String getOperator() {
		return operator;
	}
	
	public String searchKey() {
		String str = "";
		for(Feature feature : featureSet) {
			String name = feature.getName();
			str += name.toLowerCase();
		}
		
		return str;
	}
	
	@Override
	public String toString() {
		
		String str = "";
		Iterator<Feature> featureSetIterator = featureSet.iterator();
		int i = 0;
		
		while(featureSetIterator.hasNext()) {
			Feature feature = featureSetIterator.next();
			str += feature.getName();
			i++;
			
			if(i != featureSet.size()) {
				str += " " + operator + " ";
			}
		}
		
		str.trim();
		
		return str;
		
	}

}
