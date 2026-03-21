package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.RQ3_RandomWalkMultiSeed_TestGenerator;

public class RQ3_RandomWalkMultiSeed_TestGenerator_Te extends CaseStudyUtilities_Te {
	
	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_Te.initializeFilePaths();
		
		
		RQ3_RandomWalkMultiSeed_TestGenerator rnd = new RQ3_RandomWalkMultiSeed_TestGenerator();
		rnd.generateMultiSeedRandomWalkTests();
	}
	

}
