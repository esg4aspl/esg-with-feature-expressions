package tr.edu.iyte.esgfx.cases.eventcoverage.SVM;

import tr.edu.iyte.esg.model.ESG;

import tr.edu.iyte.esgfx.conversion.mxe.MXEFileToESGFxConverter;


public class MXEFileToESGFxApp_SVM {

	public static void main(String[] args) throws Exception {

		String ESGFxFilePath = "files/Cases/SodaVendingMachine/SVM_ESGFx.mxe";
		String featureModelFilePath = "files/Cases/SodaVendingMachine/configs/model.xml";

		MXEFileToESGFxConverter MXEFileToESGFxConverter = new MXEFileToESGFxConverter();
		MXEFileToESGFxConverter.parseFeatureModel(featureModelFilePath);

		ESG ESG = null;
		try {
			ESG = MXEFileToESGFxConverter.parseMXEFileForESGFxCreation(ESGFxFilePath);

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("ESG: " + ESG);

	}

}
