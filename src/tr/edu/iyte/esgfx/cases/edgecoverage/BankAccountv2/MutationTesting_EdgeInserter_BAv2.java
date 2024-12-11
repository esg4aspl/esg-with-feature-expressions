package tr.edu.iyte.esgfx.cases.edgecoverage.BankAccountv2;

import tr.edu.iyte.esgfx.cases.edgecoverage.MutantGeneratorEdgeInserter;

public class MutationTesting_EdgeInserter_BAv2 extends CaseStudyUtilities_BAv2 {

	public static void main(String[] args) throws Exception {
		CaseStudyUtilities_BAv2.initializeFilePaths();

		MutantGeneratorEdgeInserter edgeInserterMutantGenerator = new MutantGeneratorEdgeInserter();
		edgeInserterMutantGenerator.generateMutants();

	}
}
