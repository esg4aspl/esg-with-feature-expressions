package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.SamplesToProductESGFx;

public class ProductESGFxToEFGAndDOTFileWriter_Te extends CaseStudyUtilities_Te {

	public static void main(String[] args) throws Exception {

		coverageLength = 2;
		CaseStudyUtilities_Te.initializeFilePaths();
		
		SamplesToProductESGFx samplesToProductESGFx = new SamplesToProductESGFx();
		samplesToProductESGFx.writeProductESGFxToFile();

	}

}
