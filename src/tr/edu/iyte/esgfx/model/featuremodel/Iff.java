package tr.edu.iyte.esgfx.model.featuremodel;

public class Iff extends Implication{
	
	public Iff() {
		super();
	}
	
	public Iff(Implicant leftHandSide, Implicant rightHandSide) {
		super(leftHandSide, rightHandSide);
	}
	
	@Override
	public String toString() {
		return getLeftHandSide().implicantToString() + " IFF " + getRightHandSide().implicantToString();
	}
	
}

