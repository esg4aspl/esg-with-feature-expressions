package tr.edu.iyte.esgfx.cases.BankAccount;

import java.util.Map;

import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

public class CaseStudyUtilities_BA {

	protected static final String ESGFxFilePath = "files/Cases/BankAccount/BA_ESGFx.mxe";
	protected static final String featureModelFilePath = "files/Cases/BankAccount/configs/model.xml";
	protected static final String testsequencesFolderPath = "files/Cases/BankAccount/testsequences/eventcoverage/";
	protected static final String timemeasurementFolderPath = "files/Cases/BankAccount/timemeasurement/eventcoverage/";

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
		}

	}

	private static void p1(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(false);
		featureExpressionMap.get("cw").setTruthValue(false);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}
	

	private static void p2(Map<String, FeatureExpression> featureExpressionMap) {	
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(false);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(false);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(true);
	}

	private static void p3(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(false);
		featureExpressionMap.get("cw").setTruthValue(false);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(true);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(false);
		featureExpressionMap.get("dl").setTruthValue(false);
	}

	private static void p4(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(false);
		featureExpressionMap.get("cw").setTruthValue(false);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(true);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(true);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}

	private static void p5(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(true);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(true);
		
		featureExpressionMap.get("i").setTruthValue(false);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}

	private static void p6(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(true);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(true);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}

	private static void p7(Map<String, FeatureExpression> featureExpressionMap) {		
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(false);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(true);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(true);
	}

	private static void p8(Map<String, FeatureExpression> featureExpressionMap) {		
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(true);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(true);
	}

	private static void p9(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(true);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(true);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(true);
	}

	private static void p10(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(true);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(true);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(true);
		
		featureExpressionMap.get("dl").setTruthValue(true);
	}

}
