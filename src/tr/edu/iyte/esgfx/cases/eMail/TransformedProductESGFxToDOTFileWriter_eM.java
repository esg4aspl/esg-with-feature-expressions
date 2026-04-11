package tr.edu.iyte.esgfx.cases.eMail;

import tr.edu.iyte.esgfx.cases.TransformedProductESGFxToDOTFileWriter;
public class TransformedProductESGFxToDOTFileWriter_eM extends CaseStudyUtilities_eM {

	public static void main(String[] args) {
		CaseStudyUtilities_eM.initializeFilePaths();
		
		TransformedProductESGFxToDOTFileWriter transformedProductESGFxToDOTFileWriter 
		= new TransformedProductESGFxToDOTFileWriter();
		
		try {
			transformedProductESGFxToDOTFileWriter.writeTransformedProductESGFxToFile();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
