package tr.edu.iyte.esgfx.cases.SodaVendingMachine;

import tr.edu.iyte.esgfx.cases.ProductESGToEFGFileWriter;

public class ProductESGToEFGFileWriter_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {


		CaseStudyUtilities_SVM.initializeFilePaths();

		ProductESGToEFGFileWriter productESGToEFGFileWriter = new ProductESGToEFGFileWriter();

		productESGToEFGFileWriter.writeToEFGFile();

	}

}
