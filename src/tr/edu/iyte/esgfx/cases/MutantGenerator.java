package tr.edu.iyte.esgfx.cases;

import java.util.List;

import java.util.Set;
import java.util.LinkedHashSet;
import java.util.concurrent.*;
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

	protected final int MEASURE_COUNT = 1;
	protected final int WARMUP_COUNT = 0;

	protected Set<String> mutationElementSet = new LinkedHashSet<String>();

	private final EulerCycleToTestSequenceGenerator eulerCycleToTestSequenceGenerator = new EulerCycleToTestSequenceGenerator();
	private final TransformedESGFxGenerator transformedESGFxGenerator = new TransformedESGFxGenerator();
	private final EulerCycleGeneratorForEdgeCoverage eulerCycleGeneratorForEdgeCoverage = new EulerCycleGeneratorForEdgeCoverage();

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	protected FaultDetector generateFaultDetector(ESG productESGFx, int covarage) {
		Set<EventSequence> tests = buildTests(productESGFx, covarage);

		if (covarage == 0 || covarage == 1) {
			return new FaultDetector(tests);
		} else {
			Set<EventSequence> newTests = EventSequenceUtilities.removeRepetitionsFromEventSequenceSet(covarage, tests);
			return new FaultDetector(newTests);
		}
	}

	private Set<EventSequence> buildTests(ESG productESGFx, int L) {
		eulerCycleToTestSequenceGenerator.reset();
		eulerCycleGeneratorForEdgeCoverage.reset();

		if (L == 0) {
			// SAFETY LIMIT: Cap the steps to avoid infinite Random Walk
			// 275^3 * 5 is ~104 Million. We cap it at 500,000 to prevent OOM.
			long calculatedLimit = (long) (5 * Math.pow((productESGFx.getVertexList().size()), 3));
			int safetyLimit = (int) Math.min(calculatedLimit, 500_000);
//			ESGFx clone = new ESGFx((ESGFx) productESGFx);

			RandomWalkTestGenerator rw = new RandomWalkTestGenerator((ESGFx)productESGFx, 0.85);
			Set<EventSequence> tests = rw.generateWalkUntilEdgeCoverage(100, safetyLimit);
//			EventSequenceUtilities.esgEventSequenceSetPrinter(tests, "");
//			clone = null; // Help GC
			return tests;

		} else if (L == 1) {
//			System.out.println("L = 1");
//			System.out.println(ESGFx);
//			ESGToDOTFileConverter.buildDOTFileFromESG(productESGFx, DOTFolderPath + productESGFx.getID() + ".dot");
//			ESGFx clone = new ESGFx((ESGFx) productESGFx);
			ESG ESGFx = StronglyConnectedBalancedESGFxGeneration
					.getStronglyConnectedBalancedESGFxGeneration(productESGFx);
			EulerCycleGeneratorForEventCoverage eventGen = new EulerCycleGeneratorForEventCoverage(
					featureExpressionMapFromFeatureModel);

			eventGen.generateEulerCycle(ESGFx);
//			System.out.println("EulerCycleGeneratorForEventCoverage done");
			List<Vertex> eulerCycle = eventGen.getEulerCycle();
//			System.out.println(eulerCycle);
			Set<EventSequence> tests = eulerCycleToTestSequenceGenerator.CESgenerator(eulerCycle);
//			EventSequenceUtilities.esgEventSequenceSetPrinter(tests, "");
//			clone = null; // Help GC
			return tests;

		} else {
			// L2, L3, L4: Heavy Transformations
//			ESGFx clone = new ESGFx((ESGFx) productESGFx);
			ESG transformed = transformedESGFxGenerator.generateTransformedESGFx(L, productESGFx);
			ESG scb = StronglyConnectedBalancedESGFxGeneration.getStronglyConnectedBalancedESGFxGeneration(transformed);

			eulerCycleGeneratorForEdgeCoverage.generateEulerCycle(scb);
			List<Vertex> cycle = eulerCycleGeneratorForEdgeCoverage.getEulerCycle();
//			System.out.println(cycle);
			Set<EventSequence> tests = eulerCycleToTestSequenceGenerator.CESgenerator(cycle);
//			EventSequenceUtilities.esgEventSequenceSetPrinter(tests, "");
//			clone = null; // Help GC
			return tests;
		}
	}

	public static double percentageOfFaultDetection(int numberOfMutants, int numberOfDetectedMutants) {
		int undetectedMutantNumber = numberOfMutants - numberOfDetectedMutants;
		double percentage = ((double) undetectedMutantNumber) / ((double) numberOfMutants) * 100.0;

		if (undetectedMutantNumber == 0) {
			return 100.0;
		} else {
			return 100.0 - percentage;
		}
	}

	// --- TIMEOUT PROTECTED EXECUTION METHODS ---

	protected void runDetector(FaultDetector detector, ESG mutant) {
		if (WARMUP_COUNT <= 0)
			return; // Skip if warmup is disabled

		Future<?> future = executor.submit(() -> {
			for (int w = 0; w < WARMUP_COUNT; w++)
				detector.isFaultDetected(mutant);
		});

		try {
			// 5 Seconds Timeout
			future.get(5, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			System.err.println("⚠️ TIMEOUT: Warmup skipped (Infinite Loop protection).");
			System.err.println("Mutant ID " + mutant.getID());
			System.out.println(mutant.getRealVertexList());
			System.out.println(mutant.getRealEdgeList());
			EventSequenceUtilities.esgEventSequencePrinter(detector.getCESsOfESG(), detector.getCurrentCES(),
					"L = " + coverageLength);
			future.cancel(true); // Kill the thread
		} catch (Exception e) {
			System.err.println("⚠️ ERROR: Warmup failed: " + e.getMessage());
			System.err.println("Mutant ID " + mutant.getID());
			System.out.println(mutant.getRealVertexList());
			System.out.println(mutant.getRealEdgeList());
			EventSequenceUtilities.esgEventSequencePrinter(detector.getCESsOfESG(), detector.getCurrentCES(),
					"L = " + coverageLength);
		}
	}

	protected long measureTime(FaultDetector detector, ESG mutant) {
		Future<Long> future = executor.submit(() -> {
			long sum = 0;
			for (int i = 0; i < MEASURE_COUNT; i++) {
				long start = System.nanoTime();
				detector.isFaultDetected(mutant);
				sum += (System.nanoTime() - start);
			}
			return sum / MEASURE_COUNT;
		});

		try {
			// 5 Seconds Timeout
			return future.get(5, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			System.err.println("⚠️ TIMEOUT: Measurement skipped (Infinite Loop protection). Returning 0.");
			System.err.println("Mutant ID " + mutant.getID());
			System.out.println(mutant.getRealVertexList());
			System.out.println(mutant.getRealEdgeList());
			EventSequenceUtilities.esgEventSequencePrinter(detector.getCESsOfESG(), detector.getCurrentCES(),
					"L = " + coverageLength);
			future.cancel(true); // Kill the thread
			return 0;
		} catch (Exception e) {
			System.err.println("⚠️ ERROR: Measurement failed: " + e.getMessage());
			System.err.println("Mutant ID " + mutant.getID());
			System.out.println(mutant.getRealVertexList());
			System.out.println(mutant.getRealEdgeList());
			EventSequenceUtilities.esgEventSequencePrinter(detector.getCESsOfESG(), detector.getCurrentCES(),
					"L = " + coverageLength);
			return 0;
		}
	}

	// Important: Must be called at the end of the process to kill the executor
	// thread
	protected void shutdownExecutor() {
		executor.shutdownNow();
	}
}