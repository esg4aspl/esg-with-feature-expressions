package tr.edu.iyte.esgfx.cases.eventcoverage.eMail;

import tr.edu.iyte.esgfx.cases.edgecoverage.eMail.CaseStudyUtilities_eM;
import tr.edu.iyte.esgfx.conversion.mxe.MXEFileToESGFxConverter;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;

public class FeatureModelParserApp_eM extends CaseStudyUtilities_eM{
	
	public static void main(String[] args) {
		
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
