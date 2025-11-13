package tr.edu.iyte.esgfx.cases.StudentAttendanceSystem;

import tr.edu.iyte.esgfx.conversion.mxe.MXEFileToESGFxConverter;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;

public class FeatureModelParserApp_StudentAttendanceSystem extends CaseStudyUtilities_SAS {
	
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
