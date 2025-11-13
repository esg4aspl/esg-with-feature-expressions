package tr.edu.iyte.esgfx.cases.Tesla;

import tr.edu.iyte.esgfx.cases.HockertyShirts.CaseStudyUtilities_HS;
import tr.edu.iyte.esgfx.conversion.mxe.MXEFileToESGFxConverter;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;

public class FeatureModelParserApp_Tesla extends CaseStudyUtilities_Tesla {
	
	public static void main(String[] args) {
		
		CaseStudyUtilities_Tesla.initializeFilePaths();
		
		MXEFileToESGFxConverter MXEFileToESGFxConverter = new MXEFileToESGFxConverter();
		FeatureModel featureModel = new FeatureModel();
		try {
			featureModel = MXEFileToESGFxConverter.parseFeatureModel(featureModelFilePath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(featureModel.toString());
	}
	


}
