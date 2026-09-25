package tr.edu.iyte.esgfx.api;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.tools.ModelIterator;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.conversion.mxe.MXEFileToESGFxConverter;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;
import tr.edu.iyte.esgfx.productconfigurationgeneration.ProductConfigurationValidator;
import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EdgeCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EulerCycleGeneratorForEdgeCoverage;
import tr.edu.iyte.esgfx.testgeneration.eventcoverage.EulerCycleGeneratorForEventCoverage;
import tr.edu.iyte.esgfx.testgeneration.eventcoverage.EventCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.eventtriplecoverage.TransformedESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;

public final class SingleProductTestGenerationAPI {

	private SingleProductTestGenerationAPI() {
	}

	public static LoadedSplModel load(String featureModelFilePath, String esgFxFilePath) throws Exception {
		MXEFileToESGFxConverter converter = new MXEFileToESGFxConverter();
		FeatureModel featureModel = converter.parseFeatureModel(featureModelFilePath);
		ESG esgFx = converter.parseMXEFileForESGFxCreation(esgFxFilePath);
		Map<String, FeatureExpression> featureExpressionMap = converter.getFeatureExpressionMap();
		return new LoadedSplModel(featureModel, esgFx, featureExpressionMap);
	}

	public static ValidationResult validate(LoadedSplModel model, Map<String, Boolean> selection) {
		synchronized (model) {
			applySelection(model.getFeatureExpressionMap(), selection);

			ProductConfigurationValidator validator = new ProductConfigurationValidator();
			boolean isValid = validator.validate(model.getFeatureModel(), model.getFeatureExpressionMap())
					&& ConstraintEvaluator.allSatisfied(model.getFeatureModel(), model.getFeatureExpressionMap());

			if (!isValid) {
				return new ValidationResult(false,
						Collections.singletonList("Configuration does not satisfy the feature model."));
			}
			return new ValidationResult(true, Collections.emptyList());
		}
	}

	public static SingleProductTestResult generate(LoadedSplModel model, int productId,
			Map<String, Boolean> selection, int coverageLength) throws Exception {

		synchronized (model) {
			Map<String, FeatureExpression> featureExpressionMap = model.getFeatureExpressionMap();
			applySelection(featureExpressionMap, selection);

			ProductConfigurationValidator validator = new ProductConfigurationValidator();
			boolean isValid = validator.validate(model.getFeatureModel(), featureExpressionMap);
			if (!isValid) {
				throw new InvalidConfigurationException(
						Collections.singletonList("Configuration does not satisfy the feature model."));
			}

			String productName = String.format(Locale.ROOT, "P%04d", productId);

			long startNanos = System.nanoTime();

			ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();
			ESG productESGFx = productESGFxGenerator.generateProductESGFx(productId, productName, model.getEsgFx());

			Set<EventSequence> testSequences;
			double coveragePercentage;
			String coverageType;

			if (coverageLength == 1) {
				EulerCycleGeneratorForEventCoverage eulerCycleGeneratorForEventCoverage = new EulerCycleGeneratorForEventCoverage(
						featureExpressionMap);
				EulerCycleToTestSequenceGenerator eulerCycleToTestSequenceGenerator = new EulerCycleToTestSequenceGenerator();

				ESG stronglyConnectedBalancedESGFx = StronglyConnectedBalancedESGFxGeneration
						.getStronglyConnectedBalancedESGFxGeneration(productESGFx);

				eulerCycleGeneratorForEventCoverage.generateEulerCycle(stronglyConnectedBalancedESGFx);
				List<Vertex> eulerCycle = eulerCycleGeneratorForEventCoverage.getEulerCycle();

				testSequences = eulerCycleToTestSequenceGenerator.CESgenerator(eulerCycle);

				EventCoverageAnalyser eventCoverageAnalyser = new EventCoverageAnalyser();
				coveragePercentage = eventCoverageAnalyser.analyseEventCoverage(productESGFx, testSequences,
						featureExpressionMap);

				coverageType = "event";
			} else {
				TransformedESGFxGenerator transformedESGFxGenerator = new TransformedESGFxGenerator();
				EulerCycleGeneratorForEdgeCoverage eulerCycleGeneratorForEdgeCoverage = new EulerCycleGeneratorForEdgeCoverage();
				EulerCycleToTestSequenceGenerator eulerCycleToTestSequenceGenerator = new EulerCycleToTestSequenceGenerator();

				ESG transformedProductESGFx = transformedESGFxGenerator.generateTransformedESGFx(coverageLength,
						productESGFx);
				ESG stronglyConnectedBalancedESGFx = StronglyConnectedBalancedESGFxGeneration
						.getStronglyConnectedBalancedESGFxGeneration(transformedProductESGFx);

				eulerCycleGeneratorForEdgeCoverage.generateEulerCycle(stronglyConnectedBalancedESGFx);
				List<Vertex> eulerCycle = eulerCycleGeneratorForEdgeCoverage.getEulerCycle();

				testSequences = eulerCycleToTestSequenceGenerator.CESgenerator(eulerCycle);

				EdgeCoverageAnalyser edgeCoverageAnalyser = new EdgeCoverageAnalyser();
				coveragePercentage = edgeCoverageAnalyser.analyseEdgeCoverage(productESGFx, testSequences,
						featureExpressionMap);

				coverageType = "edge";
			}

			long endNanos = System.nanoTime();
			long generationTimeMs = (endNanos - startNanos) / 1_000_000L;

			List<EventSequence> orderedTestSequences = new ArrayList<EventSequence>(testSequences);
			int totalEventCount = 0;
			for (EventSequence es : orderedTestSequences) {
				totalEventCount += es.length();
			}

			return new SingleProductTestResult(productId, selection, coverageLength, coverageType, coveragePercentage,
					orderedTestSequences, orderedTestSequences.size(), totalEventCount, generationTimeMs);
		}
	}

	public static SingleProductTestResult generate(LoadedSplModel model, int productId, String configFilePath,
			int coverageLength) throws Exception {
		Map<String, Boolean> selection = readConfigFile(configFilePath);
		return generate(model, productId, selection, coverageLength);
	}

	public static long countValidConfigurations(LoadedSplModel model) throws Exception {
		return countValidConfigurations(model, Long.MAX_VALUE);
	}

	/**
	 * Counts valid configurations but stops once {@code cap} of them have been
	 * seen. Counting walks the configuration space one solution at a time, which
	 * takes minutes on a line with hundreds of thousands of products; a caller
	 * that only needs the count for display can bound it and show "cap+" instead
	 * of waiting. The uncapped overload is kept for the sampler, which needs the
	 * exact size of the space it draws from.
	 */
	public static long countValidConfigurations(LoadedSplModel model, long cap) throws Exception {
		synchronized (model) {
			Map<String, FeatureExpression> featureExpressionMap = model.getFeatureExpressionMap();
			FeatureModel featureModel = model.getFeatureModel();

			List<FeatureExpression> featureExpressionList = buildFeatureExpressionList(featureExpressionMap);

			SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();
			ISolver solver = new ModelIterator(SolverFactory.newDefault());
			satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel, featureExpressionMap,
					featureExpressionList);

			ProductConfigurationValidator validator = new ProductConfigurationValidator();

			long validCount = 0;
			while (solver.isSatisfiable()) {
				int[] modelArray = solver.model();

				for (int i = 0; i < modelArray.length; i++) {
					FeatureExpression fe = featureExpressionList.get(i);
					fe.setTruthValue(modelArray[i] > 0);
				}

				boolean exhausted = blockCurrentModel(solver, modelArray);

				if (validator.validate(featureModel, featureExpressionMap)) {
					validCount++;
					if (validCount >= cap) {
						break;
					}
				}
				if (exhausted) {
					break;
				}
			}
			return validCount;
		}
	}

	/**
	 * Excludes the model just returned so the search moves on. The solver
	 * refuses the clause once no other model can satisfy the constraints, which
	 * is how a search over a model with a single valid configuration ends;
	 * that refusal is the termination signal, not a failure.
	 *
	 * @return true when the model space is exhausted
	 */
	static boolean blockCurrentModel(ISolver solver, int[] modelArray) {
		VecInt blockingClause = new VecInt();
		for (int i = 0; i < modelArray.length; i++) {
			blockingClause.push(-modelArray[i]);
		}
		try {
			solver.addClause(blockingClause);
			return false;
		} catch (ContradictionException e) {
			return true;
		}
	}

	private static void applySelection(Map<String, FeatureExpression> featureExpressionMap,
			Map<String, Boolean> selection) {
		for (Map.Entry<String, Boolean> entry : selection.entrySet()) {
			String featureName = entry.getKey();
			if (featureName.contains("!")) {
				continue;
			}
			FeatureExpression fe = featureExpressionMap.get(featureName);
			if (fe != null) {
				fe.setTruthValue(entry.getValue());
			}
		}
	}

	private static Map<String, Boolean> readConfigFile(String configFilePath) throws Exception {
		Map<String, Boolean> selection = new LinkedHashMap<String, Boolean>();
		try (BufferedReader br = new BufferedReader(new FileReader(configFilePath))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().isEmpty()) {
					continue;
				}
				String[] parts = line.split("=");
				if (parts.length == 2) {
					selection.put(parts[0].trim(), Boolean.parseBoolean(parts[1].trim()));
				}
			}
		}
		return selection;
	}

	static List<FeatureExpression> buildFeatureExpressionList(
			Map<String, FeatureExpression> featureExpressionMap) {
		List<FeatureExpression> featureExpressionList = new ArrayList<FeatureExpression>(
				featureExpressionMap.size() + 1);
		for (Map.Entry<String, FeatureExpression> entry : featureExpressionMap.entrySet()) {
			String featureName = entry.getKey();
			FeatureExpression featureExpression = entry.getValue();
			if (!featureName.contains("!") && featureExpression != null
					&& featureExpression.getFeature().getName() != null) {
				featureExpressionList.add(featureExpression);
			}
		}
		return featureExpressionList;
	}
}
