package tr.edu.iyte.esgfx.cases.SodaVendingMachine;

import tr.edu.iyte.esgfx.cases.TransformedProductESGFxToDOTFileWriter;
public class TransformedProductESGFxToDOTFileWriter_SVM extends CaseStudyUtilities_SVM {

	public static void main(String[] args) {
		CaseStudyUtilities_SVM.initializeFilePaths();
		
		TransformedProductESGFxToDOTFileWriter transformedProductESGFxToDOTFileWriter 
		= new TransformedProductESGFxToDOTFileWriter();
		
		try {
			transformedProductESGFxToDOTFileWriter.writeTransformedProductESGFxToFile();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
