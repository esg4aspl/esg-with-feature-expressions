package tr.edu.iyte.esgfx.cases.eMail;

import tr.edu.iyte.esgfx.cases.ProductESGToEFGFileWriter;

public class ProductESGToEFGFileWriter_eM extends CaseStudyUtilities_eM {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_eM.initializeFilePaths();

		ProductESGToEFGFileWriter productESGToEFGFileWriter = new ProductESGToEFGFileWriter();

		productESGToEFGFileWriter.writeToEFGFile();

	}

}
