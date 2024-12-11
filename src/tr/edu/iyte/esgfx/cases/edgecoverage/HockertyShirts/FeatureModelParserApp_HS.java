package tr.edu.iyte.esgfx.cases.edgecoverage.HockertyShirts;

import java.util.Map;
import java.util.Set;

import tr.edu.iyte.esgfx.conversion.mxe.MXEFileToESGFxConverter;
import tr.edu.iyte.esgfx.model.featuremodel.Feature;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;

public class FeatureModelParserApp_HS extends CaseStudyUtilities_HS {
	
	public static void main(String[] args) {
		CaseStudyUtilities_HS.initializeFilePaths();
		MXEFileToESGFxConverter MXEFileToESGFxConverter = new MXEFileToESGFxConverter();
		FeatureModel featureModel = new FeatureModel();
		try {
			featureModel = MXEFileToESGFxConverter.parseFeatureModel(featureModelFilePath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(featureModel.toString());
		
//		System.out.println("Root: " + featureModel.getRoot().getName());
//		
//		 Map<Feature, Set<Feature>> xorFeatures = featureModel.getXORFeatures();
//		 
//			for (Feature feature : xorFeatures.keySet()) {
//				System.out.println("XOR Feature: " + feature.getName() + " parent " + feature.getParent().getName());
//				for (Feature feature2 : xorFeatures.get(feature)) {
//					System.out.println("XOR Feature: " + feature2.getName() + " parent " + feature2.getParent().getName());
//				}
//			}
	}
	


}
