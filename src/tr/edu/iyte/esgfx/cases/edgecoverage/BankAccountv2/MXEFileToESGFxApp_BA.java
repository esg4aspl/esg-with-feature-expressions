package tr.edu.iyte.esgfx.cases.edgecoverage.BankAccountv2;

import tr.edu.iyte.esg.model.ESG;

import tr.edu.iyte.esgfx.conversion.mxe.MXEFileToESGFxConverter;

public class MXEFileToESGFxApp_BA {

	public static void main(String[] args) {

		String ESGFxFilePath = "files/Cases/BankAccount/BA_ESGFx.mxe";
		String featureModelFilePath = "files/Cases/BankAccount/configs/model.xml";

		MXEFileToESGFxConverter MXEFileToESGFxConverter = new MXEFileToESGFxConverter();
		try {
			MXEFileToESGFxConverter.parseFeatureModel(featureModelFilePath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
