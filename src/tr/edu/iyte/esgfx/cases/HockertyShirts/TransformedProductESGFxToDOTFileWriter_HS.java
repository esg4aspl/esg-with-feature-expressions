package tr.edu.iyte.esgfx.cases.HockertyShirts;

import tr.edu.iyte.esgfx.cases.TransformedProductESGFxToDOTFileWriter;
public class TransformedProductESGFxToDOTFileWriter_HS extends CaseStudyUtilities_HS {

	public static void main(String[] args) {
		CaseStudyUtilities_HS.initializeFilePaths();
		
		TransformedProductESGFxToDOTFileWriter transformedProductESGFxToDOTFileWriter 
		= new TransformedProductESGFxToDOTFileWriter();
		
		try {
			transformedProductESGFxToDOTFileWriter.writeTransformedProductESGFxToFile();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
