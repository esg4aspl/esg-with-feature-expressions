package tr.edu.iyte.esgfx.cases.eMail;

import tr.edu.iyte.esgfx.cases.RQ3_RandomWalkMultiSeed_TestGenerator;

public class RQ3_RandomWalkMultiSeed_TestGenerator_eM extends CaseStudyUtilities_eM {
	
	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_eM.initializeFilePaths();
		
		RQ3_RandomWalkMultiSeed_TestGenerator rnd = new RQ3_RandomWalkMultiSeed_TestGenerator();
		rnd.generateMultiSeedRandomWalkTests();
	}
	

}
