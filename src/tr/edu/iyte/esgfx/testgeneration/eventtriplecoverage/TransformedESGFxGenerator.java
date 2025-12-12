package tr.edu.iyte.esgfx.testgeneration.eventtriplecoverage;

import java.util.LinkedList;
import java.util.List;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esgfx.model.sequenceesgfx.ESGFxtoSequenceESGFxConverter;
import tr.edu.iyte.esgfx.model.sequenceesgfx.SequenceESGFxTransformer;

public class TransformedESGFxGenerator {

    // OPTIMIZATION: Reuse these stateless helpers instead of creating new ones every time
    private final ESGFxtoSequenceESGFxConverter converter;
    private final SequenceESGFxTransformer transformer;

	public TransformedESGFxGenerator() {
        this.converter = new ESGFxtoSequenceESGFxConverter();
        this.transformer = new SequenceESGFxTransformer();
	}
	
	public ESG generateTransformedESGFx(int coverageLength, ESG ESGFx) {
		int numberOfTransformations = coverageLength - 2;
		
		// Convert to Sequence ESG (L=1 base)
        // Using the optimized converter here speeds up the initial step significantly
		ESG oneESGFx = converter.convert(ESGFx);
		ESG transformedESGFx = oneESGFx;
		
		for (int i = 1; i <= numberOfTransformations; i++) {
            // Transform iteratively
            // Note: Ensure 'transformer.transform' cleans up the old 'transformedESGFx' if it creates a new one internally.
            ESG nextTransform = transformer.transform(transformedESGFx, oneESGFx);
            
            // If this is not the first iteration, the previous 'transformedESGFx' is intermediate trash.
            // We can't explicitly null it easily here without knowing if 'transform' modifies in place or returns new.
            // Assuming it returns new:
            transformedESGFx = nextTransform;
		}

		return transformedESGFx;
	}
	
	public List<ESG> generateTransformedESGList(int coverageLength, ESG ESGFx) {
		int numberOfTransformations = coverageLength - 2;
		
		List<ESG> transformedESGFxList = new LinkedList<ESG>();

		ESG oneESGFx = converter.convert(ESGFx);
		transformedESGFxList.add(oneESGFx);

		ESG transformedESG = oneESGFx;
		for (int i = 1; i <= numberOfTransformations; i++) {
			transformedESG = transformer.transform(transformedESG, oneESGFx);
			transformedESGFxList.add(transformedESG);
		}

		return transformedESGFxList;
	}
}