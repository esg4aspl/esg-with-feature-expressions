package tr.edu.iyte.esgfx.cases.SodaVendingMachine;

import tr.edu.iyte.esgfx.cases.RQ3_RandomWalkMultiSeed_TestGenerator;

public class RQ3_RandomWalkMultiSeed_TestGenerator_SVM extends CaseStudyUtilities_SVM {
	
	public static void main(String[] args) throws Exception {
		
		CaseStudyUtilities_SVM.initializeFilePaths();
		
	
		RQ3_RandomWalkMultiSeed_TestGenerator rnd = new RQ3_RandomWalkMultiSeed_TestGenerator();
		rnd.generateMultiSeedRandomWalkTests();
	}
	

}
