package tr.edu.iyte.esgfx.cases.HockertyShirts;

import tr.edu.iyte.esgfx.cases.ProductESGToEFGFileWriter;


public class ProductESGToEFGFileWriter_HS extends CaseStudyUtilities_HS {

	public static void main(String[] args) throws Exception {


		CaseStudyUtilities_HS.initializeFilePaths();

		ProductESGToEFGFileWriter productESGToEFGFileWriter = new ProductESGToEFGFileWriter();

		productESGToEFGFileWriter.writeToEFGFile();

	}

}
