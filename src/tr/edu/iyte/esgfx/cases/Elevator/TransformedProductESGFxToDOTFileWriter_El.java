package tr.edu.iyte.esgfx.cases.Elevator;

import tr.edu.iyte.esgfx.cases.TransformedProductESGFxToDOTFileWriter;
public class TransformedProductESGFxToDOTFileWriter_El extends CaseStudyUtilities_El {

	public static void main(String[] args) {
		CaseStudyUtilities_El.initializeFilePaths();
		
		TransformedProductESGFxToDOTFileWriter transformedProductESGFxToDOTFileWriter 
		= new TransformedProductESGFxToDOTFileWriter();
		
		try {
			transformedProductESGFxToDOTFileWriter.writeTransformedProductESGFxToFile();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
