package tr.edu.iyte.esgfx.cases.edgecoverage.HockertyShirts;

import tr.edu.iyte.esgfx.cases.edgecoverage.MutantGeneratorEdgeInserter;

public class MutationTesting_EdgeInserter_HS extends CaseStudyUtilities_HS {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_HS.initializeFilePaths();
		
		MutantGeneratorEdgeInserter edgeInserterMutantGenerator = new MutantGeneratorEdgeInserter();
		edgeInserterMutantGenerator.generateMutants();
	}
}
