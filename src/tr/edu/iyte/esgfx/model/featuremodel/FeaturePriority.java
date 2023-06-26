package tr.edu.iyte.esgfx.model.featuremodel;

public enum FeaturePriority {
	
	MAJOR(5),
	HIGH(4),
	AVERAGE(3),
	MODERATE(2),
	LOW(1);
	
	public final int value;
	
	private FeaturePriority(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}

}
