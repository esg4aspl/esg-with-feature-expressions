package tr.edu.iyte.esgfx.mutationtesting.mutationoperators;

import java.util.LinkedHashSet;
import java.util.Set;

import tr.edu.iyte.esg.model.ESG;

public abstract class MutationOperator {
	
	protected String name;
	private Set<ESG> validMutantESGFxSet;
	private Set<ESG> invalidMutantESGFxSet;
	
	public MutationOperator() {
		validMutantESGFxSet = new LinkedHashSet<ESG>();
		invalidMutantESGFxSet = new LinkedHashSet<ESG>();
	}
	
	public String getName() {
		return name;
	}
	
	public Set<ESG> getValidMutantESGFxSet() {
		return validMutantESGFxSet;
	}

	public Set<ESG> getInvalidMutantESGFxSet() {
		return invalidMutantESGFxSet;
	}
	
	public void reportNumberOfMutants() {
		int totalNumberOfMutants = validMutantESGFxSet.size() + invalidMutantESGFxSet.size();
		System.out.println("Total Number of mutants: " + totalNumberOfMutants);
		System.out.println("Number of valid mutants: " + validMutantESGFxSet.size());
        System.out.println("Number of invalid mutants: " + invalidMutantESGFxSet.size());
    }
	
	public abstract void generateMutantESGFxSets(ESG ESGFx);


	
	
}
