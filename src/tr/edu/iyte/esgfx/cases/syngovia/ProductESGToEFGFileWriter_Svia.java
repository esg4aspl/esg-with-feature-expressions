package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.ProductESGToEFGFileWriter;

public class ProductESGToEFGFileWriter_Svia extends CaseStudyUtilities_Svia {

	public static void main(String[] args) throws Exception {

		coverageLength = 2;

		CaseStudyUtilities_Svia.initializeFilePaths();

		ProductESGToEFGFileWriter productESGToEFGFileWriter = new ProductESGToEFGFileWriter();

		productESGToEFGFileWriter.writeToEFGFile();

	}

}
