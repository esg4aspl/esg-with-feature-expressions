package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esgfx.conversion.mxe.MXEFileToESGFxConverter;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;

public class FeatureModelParserApp_Svia extends CaseStudyUtilities_Svia {

	public static void main(String[] args) {
		
		CaseStudyUtilities_Svia.coverageLength = 2;
		CaseStudyUtilities_Svia.initializeFilePaths();

		MXEFileToESGFxConverter MXEFileToESGFxConverter = new MXEFileToESGFxConverter();
		FeatureModel featureModel = new FeatureModel();
		try {
			featureModel = MXEFileToESGFxConverter.parseFeatureModel(CaseStudyUtilities_Svia.featureModelFilePath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(featureModel.toString());

	}

}
