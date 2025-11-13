package tr.edu.iyte.esgfx.cases.StudentAttendanceSystem;

import tr.edu.iyte.esgfx.cases.ProductESGToEFGFileWriter;

public class ProductESGToEFGFileWriter_SAS extends CaseStudyUtilities_SAS {

	public static void main(String[] args) throws Exception {

		coverageLength = 2;

		CaseStudyUtilities_SAS.initializeFilePaths();

		ProductESGToEFGFileWriter productESGToEFGFileWriter = new ProductESGToEFGFileWriter();
		productESGToEFGFileWriter.writeToEFGFile();

	}

}
