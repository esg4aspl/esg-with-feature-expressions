package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.ProductESGToEFGFileWriter;

public class ProductESGToEFGFileWriter_Te extends CaseStudyUtilities_Te {

	public static void main(String[] args) throws Exception {

		coverageLength = 2;
		CaseStudyUtilities_Te.initializeFilePaths();

		ProductESGToEFGFileWriter productESGToEFGFileWriter = new ProductESGToEFGFileWriter();

		productESGToEFGFileWriter.writeToEFGFile();

	}

}
