package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.ProductESGFxToEFGAndDOTFileWriter;

public class ProductESGFxToEFGAndDOTFileWriter_Svia extends CaseStudyUtilities_Svia {

	public static void main(String[] args) throws Exception {

		coverageLength = 2;
		CaseStudyUtilities_Svia.initializeFilePaths();
		
		// syngo.via has 219 products; all of them are written, without sampling.
		ProductESGFxToEFGAndDOTFileWriter writer = new ProductESGFxToEFGAndDOTFileWriter();
		writer.writeProductESGFxToFile();
	}

}
