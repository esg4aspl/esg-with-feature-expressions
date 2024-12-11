package tr.edu.iyte.esgfx.cases.edgecoverage.Tesla;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

import java.util.Map.Entry;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.conversion.mxe.MXEFileToESGFxConverter;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featuremodel.Feature;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;
import tr.edu.iyte.esgfx.productconfigurationgeneration.AutomaticProductConfigurationGenerator;
import tr.edu.iyte.esgfx.productconfigurationgeneration.ProductConfigurationValidator;
import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EdgeCoverageAnalyser;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EulerCycleGeneratorForEdgeCoverage;
import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;

public class AutomaticProductConfigurationGeneratorApp extends CaseStudyUtilities_Tesla {

	private static Set<String> M3_12 = new LinkedHashSet<String>();
	private static Set<String> M3_13 = new LinkedHashSet<String>();
	private static Set<String> M3_14 = new LinkedHashSet<String>();
	private static Set<String> M3_15 = new LinkedHashSet<String>();
	private static Set<String> M3_16 = new LinkedHashSet<String>();
	private static Set<String> M3_17 = new LinkedHashSet<String>();
	private static Set<String> M3_18 = new LinkedHashSet<String>();
	private static Set<String> M3_19 = new LinkedHashSet<String>();
	private static Set<String> M3_20 = new LinkedHashSet<String>();
	private static Set<String> M3_21 = new LinkedHashSet<String>();
	private static Set<String> M3_22 = new LinkedHashSet<String>();
	private static Set<String> M3_23 = new LinkedHashSet<String>();

	private static Set<String> MY_12 = new LinkedHashSet<String>();
	private static Set<String> MY_13 = new LinkedHashSet<String>();
	private static Set<String> MY_14 = new LinkedHashSet<String>();
	private static Set<String> MY_15 = new LinkedHashSet<String>();
	private static Set<String> MY_16 = new LinkedHashSet<String>();
	private static Set<String> MY_17 = new LinkedHashSet<String>();
	private static Set<String> MY_18 = new LinkedHashSet<String>();
	private static Set<String> MY_19 = new LinkedHashSet<String>();
	private static Set<String> MY_20 = new LinkedHashSet<String>();
	private static Set<String> MY_21 = new LinkedHashSet<String>();
	private static Set<String> MY_22 = new LinkedHashSet<String>();
	private static Set<String> MY_23 = new LinkedHashSet<String>();

	private static Set<String> MX_12 = new LinkedHashSet<String>();
	private static Set<String> MX_13 = new LinkedHashSet<String>();
	private static Set<String> MX_14 = new LinkedHashSet<String>();
	private static Set<String> MX_15 = new LinkedHashSet<String>();
	private static Set<String> MX_16 = new LinkedHashSet<String>();
	private static Set<String> MX_17 = new LinkedHashSet<String>();
	private static Set<String> MX_18 = new LinkedHashSet<String>();
	private static Set<String> MX_19 = new LinkedHashSet<String>();
	private static Set<String> MX_20 = new LinkedHashSet<String>();
	private static Set<String> MX_21 = new LinkedHashSet<String>();
	private static Set<String> MX_22 = new LinkedHashSet<String>();
	private static Set<String> MX_23 = new LinkedHashSet<String>();

	private static Set<String> MS_12 = new LinkedHashSet<String>();
	private static Set<String> MS_13 = new LinkedHashSet<String>();
	private static Set<String> MS_14 = new LinkedHashSet<String>();
	private static Set<String> MS_15 = new LinkedHashSet<String>();
	private static Set<String> MS_16 = new LinkedHashSet<String>();
	private static Set<String> MS_17 = new LinkedHashSet<String>();
	private static Set<String> MS_18 = new LinkedHashSet<String>();
	private static Set<String> MS_19 = new LinkedHashSet<String>();
	private static Set<String> MS_20 = new LinkedHashSet<String>();
	private static Set<String> MS_21 = new LinkedHashSet<String>();
	private static Set<String> MS_22 = new LinkedHashSet<String>();
	private static Set<String> MS_23 = new LinkedHashSet<String>();

	public static void main(String[] args) throws ContradictionException, TimeoutException {

		CaseStudyUtilities_Tesla.initializeFilePaths();
		MXEFileToESGFxConverter MXEFileToESGFxConverter = new MXEFileToESGFxConverter();
		FeatureModel featureModel = null;
		try {
			featureModel = MXEFileToESGFxConverter.parseFeatureModel(featureModelFilePath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ESG ESGFx = null;
		try {
			ESGFx = MXEFileToESGFxConverter.parseMXEFileForESGFxCreation(ESGFxFilePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Map<String, FeatureExpression> featureExpressionMapFromFeatureModel = MXEFileToESGFxConverter
				.getFeatureExpressionMap();

		AutomaticProductConfigurationGenerator automaticProductConfigurationGenerator = new AutomaticProductConfigurationGenerator();
		List<FeatureExpression> featureExpressionList = automaticProductConfigurationGenerator
				.getFeatureExpressionList(featureExpressionMapFromFeatureModel);
		SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();
		ISolver solver = new ModelIterator(SolverFactory.newDefault());
		satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel,
				featureExpressionList);

		int productID = 0;
		while (solver.isSatisfiable()) {
			
			productID++;
			String productName = "P";
			if (productID < 10)
				productName = "P0";

			String ESGFxName = productName + Integer.toString(productID);

			String productConfiguration = ESGFxName + ": <";
			int numberOfFeatures = 0;
			int[] model = solver.model();
			for (int i = 0; i < model.length; i++) {
				FeatureExpression featureExpression = featureExpressionList.get(i);
				String featureName = featureExpression.getFeature().getName();
				if (model[i] > 0) {
//					System.out.println(featureName + " = true");
					featureExpression.setTruthValue(true);
					productConfiguration += featureName + ", ";
					numberOfFeatures++;
				} else {
//					System.out.println(featureName + " = false");
					featureExpression.setTruthValue(false);
				}
			}
			productConfiguration = productConfiguration.substring(0, productConfiguration.length() - 2);
			productConfiguration += ">:" + numberOfFeatures  + " features";
			categorizeProductConfiguration(productConfiguration, numberOfFeatures);
//			System.out.println(productConfiguration);
//			System.out.println("-----------------------------------");

			// Add a clause to block the current model to find the next one
			VecInt blockingClause = new VecInt();
			for (int i = 0; i < model.length; i++) {
				blockingClause.push(-model[i]);
			}
			solver.addClause(blockingClause);

		}

		System.out.println("Number of Products: " + productID);

		System.out.println("Total number of products: " + numberOfTotatlProducts());
		System.out.println("Number of M3 products: " + numberOfM3Products());
		System.out.println("Number of MY products: " + numberOfMYProducts());
		System.out.println("Number of MX products: " + numberOfMXProducts());
		System.out.println("Number of MS products: " + numberOfMSProducts());

		printM3ProductConfigurations();
		printMYProductConfigurations();
		printMXProductConfigurations();
		printMSProductConfigurations();

	}

	private static void printM3ProductConfigurations() {
		System.out.println("M3 Products:");
		System.out.println("12 Features");
		for (String productConfiguration : M3_12) {
			System.out.println(productConfiguration);
		}
		System.out.println("13 Features");
		for (String productConfiguration : M3_13) {
			System.out.println(productConfiguration);
		}
		System.out.println("14 Features");
		for (String productConfiguration : M3_14) {
			System.out.println(productConfiguration);
		}
		System.out.println("15 Features");
		for (String productConfiguration : M3_15) {
			System.out.println(productConfiguration);
		}
		System.out.println("16 Features");
		for (String productConfiguration : M3_16) {
			System.out.println(productConfiguration);
		}
		System.out.println("17 Features");
		for (String productConfiguration : M3_17) {
			System.out.println(productConfiguration);
		}
		System.out.println("18 Features");
		for (String productConfiguration : M3_18) {
			System.out.println(productConfiguration);
		}
		System.out.println("19 Features");
		for (String productConfiguration : M3_19) {
			System.out.println(productConfiguration);
		}
		System.out.println("20 Features");
		for (String productConfiguration : M3_20) {
			System.out.println(productConfiguration);
		}
		System.out.println("21 Features");
		for (String productConfiguration : M3_21) {
			System.out.println(productConfiguration);
		}
		System.out.println("22 Features");
		for (String productConfiguration : M3_22) {
			System.out.println(productConfiguration);
		}
		System.out.println("23 Features");
		for (String productConfiguration : M3_23) {
			System.out.println(productConfiguration);
		}
	}

	private static void printMYProductConfigurations() {
		System.out.println("MY Products:");
		System.out.println("12 Features");
		for (String productConfiguration : MY_12) {
			System.out.println(productConfiguration);
		}
		System.out.println("13 Features");
		for (String productConfiguration : MY_13) {
			System.out.println(productConfiguration);
		}
		System.out.println("14 Features");
		for (String productConfiguration : MY_14) {
			System.out.println(productConfiguration);
		}
		System.out.println("15 Features");
		for (String productConfiguration : MY_15) {
			System.out.println(productConfiguration);
		}
		System.out.println("16 Features");
		for (String productConfiguration : MY_16) {
			System.out.println(productConfiguration);
		}
		System.out.println("17 Features");
		for (String productConfiguration : MY_17) {
			System.out.println(productConfiguration);
		}
		System.out.println("18 Features");
		for (String productConfiguration : MY_18) {
			System.out.println(productConfiguration);
		}
		System.out.println("19 Features");
		for (String productConfiguration : MY_19) {
			System.out.println(productConfiguration);
		}
		System.out.println("20 Features");
		for (String productConfiguration : MY_20) {
			System.out.println(productConfiguration);
		}
		System.out.println("21 Features");
		for (String productConfiguration : MY_21) {
			System.out.println(productConfiguration);
		}
		System.out.println("22 Features");
		for (String productConfiguration : MY_22) {
			System.out.println(productConfiguration);
		}
		System.out.println("23 Features");
		for (String productConfiguration : MY_23) {
			System.out.println(productConfiguration);
		}

	}

	private static void printMXProductConfigurations() {
		System.out.println("MX Products:");
		System.out.println("12 Features");
		for (String productConfiguration : MX_12) {
			System.out.println(productConfiguration);
		}
		System.out.println("13 Features");
		for (String productConfiguration : MX_13) {
			System.out.println(productConfiguration);
		}
		System.out.println("14 Features");
		for (String productConfiguration : MX_14) {
			System.out.println(productConfiguration);
		}
		System.out.println("15 Features");
		for (String productConfiguration : MX_15) {
			System.out.println(productConfiguration);
		}
		System.out.println("16 Features");
		for (String productConfiguration : MX_16) {
			System.out.println(productConfiguration);
		}
		System.out.println("17 Features");
		for (String productConfiguration : MX_17) {
			System.out.println(productConfiguration);
		}
		System.out.println("18 Features");
		for (String productConfiguration : MX_18) {
			System.out.println(productConfiguration);
		}
		System.out.println("19 Features");
		for (String productConfiguration : MX_19) {
			System.out.println(productConfiguration);
		}
		System.out.println("20 Features");
		for (String productConfiguration : MX_20) {
			System.out.println(productConfiguration);
		}
		System.out.println("21 Features");
		for (String productConfiguration : MX_21) {
			System.out.println(productConfiguration);
		}
		System.out.println("22 Features");
		for (String productConfiguration : MX_22) {
			System.out.println(productConfiguration);
		}
		System.out.println("23 Features");
		for (String productConfiguration : MX_23) {
			System.out.println(productConfiguration);
		}
	}

	private static void printMSProductConfigurations() {
		System.out.println("MS Products:");
		System.out.println("12 Features");
		for (String productConfiguration : MS_12) {
			System.out.println(productConfiguration);
		}
		System.out.println("13 Features");
		for (String productConfiguration : MS_13) {
			System.out.println(productConfiguration);
		}
		System.out.println("14 Features");
		for (String productConfiguration : MS_14) {
			System.out.println(productConfiguration);
		}
		System.out.println("15 Features");
		for (String productConfiguration : MS_15) {
			System.out.println(productConfiguration);
		}
		System.out.println("16 Features");
		for (String productConfiguration : MS_16) {
			System.out.println(productConfiguration);
		}
		System.out.println("17 Features");
		for (String productConfiguration : MS_17) {
			System.out.println(productConfiguration);
		}
		System.out.println("18 Features");
		for (String productConfiguration : MS_18) {
			System.out.println(productConfiguration);
		}
		System.out.println("19 Features");
		for (String productConfiguration : MS_19) {
			System.out.println(productConfiguration);
		}
		System.out.println("20 Features");
		for (String productConfiguration : MS_20) {
			System.out.println(productConfiguration);
		}
		System.out.println("21 Features");
		for (String productConfiguration : MS_21) {
			System.out.println(productConfiguration);
		}
		System.out.println("22 Features");
		for (String productConfiguration : MS_22) {
			System.out.println(productConfiguration);
		}
		System.out.println("23 Features");
		for (String productConfiguration : MS_23) {
			System.out.println(productConfiguration);
		}
	}

	private static int numberOfMXProducts() {
		return MX_12.size() + MX_13.size() + MX_14.size() + MX_15.size() + MX_16.size() + MX_17.size() + MX_18.size()
				+ MX_19.size() + MX_20.size() + MX_21.size() + MX_22.size() + MX_23.size();
	}

	private static int numberOfMYProducts() {
		return MY_12.size() + MY_13.size() + MY_14.size() + MY_15.size() + MY_16.size() + MY_17.size() + MY_18.size()
				+ MY_19.size() + MY_20.size() + MY_21.size() + MY_22.size() + MY_23.size();
	}

	private static int numberOfMSProducts() {
		return MS_12.size() + MS_13.size() + MS_14.size() + MS_15.size() + MS_16.size() + MS_17.size() + MS_18.size()
				+ MS_19.size() + MS_20.size() + MS_21.size() + MS_22.size() + MS_23.size();
	}

	private static int numberOfM3Products() {
		return M3_12.size() + M3_13.size() + M3_14.size() + M3_15.size() + M3_16.size() + M3_17.size() + M3_18.size()
				+ M3_19.size() + M3_20.size() + M3_21.size() + M3_22.size() + M3_23.size();
	}

	private static int numberOfTotatlProducts() {
		return numberOfMXProducts() + numberOfMYProducts() + numberOfMSProducts() + numberOfM3Products();
	}

	private static void categorizeProductConfiguration(String productConfiguration, int numberOfFeatures) {

		if (productConfiguration.contains(", M3,")) {
			categorizeProductConfiguration(productConfiguration, numberOfFeatures, M3_12, M3_13, M3_14, M3_15, M3_16,
					M3_17, M3_18, M3_19, M3_20, M3_21, M3_22, M3_23);
		} else if (productConfiguration.contains(", MY,")) {
			categorizeProductConfiguration(productConfiguration, numberOfFeatures, MY_12, MY_13, MY_14, MY_15, MY_16,
					MY_17, MY_18, MY_19, MY_20, MY_21, MY_22, MY_23);
		} else if (productConfiguration.contains(", MX,")) {
			categorizeProductConfiguration(productConfiguration, numberOfFeatures, MX_12, MX_13, MX_14, MX_15, MX_16,
					MX_17, MX_18, MX_19, MX_20, MX_21, MX_22, MX_23);
		} else if (productConfiguration.contains(", MS,")) {
			categorizeProductConfiguration(productConfiguration, numberOfFeatures, MS_12, MS_13, MS_14, MS_15, MS_16,
					MS_17, MS_18, MS_19, MS_20, MS_21, MS_22, MS_23);
		}

	}

	private static void categorizeProductConfiguration(String productConfiguration, int numberOfFeatures,
			Set<String> element12, Set<String> element13, Set<String> element14, Set<String> element15,
			Set<String> element16, Set<String> element17, Set<String> element18, Set<String> element19,
			Set<String> element20, Set<String> element21, Set<String> element22, Set<String> element23) {
		switch (numberOfFeatures) {
		case 12:
			element12.add(productConfiguration);
			break;
		case 13:
			element13.add(productConfiguration);
			break;
		case 14:
			element14.add(productConfiguration);
			break;
		case 15:
			element15.add(productConfiguration);
			break;
		case 16:
			element16.add(productConfiguration);
			break;
		case 17:
			element17.add(productConfiguration);
			break;
		case 18:
			element18.add(productConfiguration);
			break;
		case 19:
			element19.add(productConfiguration);
			break;
		case 20:
			element20.add(productConfiguration);
			break;
		case 21:
			element21.add(productConfiguration);
			break;
		case 22:
			element22.add(productConfiguration);
			break;
		case 23:
			element23.add(productConfiguration);
			break;
		}

	}
}
