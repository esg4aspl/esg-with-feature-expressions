package tr.edu.iyte.esgfx.cases.BankAccountv2;

import tr.edu.iyte.esgfx.cases.RQ3_RandomWalkMultiSeed_TestGenerator;

public class RQ3_RandomWalkMultiSeed_TestGenerator_BAv2 extends CaseStudyUtilities_BAv2 {
	
	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_BAv2.initializeFilePaths();
		
		RQ3_RandomWalkMultiSeed_TestGenerator rnd = new RQ3_RandomWalkMultiSeed_TestGenerator();
		rnd.generateMultiSeedRandomWalkTests();
	}
	

}
