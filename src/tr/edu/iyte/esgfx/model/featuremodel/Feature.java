package tr.edu.iyte.esgfx.model.featuremodel;

public class Feature implements Implicant, Comparable<Feature> {

	private boolean isMandatory;
	private boolean isAbstract;
	private String name;
	private Feature parent;
	private FeaturePriority priority;
	
	public Feature() {
		setName(null);
	}

	public Feature(String name) {
		setName(name);
	}

	public Feature(String name, boolean isAbstract, boolean isMandatory) {
		setName(name);
		setAbstract(isAbstract);
		setMandatory(isMandatory);
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public boolean isMandatory() {
		return isMandatory;
	}

	public void setMandatory(boolean isMandatory) {
		this.isMandatory = isMandatory;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Feature getParent() {
		return parent;
	}

	public void setParent(Feature parent) {
		this.parent = parent;
	}

	public FeaturePriority getPriority() {
		return priority;
	}

	public void setPriority(FeaturePriority priority) {
		this.priority = priority;
	}

	@Override
	public boolean equals(Object feature) {

		if (feature == this) {
			return true;
		}
		if (!(feature instanceof Feature)) {
			return false;
		}

		return this.name == ((Feature) feature).getName();

	}

	@Override
	public String toString() {

		String s = name + " ";
		if (isAbstract) {
			s += "abstract ";
		} else {
			s += "concrete ";
		}

		if (isMandatory) {
			s += "mandatory ";
		} else {
			s += "optional ";
		}

		if (!(parent == null))
			s += " Parent: " + parent.getName();

		return s;
	}

	@Override
	public String implicantToString() {
		return name;
	}

	@Override
	public int compareTo(Feature o) {

		if (priority.getValue() == o.getPriority().getValue()) {
			return 0;
		} else if (priority.getValue() > o.getPriority().getValue()) {
			return -1;
		} else
			return 1;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}

}
