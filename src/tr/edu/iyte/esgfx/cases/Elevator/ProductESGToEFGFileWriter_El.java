package tr.edu.iyte.esgfx.cases.Elevator;

import tr.edu.iyte.esgfx.cases.ProductESGToEFGFileWriter;

public class ProductESGToEFGFileWriter_El extends CaseStudyUtilities_El {

	public static void main(String[] args) throws Exception {

		CaseStudyUtilities_El.initializeFilePaths();

		ProductESGToEFGFileWriter productESGToEFGFileWriter = new ProductESGToEFGFileWriter();

		productESGToEFGFileWriter.writeToEFGFile();

	}

}
