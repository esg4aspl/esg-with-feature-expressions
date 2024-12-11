package tr.edu.iyte.esgfx.model.featuremodel;

public class Negation extends Feature {
	
	public Negation() {
		super();
	}
	
	public Negation(String name) {
		super("!" + name);
	}
	
	public Negation(Feature feature) {
		super("!" + feature.getName());
	}

}
