package tr.edu.iyte.esgfx.cases.edgecoverage.eMail;

import tr.edu.iyte.esgfx.cases.edgecoverage.MutantGeneratorEdgeInserter;

public class MutationTesting_EdgeInserter_eM extends CaseStudyUtilities_eM {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_eM.initializeFilePaths();
		
		MutantGeneratorEdgeInserter edgeInserterMutantGenerator = new MutantGeneratorEdgeInserter();
		edgeInserterMutantGenerator.generateMutants();
	}
}
