package tr.edu.iyte.esgfx.model.featuremodel;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class FeatureModel {

	private String softwareProductLineName;
	private Set<Feature> featureSet;
	private Feature root;
	private Map<Feature, Set<Feature>> xorFeatures;
	private Map<Feature, Set<Feature>> orFeatures;
	private Map<Feature, Set<Feature>> andFeatures;
	private Set<Connector> connConstraints;
	private Set<Implication> impConstraints;
	private Set<Implication> iffConstraints;

	public FeatureModel() {
		featureSet = new LinkedHashSet<Feature>();
		xorFeatures = new LinkedHashMap<Feature, Set<Feature>>();
		orFeatures = new LinkedHashMap<Feature, Set<Feature>>();
		andFeatures = new LinkedHashMap<Feature, Set<Feature>>();
		connConstraints = new LinkedHashSet<Connector>();
		impConstraints = new LinkedHashSet<Implication>();
		iffConstraints = new LinkedHashSet<Implication>();
	}

	public String getSoftwareProductLineName() {
		return softwareProductLineName;
	}

	public Set<Feature> getFeatureSet() {
		return featureSet;
	}

	public boolean containsFeature(Feature feature) {
		return featureSet.contains(feature);
	}

	public boolean containsFeature(String featureName) {

		for (Feature feature : featureSet) {
			if (feature.getName().equals(featureName)) {
				return true;
			}
		}
		return false;
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
	
	public boolean isLeaf(Feature feature) {
				
		if(featureSet.contains(feature)) {
			
//			System.out.println(feature.getName());
			
			boolean isAND = isANDFeature(feature);
//			System.out.println("isAND " + isAND);
			boolean isOR = isORFeature(feature);
//			System.out.println("isOR " + isOR);
			boolean isXOR = isXORFeature(feature);
//			System.out.println("isXOR " + isXOR);
			
			return !isAND && !isAND && !isXOR;
			
		}else
//			System.out.println(feature.getName());
			return false;
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

	public boolean isXORFeature(Feature feature) {

		return xorFeatures.containsKey(feature);
	}

	public Set<Feature> getChildXORFeatures(Feature feature) {
		if (xorFeatures.containsKey(feature)) {
			return xorFeatures.get(feature);
		} else {
			return new LinkedHashSet<Feature>();
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

	public boolean isORFeature(Feature feature) {

		return orFeatures.containsKey(feature);
	}

	public Set<Feature> getChildORFeatures(Feature feature) {
		if (orFeatures.containsKey(feature)) {
			return orFeatures.get(feature);
		} else {
			return new LinkedHashSet<Feature>();
		}
	}

	public void addANDFeature(Feature parent, Feature child) {

		if (andFeatures.containsKey(parent)) {
			andFeatures.get(parent).add(child);
		} else {
			Set<Feature> featureSet = new LinkedHashSet<Feature>();
			featureSet.add(child);
			andFeatures.put(parent, featureSet);
		}

	}

	public boolean isANDFeature(Feature feature) {

		return andFeatures.containsKey(feature);
	}

	public Set<Feature> getChildANDFeatures(Feature feature) {
		if (andFeatures.containsKey(feature)) {
			return andFeatures.get(feature);
		} else {
			return new LinkedHashSet<Feature>();
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

	public Set<Implication> getIffConstraints() {
		return iffConstraints;
	}

	public void addIffConstraint(Implication iffConstraint) {

		this.iffConstraints.add(iffConstraint);
	}

	public Feature findFeatureByName(String featureName) {

//		System.out.println("Searching for " + featureName);
		Feature feature = null;
		Iterator<Feature> featureSetIterator = featureSet.iterator();
		while (featureSetIterator.hasNext()) {
			Feature next = featureSetIterator.next();
//			System.out.println("NEXT " + next.getName());

			if (next.getName().equals(featureName)) {
//				System.out.println("FOUND " + featureName);
				feature = next;
			}
		}

		if (feature == null) {
			Iterator<Entry<Feature, Set<Feature>>> orFeaturesEntrySetIterator = orFeatures.entrySet().iterator();
			while (orFeaturesEntrySetIterator.hasNext()) {
				Map.Entry<Feature, Set<Feature>> entry = orFeaturesEntrySetIterator.next();
				if (entry.getKey().getName().equals(featureName)) {
					feature = entry.getKey();
				}
			}

		}

		if (feature == null) {
			Iterator<Entry<Feature, Set<Feature>>> xorFeaturesEntrySetIterator = xorFeatures.entrySet().iterator();
			while (xorFeaturesEntrySetIterator.hasNext()) {
				Map.Entry<Feature, Set<Feature>> entry = xorFeaturesEntrySetIterator.next();
				if (entry.getKey().getName().equals(featureName)) {
					feature = entry.getKey();
				}
			}

		}
		
		if (feature == null) {
			Iterator<Entry<Feature, Set<Feature>>> xorFeaturesEntrySetIterator = andFeatures.entrySet().iterator();
			while (xorFeaturesEntrySetIterator.hasNext()) {
				Map.Entry<Feature, Set<Feature>> entry = xorFeaturesEntrySetIterator.next();
				if (entry.getKey().getName().equals(featureName)) {
					feature = entry.getKey();
				}
			}

		}

		if (feature == null) {
			return new Feature();
		}

		return feature;

	}

	public Connector findConnectorByFeatureName(String str) {

//		System.out.println("Searching for" + str);

		for (Connector connector : connConstraints) {
//			System.out.println("CONN " + connector.searchKey());
			if (connector.searchKey().equals(str)) {
				return connector;
			}
		}

		return null;

	}

	public void removeFeature(Feature feature) {
		if (!feature.equals(root)) {
			featureSet.remove(feature);

			if (xorFeatures.containsKey(feature)) {
				xorFeatures.remove(feature);
			} else {
				Iterator<Map.Entry<Feature, Set<Feature>>> xorFeaturesEntrySetIterator = xorFeatures.entrySet()
						.iterator();
				while (xorFeaturesEntrySetIterator.hasNext()) {
					Map.Entry<Feature, Set<Feature>> entry = xorFeaturesEntrySetIterator.next();
					if (entry.getValue().contains(feature)) {
						entry.getValue().remove(feature);
						System.out.println(feature.getName() + " is removed XOR");
					}

					if (entry.getValue().size() < 2) {
						xorFeaturesEntrySetIterator.remove();
					}

				}
			}
			if (orFeatures.containsKey(feature)) {
				orFeatures.remove(feature);
			} else {
				for (Feature keyFeature : orFeatures.keySet()) {
					Set<Feature> subFeatureSet = orFeatures.get(keyFeature);
					Iterator<Feature> subFeatureSetIterator = subFeatureSet.iterator();
					while (subFeatureSetIterator.hasNext()) {
						Feature next = subFeatureSetIterator.next();
						if (next.equals(feature)) {
							subFeatureSetIterator.remove();
							System.out.println(feature.getName() + " is removed OR");
						}
					}
				}
			}
			
			if (andFeatures.containsKey(feature)) {
				andFeatures.remove(feature);
			} else {
				for (Feature keyFeature : andFeatures.keySet()) {
					Set<Feature> subFeatureSet = andFeatures.get(keyFeature);
					Iterator<Feature> subFeatureSetIterator = subFeatureSet.iterator();
					while (subFeatureSetIterator.hasNext()) {
						Feature next = subFeatureSetIterator.next();
						if (next.equals(feature)) {
							subFeatureSetIterator.remove();
							System.out.println(feature.getName() + " is removed OR");
						}
					}
				}
			}

			Iterator<Connector> connConstraintsIterator = connConstraints.iterator();
			while (connConstraintsIterator.hasNext()) {
				Set<Feature> connFeature = connConstraintsIterator.next().getFeatureSet();
				Iterator<Feature> connFeatureIterator = connFeature.iterator();
				while (connFeatureIterator.hasNext()) {
					Feature next = connFeatureIterator.next();
//					System.out.println("NEXT " + next.getName());
//					System.out.println("FEATURE " + feature.getName());
					if (next.equals(feature)) {
//						System.out.println("CONN " + feature.getName());
						connFeatureIterator.remove();
					}
				}

				if (connFeature.size() < 3) {
//					System.out.println("HERE ");
					connConstraintsIterator.remove();
				}
			}
		}

	}

	@Override
	public String toString() {

		String str = softwareProductLineName + "\n";
		str += "Root feature" + " " + root.getName() + "\n";
		str += featureSetToString();
		str += mapToString("OR FEATURES", "OR", orFeatures);
		str += mapToString("ALTERNATIVE FEATURES", "ALT", xorFeatures);
		str += mapToString("AND FEATURES", "AND", andFeatures);
		str += constraintsToString("CONSTRAINTS")+ "\n";
		str += "Number of features: " + featureSet.size() + "\n";
		str += "Number of concrete features: " + countConcreteFeatures() + "\n";

		return str;
	}
	
	private int countConcreteFeatures() {
		int count = 0;
		for (Feature feature : featureSet) {
			if (!feature.isAbstract()) {
				count++;
			}
		}
		
		return count;
	}

	private String featureSetToString() {

		String str = "";
		for (Feature feature : featureSet) {
			str += feature.toString() + "\n";
		}

		return str;
	}

	private String mapToString(String header, String abr, Map<Feature, Set<Feature>> featureMap) {

		String str = header + "\n";
		for (Feature key : featureMap.keySet()) {
			str += key.getName().toUpperCase() + " " + abr + " ";
			for (Feature value : featureMap.get(key)) {
				str += value.getName() + " ";
			}
			str += "\n";
		}
		str += "\n";

		return str;

	}

	private String constraintsToString(String header) {
		String str = header + "\n";

		for (Connector connector : connConstraints) {
			str += connector.toString() + "\n";
		}

		for (Implication implication : impConstraints) {
			str += implication.toString() + "\n";
		}

		for (Implication iff : iffConstraints) {
			str += iff.toString() + "\n";
		}

		return str;
	}

}
