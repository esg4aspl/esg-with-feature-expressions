package tr.edu.iyte.esgfx.cases.BankAccountv2;

import tr.edu.iyte.esgfx.cases.ProductESGFxToEFGAndDOTFileWriter;

public class ProductESGFxToEFGAndDOTFileWriter_BAv2 extends CaseStudyUtilities_BAv2 {

	public static void main(String[] args) throws Exception {

		coverageLength = 2;
		CaseStudyUtilities_BAv2.initializeFilePaths();

		ProductESGFxToEFGAndDOTFileWriter productESGToEFGFileWriter = new ProductESGFxToEFGAndDOTFileWriter();
		productESGToEFGFileWriter.writeProductESGFxToFile();

	}

}
