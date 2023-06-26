package tr.edu.iyte.esgfx.model.featuremodel;

public class Implication {
	
	
	private Implicant leftHandSide;
	private Implicant rightHandSide;
	
	private String LHStype;
	private String RHStype;
	
	
	public Implication() {
		
	}
	
	public Implication(Implicant leftHandSide,Implicant rightHandSide) {
		setLeftHandSide(leftHandSide);
		setRightHandSide(rightHandSide);
	}
	
	
	public Implicant getLeftHandSide() {
		return leftHandSide;
	}
	public void setLeftHandSide(Implicant leftHandSide) {
		this.leftHandSide = leftHandSide;
	}
	public Implicant getRightHandSide() {
		return rightHandSide;
	}
	public void setRightHandSide(Implicant rightHandSide) {
		this.rightHandSide = rightHandSide;
	}
	
	public String getLHStype() {
		return LHStype;
	}

	public void setLHStype(String lHStype) {
		LHStype = lHStype;
	}

	public String getRHStype() {
		return RHStype;
	}

	public void setRHStype(String rHStype) {
		RHStype = rHStype;
	}

	@Override
	public String toString(){
		return leftHandSide.implicantToString() + " IMPLIES " + rightHandSide.implicantToString();
	}




}
