package tr.edu.iyte.esgfx.cases.eventcoverage.eMail;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esgfx.conversion.mxe.MXEFileToESGFxConverter;


public class MXEFileToESGFxApp_eM {

	public static void main(String[] args) throws Exception {
		
		String ESGFxFilePath = "files/Cases/eMail/eM_ESGFx.mxe";
		String featureModelFilePath = "files/Cases/eMail/configs/model.xml";
		
		
		MXEFileToESGFxConverter MXEFileToESGFxConverter = new MXEFileToESGFxConverter();
		MXEFileToESGFxConverter.parseFeatureModel(featureModelFilePath);

		ESG ESG = null;
		try {
			ESG = MXEFileToESGFxConverter.parseMXEFileForESGFxCreation(ESGFxFilePath);

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Number of vertices: " + ESG.getEdgeList().size());
		System.out.println("Number of edges: " + ESG.getVertexList().size());

	}

}
