package tr.edu.iyte.esgfx.cases.SVM;

import java.util.Map;

import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.Negation;

public class CaseStudyUtilities_SVM {

	protected static final String ESGFxFilePath = "files/Cases/SodaVendingMachine/SVM_ESGFx.mxe";
	protected static final String featureModelFilePath = "files/Cases/SodaVendingMachine/configs/model.xml";
	protected static final String testsequencesFolderPath = "files/Cases/SodaVendingMachine/testsequences/eventcoverage/";
	protected static final String timemeasurementFolderPath = "files/Cases/SodaVendingMachine/timemeasurement/eventcoverage/";

	public static void configureProduct(int productID, Map<String, FeatureExpression> featureExpressionMap) {
		switch (productID) {
		case 1:
			p1(featureExpressionMap);
			break;

		case 2:
			p2(featureExpressionMap);
			break;
		case 3:
			p3(featureExpressionMap);
			break;
		case 4:
			p4(featureExpressionMap);
			break;
		case 5:
			p5(featureExpressionMap);
			break;
		case 6:
			p6(featureExpressionMap);
			break;
		case 7:
			p7(featureExpressionMap);
			break;
		case 8: 
			p8(featureExpressionMap);
			break;
		case 9:
			p9(featureExpressionMap);
			break;
		case 10:
			p10(featureExpressionMap);
			break;
		case 11:
			p11(featureExpressionMap);
			break;
		case 12:
			p12(featureExpressionMap);
			break;
		}
		

	}

	private static void p1(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("s").setTruthValue(true);
		featureExpressionMap.get("t").setTruthValue(false);
		featureExpressionMap.get("f").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(false);
	}

	private static void p2(Map<String, FeatureExpression> featureExpressionMap) {	
		featureExpressionMap.get("s").setTruthValue(false);
		featureExpressionMap.get("t").setTruthValue(true);
		featureExpressionMap.get("f").setTruthValue(true);
		featureExpressionMap.get("c").setTruthValue(false);
	}
	
	private static void p3(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("s").setTruthValue(true);
		featureExpressionMap.get("t").setTruthValue(true);
		Negation neg = (Negation)featureExpressionMap.get("!f");
		neg.setTruthValueExplicitly(true);
		featureExpressionMap.get("f").setTruthValue(true);
		featureExpressionMap.get("c").setTruthValue(false);
	}

	private static void p4(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("s").setTruthValue(true);
		featureExpressionMap.get("t").setTruthValue(true);
		Negation neg = (Negation)featureExpressionMap.get("!f");
		neg.setTruthValueExplicitly(true);
		featureExpressionMap.get("f").setTruthValue(true);
		featureExpressionMap.get("c").setTruthValue(false);
	}

	private static void p5(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("s").setTruthValue(false);
		featureExpressionMap.get("t").setTruthValue(true);
		featureExpressionMap.get("f").setTruthValue(true);
		featureExpressionMap.get("c").setTruthValue(false);
	}

	private static void p6(Map<String, FeatureExpression> featureExpressionMap) {
		
		featureExpressionMap.get("s").setTruthValue(true);
		featureExpressionMap.get("t").setTruthValue(false);
		Negation neg = (Negation)featureExpressionMap.get("!f");
		neg.setTruthValueExplicitly(true);
		featureExpressionMap.get("f").setTruthValue(true);
		featureExpressionMap.get("c").setTruthValue(true);
	}

	private static void p7(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("s").setTruthValue(false);
		featureExpressionMap.get("t").setTruthValue(true);
		featureExpressionMap.get("f").setTruthValue(true);
		featureExpressionMap.get("c").setTruthValue(true);
	}
	
	private static void p8(Map<String, FeatureExpression> featureExpressionMap) {		
		featureExpressionMap.get("s").setTruthValue(true);
		featureExpressionMap.get("t").setTruthValue(true);
		Negation neg = (Negation)featureExpressionMap.get("!f");
		neg.setTruthValueExplicitly(true);
		featureExpressionMap.get("f").setTruthValue(true);
		featureExpressionMap.get("c").setTruthValue(true);
	}

	private static void p9(Map<String, FeatureExpression> featureExpressionMap) {		
		featureExpressionMap.get("s").setTruthValue(true);
		featureExpressionMap.get("t").setTruthValue(false);
		Negation neg = (Negation)featureExpressionMap.get("!f");
		neg.setTruthValueExplicitly(true);
		featureExpressionMap.get("f").setTruthValue(true);
		featureExpressionMap.get("c").setTruthValue(true);
	}

	private static void p10(Map<String, FeatureExpression> featureExpressionMap) {		
		featureExpressionMap.get("s").setTruthValue(false);
		featureExpressionMap.get("t").setTruthValue(true);
		featureExpressionMap.get("f").setTruthValue(true);
		featureExpressionMap.get("c").setTruthValue(true);
	}

	private static void p11(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("s").setTruthValue(true);
		featureExpressionMap.get("t").setTruthValue(true);
		Negation neg = (Negation)featureExpressionMap.get("!f");
		neg.setTruthValueExplicitly(true);
		featureExpressionMap.get("f").setTruthValue(true);
		featureExpressionMap.get("c").setTruthValue(false);
	}

	private static void p12(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("s").setTruthValue(true);
		featureExpressionMap.get("t").setTruthValue(true);
		Negation neg = (Negation)featureExpressionMap.get("!f");
		neg.setTruthValueExplicitly(true);
		featureExpressionMap.get("f").setTruthValue(true);
		featureExpressionMap.get("c").setTruthValue(true);
	}
	

	


}
