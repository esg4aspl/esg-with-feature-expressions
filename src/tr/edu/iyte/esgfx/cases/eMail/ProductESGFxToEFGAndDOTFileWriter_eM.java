package tr.edu.iyte.esgfx.cases.eMail;

import tr.edu.iyte.esgfx.cases.ProductESGFxToEFGAndDOTFileWriter;

public class ProductESGFxToEFGAndDOTFileWriter_eM extends CaseStudyUtilities_eM {

	public static void main(String[] args) throws Exception {

		coverageLength = 2;
		CaseStudyUtilities_eM.initializeFilePaths();

		ProductESGFxToEFGAndDOTFileWriter productESGToEFGFileWriter = new ProductESGFxToEFGAndDOTFileWriter();

		productESGToEFGFileWriter.writeProductESGFxToFile();

	}

}
