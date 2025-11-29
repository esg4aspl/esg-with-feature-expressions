package tr.edu.iyte.esgfx.cases;

import java.util.List;

import java.util.Set;
import java.util.LinkedHashSet;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.sequenceesgfx.EventSequenceUtilities;
import tr.edu.iyte.esgfx.mutationtesting.faultdetection.FaultDetector;

import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EulerCycleGeneratorForEdgeCoverage;
import tr.edu.iyte.esgfx.testgeneration.eventcoverage.EulerCycleGeneratorForEventCoverage;
import tr.edu.iyte.esgfx.testgeneration.eventtriplecoverage.TransformedESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.randomwalktesting.RandomWalkTestGenerator;
import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;

public class MutantGenerator extends CaseStudyUtilities {

	protected long totalExecTimeNanosL0 = 0;
	protected long totalExecTimeNanosL1 = 0;
	protected long totalExecTimeNanosL2 = 0;
	protected long totalExecTimeNanosL3 = 0;
	protected long totalExecTimeNanosL4 = 0;
	
	
	protected final int MEASURE_COUNT = 2;

	protected final int WARMUP_COUNT = 1;

	protected Set<String> mutationElementSet = new LinkedHashSet<String>();

	protected FaultDetector generateFaultDetector(ESG productESGFx, int covarage) {

		Set<EventSequence> tests = buildTests(productESGFx, covarage);

		if (covarage == 0 || covarage == 1) {
			FaultDetector detector = new FaultDetector(tests);
			return detector;
		} else {
			Set<EventSequence> newTests = EventSequenceUtilities.removeRepetitionsFromEventSequenceSet(covarage, tests);
			FaultDetector detector = new FaultDetector(newTests);
			return detector;
		}

	}

	private Set<EventSequence> buildTests(ESG productESGFx, int L) {

		if (L == 0) {
			int safetyLimit = (int) (5 * Math.pow((productESGFx.getVertexList().size()),3));
			
			RandomWalkTestGenerator rw = new RandomWalkTestGenerator((ESGFx) productESGFx, 0.85);
			Set<EventSequence> tests = rw.generateWalkUntilEdgeCoverage(100, safetyLimit);
			return tests;
		} else if (L == 1) {
			ESG ESGFx = StronglyConnectedBalancedESGFxGeneration
					.getStronglyConnectedBalancedESGFxGeneration(productESGFx);
//			System.out.println(ESGFx);

			EulerCycleGeneratorForEventCoverage eulerCycleGeneratorForEventCoverage = new EulerCycleGeneratorForEventCoverage(
					featureExpressionMapFromFeatureModel);
			eulerCycleGeneratorForEventCoverage.generateEulerCycle(ESGFx);
			List<Vertex> eulerCycle = eulerCycleGeneratorForEventCoverage.getEulerCycle();

			EulerCycleToTestSequenceGenerator eulerCycleToTestSequenceGenerator = new EulerCycleToTestSequenceGenerator();
			return eulerCycleToTestSequenceGenerator.CESgenerator(eulerCycle);
		} else {
			TransformedESGFxGenerator t = new TransformedESGFxGenerator();
			ESG transformed = t.generateTransformedESGFx(L, productESGFx);
			ESG scb = StronglyConnectedBalancedESGFxGeneration.getStronglyConnectedBalancedESGFxGeneration(transformed);
			EulerCycleGeneratorForEdgeCoverage ec = new EulerCycleGeneratorForEdgeCoverage();
			ec.generateEulerCycle(scb);
			List<Vertex> cycle = ec.getEulerCycle();
			EulerCycleToTestSequenceGenerator ts = new EulerCycleToTestSequenceGenerator();
			return ts.CESgenerator(cycle);
		}
	}

	public static double percentageOfFaultDetection(int numberOfMutants, int numberOfDetectedMutants) {

		int undetectedMutantNumber = numberOfMutants - numberOfDetectedMutants;

		double percentage = ((double) undetectedMutantNumber) / ((double) numberOfMutants) * 100.0;

		if (undetectedMutantNumber == 0) {
			return 100.0;
		} else {
			// System.out.printf("Coverage %.2f %s\n", 100.0 - coverage, "%");
			return 100.0 - percentage;
		}

	}
	
    // --- Helper Methods ---
    protected void runDetector(FaultDetector detector, ESG mutant) {
        for(int w=0; w<WARMUP_COUNT; w++) detector.isFaultDetected(mutant);
    }
    
    protected long measureTime(FaultDetector detector, ESG mutant) {
        long sum = 0;
        for (int i = 0; i < MEASURE_COUNT; i++) {
            long start = System.nanoTime();
            detector.isFaultDetected(mutant);
            sum += (System.nanoTime() - start);
        }
        return sum / MEASURE_COUNT;
    }

}
