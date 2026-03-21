package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.cases.SamplesToProductESGFx;

public class ProductESGFxToEFGAndDOTFileWriter_Svia extends CaseStudyUtilities_Svia {

	public static void main(String[] args) throws Exception {

		coverageLength = 2;
		CaseStudyUtilities_Svia.initializeFilePaths();
		
		SamplesToProductESGFx samplesToProductESGFx = new SamplesToProductESGFx();
		samplesToProductESGFx.writeProductESGFxToFile();
		
	}

}
