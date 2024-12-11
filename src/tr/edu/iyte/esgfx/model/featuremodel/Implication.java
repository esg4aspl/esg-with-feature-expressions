package tr.edu.iyte.esgfx.model.featuremodel;

import java.util.ArrayList;
import java.util.List;

public class Implication {

	private Implicant leftHandSide;
	private Implicant rightHandSide;

	private List<Implicant> implicants;

	private String LHStype;
	private String RHStype;

	public Implication() {
		implicants = new ArrayList<Implicant>(2);
	}

	public Implication(Implicant leftHandSide, Implicant rightHandSide) {
		setLeftHandSide(leftHandSide);
		setRightHandSide(rightHandSide);
	}
	
	public List<Implicant> getImplicants() {
		// TODO Auto-generated method stub
		return implicants;
	}

	public void addImplicant(Implicant implicant) {

		if (!implicants.contains(implicant)) {
			implicants.add(implicant);
		}
	}

	public Implicant getLeftHandSide() {
		return leftHandSide;
	}

	public void setLeftHandSide(Implicant leftHandSide) {
		this.leftHandSide = leftHandSide;
	}

	public void setLeftHandSide() {
		if (!implicants.isEmpty()) {
			this.leftHandSide = implicants.get(0);
		}
	}
	
	public Implicant getRightHandSide() {
		return rightHandSide;
	}

	public void setRightHandSide(Implicant rightHandSide) {
		this.rightHandSide = rightHandSide;
	}

	public void setRightHandSide() {

		if (!implicants.isEmpty()) {
			this.rightHandSide = implicants.get(1);
		}
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
	public String toString() {
		return leftHandSide.implicantToString() + " IMPLIES " + rightHandSide.implicantToString();
	}



}
