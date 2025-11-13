package tr.edu.iyte.esgfx.testgeneration.eventtriplecoverage;

import java.util.LinkedList;


import java.util.List;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esgfx.model.sequenceesgfx.ESGFxtoSequenceESGFxConverter;
import tr.edu.iyte.esgfx.model.sequenceesgfx.SequenceESGFxTransformer;


public class TransformedESGFxGenerator {

	public TransformedESGFxGenerator() {
		
	}
	
	/**
	 * 
	 * @param coverageLength
	 * @param ESGFx
	 * @return
	 */
	public ESG generateTransformedESGFx(int coverageLength, ESG ESGFx) {
		int numberOfTransformations = coverageLength - 2;
		
		
		ESGFxtoSequenceESGFxConverter ESGFxtoSequenceESGFxConverter = new ESGFxtoSequenceESGFxConverter();
		
		ESG oneESGFx = ESGFxtoSequenceESGFxConverter.convert(ESGFx);
		ESG transformedESGFx = oneESGFx;
		
		SequenceESGFxTransformer sequenceESGFxTransformer = new SequenceESGFxTransformer();
		
		
		for (int i = 1; i <= numberOfTransformations; i++) {
			transformedESGFx = sequenceESGFxTransformer.transform(transformedESGFx, oneESGFx);
			
//			System.out.println(transformedESGFx);
		}

		return transformedESGFx;

	}
	
	public List<ESG> generateTransformedESGList(int coverageLength, ESG ESGFx) {
		int numberOfTransformations = coverageLength - 2;
		
		List<ESG> transformedESGFxList = new LinkedList<ESG>();

		ESGFxtoSequenceESGFxConverter ESGFxtoSequenceESGFxConverter = new ESGFxtoSequenceESGFxConverter();
		SequenceESGFxTransformer sequenceESGFxTransformer = new SequenceESGFxTransformer();

		ESG oneESGFx = ESGFxtoSequenceESGFxConverter.convert(ESGFx);
		transformedESGFxList.add(oneESGFx);

		ESG transformedESG = oneESGFx;
		for (int i = 1; i <= numberOfTransformations; i++) {
			transformedESG = sequenceESGFxTransformer.transform(transformedESG, oneESGFx);
			transformedESGFxList.add(transformedESG);
		}

		return transformedESGFxList;

	}

}
