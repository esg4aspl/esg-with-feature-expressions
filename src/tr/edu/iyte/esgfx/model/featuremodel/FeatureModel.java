package tr.edu.iyte.esgfx.model.featuremodel;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class FeatureModel {

	private String softwareProductLineName;
	private Set<Feature> featureSet;
	private Feature root;
	private Map<Feature, Set<Feature>> xorFeatures;
	private Map<Feature, Set<Feature>> orFeatures;
	private Set<Connector> connConstraints;
	
	private Set<Implication> impConstraints;
	

	public FeatureModel() {
		featureSet = new LinkedHashSet<Feature>();
		xorFeatures = new LinkedHashMap<Feature, Set<Feature>>();
		orFeatures = new LinkedHashMap<Feature, Set<Feature>>();
		connConstraints = new LinkedHashSet<Connector>();
		impConstraints = new LinkedHashSet<Implication>();
	}

	public String getSoftwareProductLineName() {
		return softwareProductLineName;
	}

	public Set<Feature> getFeatureSet() {
		return featureSet;
	}
	

	public boolean addFeature(Feature feature) {
		if (!featureSet.contains(feature))
			return featureSet.add(feature);
		else
			return false;
	}

	public Feature getRoot() {
		return root;
	}

	public void setRoot(Feature root) {
		this.root = root;
		this.softwareProductLineName = root.getName();
	}

	public Map<Feature, Set<Feature>> getXORFeatures() {
		return xorFeatures;
	}

	public void addXORFeature(Feature parent, Feature child) {
		if (xorFeatures.containsKey(parent)) {
			xorFeatures.get(parent).add(child);
		} else {
			Set<Feature> featureSet = new LinkedHashSet<Feature>();
			featureSet.add(child);
			xorFeatures.put(parent, featureSet);
		}
	}

	public Map<Feature, Set<Feature>> getORFeatures() {
		return orFeatures;
	}

	public void addORFeature(Feature parent, Feature child) {

		if (orFeatures.containsKey(parent)) {
			orFeatures.get(parent).add(child);
		} else {
			Set<Feature> featureSet = new LinkedHashSet<Feature>();
			featureSet.add(child);
			orFeatures.put(parent, featureSet);
		}

	}

	public Set<Connector> getConnConstraints() {
		return connConstraints;
	}

	public void addConnConstraint(Connector constraint) {
		
		this.connConstraints.add(constraint);
	}
	
	public Set<Implication> getImpConstraints() {
		return impConstraints;
	}

	public void addImpConstraint(Implication impConstraint) {
	
		this.impConstraints.add(impConstraint);
	}
	
	public Feature findFeatureByName(String featureName) {

		Feature feature = new Feature();
		Iterator<Feature> featureSetIterator = featureSet.iterator();
		while (featureSetIterator.hasNext()) {
			Feature next = featureSetIterator.next();

			if (next.getName().equals(featureName)) {
				feature = next;
			}
		}

		return feature;

	}
	
	public Connector findConnectorByFeatureName(String str) {
	
		for(Connector connector : connConstraints) {
			if(connector.searchKey().equals(str)) {
				return connector;
			}
		}
		
		return null;
		
	}
	
	public void removeFeature(Feature feature) {
		if (!feature.equals(root)) {
			featureSet.remove(feature);
			
			if(xorFeatures.containsKey(feature)) {
				xorFeatures.remove(feature);
			}else {
				Iterator<Map.Entry<Feature, Set<Feature>>> xorFeaturesEntrySetIterator = xorFeatures.entrySet().iterator();
				while(xorFeaturesEntrySetIterator.hasNext()) {
					Map.Entry<Feature, Set<Feature>> entry = xorFeaturesEntrySetIterator.next();
					if(entry.getValue().contains(feature)) {
						entry.getValue().remove(feature);
						System.out.println(feature.getName() + " is removed XOR");
					}
					
					if(entry.getValue().size() < 2) { 
						xorFeaturesEntrySetIterator.remove();
					}
					
				}
			}
			if(orFeatures.containsKey(feature)) {
				orFeatures.remove(feature);
			}else {
				for(Feature keyFeature : orFeatures.keySet()) {
					Set<Feature> subFeatureSet = orFeatures.get(keyFeature);
					Iterator<Feature> subFeatureSetIterator = subFeatureSet.iterator();
					while(subFeatureSetIterator.hasNext()) {
						Feature next = subFeatureSetIterator.next();
						if(next.equals(feature)) {
							subFeatureSetIterator.remove();
							System.out.println(feature.getName() + " is removed OR");
						}
					}
				}
			}
			
			Iterator<Connector> connConstraintsIterator = connConstraints.iterator();
			while(connConstraintsIterator.hasNext()) {
				Set<Feature> connFeature = connConstraintsIterator.next().getFeatureSet();
				Iterator<Feature> connFeatureIterator = connFeature.iterator();
				while(connFeatureIterator.hasNext()) {
					Feature next = connFeatureIterator.next();
					System.out.println("NEXT " + next.getName());
					System.out.println("FEATURE " + feature.getName());
					if(next.equals(feature)) {
						System.out.println("CONN " + feature.getName());
						connFeatureIterator.remove();
					}
				}
				
				if(connFeature.size() < 3) {
					System.out.println("HERE ");
					connConstraintsIterator.remove();
				}
			}
		}
		
	
	}

	
	@Override
	public String toString() {
		
		String str = softwareProductLineName + "\n";
		str += featureSetToString();
		str += mapToString("OR FEATURES", "OR", orFeatures);
		str += mapToString("ALTERNATIVE FEATURES", "ALT", xorFeatures);
		str += constraintsToString("CONSTRAINTS");	
		
		return str;
	}
	
	private String featureSetToString() {
		
		String str = "";
		for(Feature feature : featureSet) {
			str += feature.toString() + "\n";
		}
		
		return str;
	}
	
	private String mapToString(String header,String abr, Map<Feature,Set<Feature>> featureMap) {
		
		String str = header +  "\n";
		for(Feature key : featureMap.keySet()) {
			str += key.getName().toUpperCase() + " " + abr + " ";
			for(Feature value : featureMap.get(key)) {
				str += value.getName() + " ";
			}
			str += "\n";
		}
		str += "\n";
		
		
		return str;
		
	}
	
	private String constraintsToString(String header) {
		String str = header +  "\n";
		
		for(Connector connector : connConstraints) {
			str += connector.toString() + "\n";
		}
		
		for(Implication implication: impConstraints) {
			str += implication.toString() + "\n";
		}
		
		return str;
	}

}
