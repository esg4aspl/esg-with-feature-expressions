package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.TransformedProductESGFxToDOTFileWriter;
public class TransformedProductESGFxToDOTFileWriter_Te extends CaseStudyUtilities_Te {

	public static void main(String[] args) {
		CaseStudyUtilities_Te.initializeFilePaths();
		
		TransformedProductESGFxToDOTFileWriter transformedProductESGFxToDOTFileWriter 
		= new TransformedProductESGFxToDOTFileWriter();
		
		try {
			transformedProductESGFxToDOTFileWriter.writeTransformedProductESGFxToFile();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
