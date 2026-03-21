package tr.edu.iyte.esgfx.cases.Elevator;

import tr.edu.iyte.esgfx.cases.RQ3_RandomWalkMultiSeed_TestGenerator;


public class RQ3_RandomWalkMultiSeed_TestGenerator_El extends CaseStudyUtilities_El {
	
	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_El.initializeFilePaths();
		
		
		RQ3_RandomWalkMultiSeed_TestGenerator rnd = new RQ3_RandomWalkMultiSeed_TestGenerator();
		rnd.generateMultiSeedRandomWalkTests();
	}
	

}
