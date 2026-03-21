package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.RQ3_RandomWalkMultiSeed_TestGenerator;

public class RQ3_RandomWalkMultiSeed_TestGenerator_Svia extends CaseStudyUtilities_Svia {
	
	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_Svia.initializeFilePaths();
		
		
		RQ3_RandomWalkMultiSeed_TestGenerator rnd = new RQ3_RandomWalkMultiSeed_TestGenerator();
		rnd.generateMultiSeedRandomWalkTests();
	}
	

}
