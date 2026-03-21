package tr.edu.iyte.esgfx.cases.HockertyShirts;

import tr.edu.iyte.esgfx.cases.SamplesToProductESGFx;

public class ProductESGFxToEFGAndDOTFileWriter_HS extends CaseStudyUtilities_HS {

	public static void main(String[] args) throws Exception {

		coverageLength = 2;
		CaseStudyUtilities_HS.initializeFilePaths();

		SamplesToProductESGFx samplesToProductESGFx = new SamplesToProductESGFx();
		samplesToProductESGFx.writeProductESGFxToFile();
		
	}

}
