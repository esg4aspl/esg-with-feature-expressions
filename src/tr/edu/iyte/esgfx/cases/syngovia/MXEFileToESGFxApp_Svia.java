package tr.edu.iyte.esgfx.cases.syngovia;

import tr.edu.iyte.esg.model.ESG;

import tr.edu.iyte.esgfx.conversion.mxe.MXEFileToESGFxConverter;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;

public class MXEFileToESGFxApp_Svia extends CaseStudyUtilities_Svia {

	public static void main(String[] args) throws Exception {

		coverageLength = 2;
		CaseStudyUtilities_Svia.initializeFilePaths();

		MXEFileToESGFxConverter MXEFileToESGFxConverter = new MXEFileToESGFxConverter();
		FeatureModel  fm = MXEFileToESGFxConverter.parseFeatureModel(featureModelFilePath);
		System.out.println(fm);
		
		ESG ESG = null;
		try {
			ESG = MXEFileToESGFxConverter.parseMXEFileForESGFxCreation(ESGFxFilePath);

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("ESG: " + ESG);
		System.out.println("ESG: " + ESG.getRealVertexList().size());
		System.out.println("ESG: " + ESG.getRealEdgeList().size());
		

	} 

}
