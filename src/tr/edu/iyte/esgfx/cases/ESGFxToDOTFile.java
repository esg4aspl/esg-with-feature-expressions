package tr.edu.iyte.esgfx.cases;

import tr.edu.iyte.esgfx.conversion.dot.ESGFxToDOTFileConverter;

public class ESGFxToDOTFile extends CaseStudyUtilities {
	
	public void writeTransformedProductESGFxToFile() throws Exception {
		
		generateFeatureExpressionMapFromFeatureModel(featureModelFile,
				ESGFxFile);
		
		ESGFxToDOTFileConverter.buildDOTFileFromESGFx(ESGFx, DOTFolder + coverageType + "/",
				"ESGFx" );

	}

}
