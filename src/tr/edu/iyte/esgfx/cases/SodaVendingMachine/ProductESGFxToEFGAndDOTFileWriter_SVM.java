package tr.edu.iyte.esgfx.cases.SodaVendingMachine;

import tr.edu.iyte.esgfx.cases.ProductESGFxToEFGAndDOTFileWriter;

public class ProductESGFxToEFGAndDOTFileWriter_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) throws Exception {


		coverageLength = 2;
		CaseStudyUtilities_SVM.initializeFilePaths();

		ProductESGFxToEFGAndDOTFileWriter productESGToEFGFileWriter = new ProductESGFxToEFGAndDOTFileWriter();

		productESGToEFGFileWriter.writeProductESGFxToFile();

	}

}
