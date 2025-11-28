package tr.edu.iyte.esgfx.model.featuremodel;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class Connector implements Implicant {
	
	private Set<Implicant> implicantSet;
	private final String operator;
	
	public Connector(String operator) {
		this.operator = operator;
		implicantSet = new LinkedHashSet<Implicant>();
	}

	public Set<Implicant> getImplicantSet() {
		return implicantSet;
	}

	public void addImplicant(Implicant implicant) {
		this.implicantSet.add(implicant);
	}

	public String getOperator() {
		return operator;
	}
	
	public String searchKey() {
		String str = "";
		for(Implicant implicant : implicantSet) {
			String name = implicant.implicantToString();
			str += name.toLowerCase();
		}
		return str;
	}
	
	@Override
	public String toString() {
		
		String str = "";
		Iterator<Implicant> implicantSetIterator = implicantSet.iterator();
		int i = 0;
		
		while(implicantSetIterator.hasNext()) {
			Implicant implicant = implicantSetIterator.next();
			str += implicant.implicantToString();
			i++;
			
			if(i != implicantSet.size()) {
				str += " " + operator + " ";
			}
		}
		
		str.trim();
		
		return str;
		
	}

}
