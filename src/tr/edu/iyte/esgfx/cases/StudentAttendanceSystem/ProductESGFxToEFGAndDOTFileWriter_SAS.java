package tr.edu.iyte.esgfx.cases.StudentAttendanceSystem;

import tr.edu.iyte.esgfx.cases.ProductESGFxToEFGAndDOTFileWriter;

public class ProductESGFxToEFGAndDOTFileWriter_SAS extends CaseStudyUtilities_SAS {

	public static void main(String[] args) throws Exception {

		coverageLength = 2;
		CaseStudyUtilities_SAS.initializeFilePaths();

		ProductESGFxToEFGAndDOTFileWriter productESGToEFGFileWriter = new ProductESGFxToEFGAndDOTFileWriter();
		productESGToEFGFileWriter.writeProductESGFxToFile();

	}

}
