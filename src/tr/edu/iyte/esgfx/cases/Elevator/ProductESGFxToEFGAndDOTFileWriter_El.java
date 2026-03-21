package tr.edu.iyte.esgfx.cases.Elevator;

import tr.edu.iyte.esgfx.cases.ProductESGFxToEFGAndDOTFileWriter;

public class ProductESGFxToEFGAndDOTFileWriter_El extends CaseStudyUtilities_El {

	public static void main(String[] args) throws Exception {

		coverageLength = 2;
		CaseStudyUtilities_El.initializeFilePaths();

		ProductESGFxToEFGAndDOTFileWriter productESGToEFGFileWriter = new ProductESGFxToEFGAndDOTFileWriter();

		productESGToEFGFileWriter.writeProductESGFxToFile();

	}

}
