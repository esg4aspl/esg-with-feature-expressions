package tr.edu.iyte.esgfx.cases;

import java.io.File;
import java.util.Arrays;
import java.util.Set;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esgfx.conversion.dot.DOTFileToESGFxConverter;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.testgeneration.randomwalktesting.RandomWalkTestGenerator;
import tr.edu.iyte.esgfx.mutationtesting.resultutils.FaultDetectionResultRecorder;
import tr.edu.iyte.esgfx.testgeneration.FileToTestSuiteConverter;
import tr.edu.iyte.esgfx.testgeneration.SequenceCoverageAnalyser;

/**
 * RQ3, sequence omission: a mutant forbids one walk of k events of the
 * product-level model. With k = 1 this is event omission and with k = 2 edge
 * omission, as in the two drivers of those operators, whose rows it
 * repeats as a check; with k = 3 and k = 4 it is the edge-omission operator
 * applied to the transformed graphs. One row is written per product,
 * approach and k, with the columns of those drivers.
 *
 * Four groups of suites, each in its own file: the deterministic approaches;
 * the random baseline that runs until full event-couple coverage, per seed;
 * the same baseline at each damping factor, per seed; and the random
 * baseline with the event budget of the suite of the approach at each
 * level, per seed.
 *
 * Arguments: the package of the case study and its short name, for example
 * "eMail eM". Environment: N_SHARDS, SHARD, RW_SEEDS.
 */
public class RQ3_FaultDetection_SequenceOmitter extends CaseStudyUtilities {

	private static final double RW_DAMPING = 0.85;
	private static final long RW_DETERMINISTIC_SEED = 42L;
	private static final int[] LENGTHS = { 1, 2, 3, 4 };
	private static final double[] DAMPING_FACTORS = { 0.80, 0.85, 0.90, 0.95, 1.00 };

	public static void main(String[] args) throws Exception {
		Class.forName("tr.edu.iyte.esgfx.cases." + args[0] + ".CaseStudyUtilities_" + args[1])
				.getMethod("initializeFilePaths").invoke(null);
		new RQ3_FaultDetection_SequenceOmitter().evaluateFaultDetection();
	}

	public void evaluateFaultDetection() throws Exception {
		System.out.println("RQ3 FAULT DETECTION (SEQUENCE OMISSION) - SPL: " + SPLName + " STARTED");
		int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
		int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));

		featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFile,
				ESGFxFile);

		File dotDir = new File(DOTFolder + "L2/");
		File[] dotFiles = dotDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".dot"));
		if (dotFiles == null || dotFiles.length == 0) {
			throw new RuntimeException("No product models in " + dotDir);
		}
		Arrays.sort(dotFiles, (f1, f2) -> f1.getName().compareTo(f2.getName()));

		String[] deterministicApproaches = { "ESG-Fx_L0", "ESG-Fx_L1", "ESG-Fx_L2", "ESG-Fx_L3", "ESG-Fx_L4", "EFG_L2",
				"EFG_L3", "EFG_L4" };
		long[] seeds = TestSuiteFactory.randomWalkSeeds();

		String perProductLogDir = faultDetectionFolder + "perProduct/";
		new File(perProductLogDir).mkdirs();
		String deterministicLogPath = perProductLogDir + SPLName + "_SequenceOmission_shard"
				+ String.format("%02d", CURRENT_SHARD) + ".csv";
		String multiSeedLogPath = perProductLogDir + SPLName + "_SequenceOmission_MultiSeedRW_shard"
				+ String.format("%02d", CURRENT_SHARD) + ".csv";
		String dampingLogPath = perProductLogDir + SPLName + "_SequenceOmission_DampingRW_shard"
				+ String.format("%02d", CURRENT_SHARD) + ".csv";
		String budgetLogPath = perProductLogDir + SPLName + "_SequenceOmission_BudgetRW_shard"
				+ String.format("%02d", CURRENT_SHARD) + ".csv";

		int productID = 0;
		int handledProducts = 0;
		for (File dotFile : dotFiles) {
			productID++;
			if (((productID - 1) % N_SHARDS) != CURRENT_SHARD) {
				continue;
			}
			handledProducts++;
			String productName = dotFile.getName().replaceAll("(?i)\\.dot", "");
			updateFeatureExpressionMapFromConfigFile(productConfigurationFolder + productName + ".config");
			ESG productESGFx = DOTFileToESGFxConverter.parseDOTFileForESGFxCreation(dotFile.getAbsolutePath(),
					featureExpressionMapFromFeatureModel);
			SequenceCoverageAnalyser analyser = new SequenceCoverageAnalyser(productESGFx);
			int[] eventBudgetOfLevel = new int[5];

			for (String approach : deterministicApproaches) {
				Set<EventSequence> suite;
				if ("ESG-Fx_L0".equals(approach)) {
					suite = TestSuiteFactory.generateRandomWalkSuite(productESGFx, RW_DAMPING, RW_DETERMINISTIC_SEED);
				} else if (TestSuiteFactory.approachToLevel(approach) >= 1) {
					suite = TestSuiteFactory.generateESGFxSuite(productESGFx,
							TestSuiteFactory.approachToLevel(approach), featureExpressionMapFromFeatureModel);
				} else {
					suite = FileToTestSuiteConverter.loadTestSequencesFromFile(productName, approach, productESGFx);
				}
				if (suite == null || suite.isEmpty()) {
					continue;
				}
				Set<EventSequence> unfolded = SequenceCoverageAnalyser.unfold(suite);
				int level = TestSuiteFactory.approachToLevel(approach);
				if (level >= 1) {
					eventBudgetOfLevel[level] = SequenceCoverageAnalyser.numberOfEvents(unfolded);
				}
				write(deterministicLogPath, productName, approach, analyser, unfolded);
			}

			for (long seed : seeds) {
				Set<EventSequence> suite = TestSuiteFactory.generateRandomWalkSuite(productESGFx, RW_DAMPING, seed);
				if (suite == null || suite.isEmpty()) {
					continue;
				}
				write(multiSeedLogPath, productName, "ESG-Fx_L0_seed" + seed, analyser, suite);
			}

			int safetyLimit = TestSuiteFactory.defaultRwSafetyLimit(productESGFx);
			for (double damping : DAMPING_FACTORS) {
				for (long seed : seeds) {
					Set<EventSequence> suite = new RandomWalkTestGenerator((ESGFx) productESGFx, damping, seed)
							.generateWalkUntilEdgeCoverage(100.0, safetyLimit);
					write(dampingLogPath, productName,
							String.format(java.util.Locale.ROOT, "RW_d%.2f_seed%d", damping, seed), analyser, suite);
				}
			}

			for (int level = 1; level <= 4; level++) {
				if (eventBudgetOfLevel[level] <= 0) {
					System.err.println("No suite of the approach at level " + level + " for " + productName
							+ "; no equal-budget rows are written for it.");
					continue;
				}
				for (long seed : seeds) {
					Set<EventSequence> suite = new RandomWalkTestGenerator((ESGFx) productESGFx, RW_DAMPING, seed)
							.generateWalkUntilEvents(eventBudgetOfLevel[level], safetyLimit);
					write(budgetLogPath, productName, "RW_budgetL" + level + "_seed" + seed, analyser, suite);
				}
			}

			if (handledProducts % 50 == 0) {
				System.out.println("Handled products: " + handledProducts);
			}
		}
		System.out.println("RQ3 FAULT DETECTION (SEQUENCE OMISSION) - SPL: " + SPLName + " FINISHED, products: "
				+ handledProducts);
	}

	private void write(String logPath, String productName, String approach, SequenceCoverageAnalyser analyser,
			Set<EventSequence> unfoldedSuite) {
		for (int length : LENGTHS) {
			SequenceCoverageAnalyser.Omission omission = analyser.sequenceOmission(unfoldedSuite, length);
			int mutants = (int) Math.min(Integer.MAX_VALUE, omission.mutants);
			int killed = omission.killed();
			double score = mutants > 0 ? 100.0 * killed / mutants : 0.0;
			// Medians over the killed mutants; the penalized median counts an unkilled mutant as 100%.
			double medianEvents = median(omission.eventsToKill, 0, 1.0);
			double scale = omission.eventsInSuite > 0 ? 100.0 / omission.eventsInSuite : 0.0;
			double medianPercentage = median(omission.eventsToKill, 0, scale);
			double penalizedMedianPercentage = median(omission.eventsToKill, mutants - killed, scale);
			FaultDetectionResultRecorder.writeRQ3PerProductLog(logPath, SPLName, productName,
					"SequenceOmission" + length, approach, mutants, killed, score, omission.eventsInSuite,
					medianEvents, medianPercentage, penalizedMedianPercentage, killed, 0, 0, "", mutants);
		}
	}

	/**
	 * Median of the sorted values, each multiplied by 'scale', followed by
	 * 'unkilled' values of 100.
	 */
	private static double median(int[] sortedEvents, int unkilled, double scale) {
		int n = sortedEvents.length + unkilled;
		if (n == 0) {
			return 0.0;
		}
		double lower = value(sortedEvents, (n - 1) / 2, scale);
		double upper = value(sortedEvents, n / 2, scale);
		return (lower + upper) / 2.0;
	}

	private static double value(int[] sortedEvents, int index, double scale) {
		return index < sortedEvents.length ? sortedEvents[index] * scale : 100.0;
	}
}
