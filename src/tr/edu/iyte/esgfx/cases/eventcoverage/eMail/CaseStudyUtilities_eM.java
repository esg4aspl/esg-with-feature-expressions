package tr.edu.iyte.esgfx.cases.eventcoverage.eMail;

import java.util.Map;

import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

public class CaseStudyUtilities_eM {

	protected static final String ESGFxFilePath = "files/Cases/eMail/eM_ESGFx.mxe";
	protected static final String featureModelFilePath = "files/Cases/eMail/configs/model.xml";
	protected static String testsequencesFolderPath = "files/Cases/eMail/testsequences/eventcoverage/";
	protected static String timemeasurementFolderPath = "files/Cases/eMail/timemeasurement/eventcoverage/";

	public static void configureProduct(int productID, Map<String, FeatureExpression> featureExpressionMap) {
		switch (productID) {
		case 0:
			p0(featureExpressionMap);
			break;
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
		case 13:
			p13(featureExpressionMap);
			break;
		case 14:
			p14(featureExpressionMap);
			break;
		case 15:
			p15(featureExpressionMap);
			break;
		case 16:
			p16(featureExpressionMap);
			break;
		case 17:
			p17(featureExpressionMap);
			break;
		case 18:
			p18(featureExpressionMap);
			break;
		case 19:
			p19(featureExpressionMap);
			break;
		case 20:
			p20(featureExpressionMap);
			break;
		case 21:
			p21(featureExpressionMap);
			break;
		case 22:
			p22(featureExpressionMap);
			break;
		case 23:
			p23(featureExpressionMap);
			break;
		}

	}

	/*
	 * 0 feature
	 */
	private static void p0(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(false);
		featureExpressionMap.get("au").setTruthValue(false);
		featureExpressionMap.get("f").setTruthValue(false);
		featureExpressionMap.get("en").setTruthValue(false);
		featureExpressionMap.get("s").setTruthValue(false);
	}

	/*
	 * 1 feature
	 */
	// addressbook
	private static void p1(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(true);
		featureExpressionMap.get("au").setTruthValue(false);
		featureExpressionMap.get("f").setTruthValue(false);
		featureExpressionMap.get("en").setTruthValue(false);
		featureExpressionMap.get("s").setTruthValue(false);
	}

	// autoresponder
	private static void p2(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(false);
		featureExpressionMap.get("au").setTruthValue(true);
		featureExpressionMap.get("f").setTruthValue(false);
		featureExpressionMap.get("en").setTruthValue(false);
		featureExpressionMap.get("s").setTruthValue(false);
	}

	// forward
	private static void p4(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(false);
		featureExpressionMap.get("au").setTruthValue(false);
		featureExpressionMap.get("f").setTruthValue(true);
		featureExpressionMap.get("en").setTruthValue(false);
		featureExpressionMap.get("s").setTruthValue(false);
	}

	// encrypt
	private static void p8(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(false);
		featureExpressionMap.get("au").setTruthValue(false);
		featureExpressionMap.get("f").setTruthValue(false);
		featureExpressionMap.get("en").setTruthValue(true);
		featureExpressionMap.get("s").setTruthValue(false);
	}

	// sign
	private static void p12(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(false);
		featureExpressionMap.get("au").setTruthValue(false);
		featureExpressionMap.get("f").setTruthValue(false);
		featureExpressionMap.get("en").setTruthValue(false);
		featureExpressionMap.get("s").setTruthValue(true);
	}

	/*
	 * 2 features
	 */
	// addressbook, autoresponder
	private static void p3(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(true);
		featureExpressionMap.get("au").setTruthValue(true);
		featureExpressionMap.get("f").setTruthValue(false);
		featureExpressionMap.get("en").setTruthValue(false);
		featureExpressionMap.get("s").setTruthValue(false);
	}

	// addressbook, forward
	private static void p5(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(true);
		featureExpressionMap.get("au").setTruthValue(false);
		featureExpressionMap.get("f").setTruthValue(true);
		featureExpressionMap.get("en").setTruthValue(false);
		featureExpressionMap.get("s").setTruthValue(false);
	}

	// autoresponder, forward
	private static void p6(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(false);
		featureExpressionMap.get("au").setTruthValue(true);
		featureExpressionMap.get("f").setTruthValue(true);
		featureExpressionMap.get("en").setTruthValue(false);
		featureExpressionMap.get("s").setTruthValue(false);
	}

	// addressbook, encrypt
	private static void p9(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(true);
		featureExpressionMap.get("au").setTruthValue(false);
		featureExpressionMap.get("f").setTruthValue(false);
		featureExpressionMap.get("en").setTruthValue(true);
		featureExpressionMap.get("s").setTruthValue(false);
	}

	// autoresponder, encrypt
	private static void p10(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(false);
		featureExpressionMap.get("au").setTruthValue(true);
		featureExpressionMap.get("f").setTruthValue(false);
		featureExpressionMap.get("en").setTruthValue(true);
		featureExpressionMap.get("s").setTruthValue(false);
	}

	// addressbook, sign
	private static void p13(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(true);
		featureExpressionMap.get("au").setTruthValue(false);
		featureExpressionMap.get("f").setTruthValue(false);
		featureExpressionMap.get("en").setTruthValue(false);
		featureExpressionMap.get("s").setTruthValue(true);
	}

	// autoresponder, sign
	private static void p14(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(false);
		featureExpressionMap.get("au").setTruthValue(true);
		featureExpressionMap.get("f").setTruthValue(false);
		featureExpressionMap.get("en").setTruthValue(false);
		featureExpressionMap.get("s").setTruthValue(true);
	}

	// forward, sign
	private static void p16(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(false);
		featureExpressionMap.get("au").setTruthValue(false);
		featureExpressionMap.get("f").setTruthValue(true);
		featureExpressionMap.get("en").setTruthValue(false);
		featureExpressionMap.get("s").setTruthValue(true);
	}

	// encrypt, sign
	private static void p20(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(false);
		featureExpressionMap.get("au").setTruthValue(false);
		featureExpressionMap.get("f").setTruthValue(false);
		featureExpressionMap.get("en").setTruthValue(true);
		featureExpressionMap.get("s").setTruthValue(true);
	}

	/*
	 * 3 features
	 */

	// addressbook, autoresponder, forward
	private static void p7(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(true);
		featureExpressionMap.get("au").setTruthValue(true);
		featureExpressionMap.get("f").setTruthValue(true);
		featureExpressionMap.get("en").setTruthValue(false);
		featureExpressionMap.get("s").setTruthValue(false);
	}

	// addressbook, autoresponder, encrypt
	private static void p11(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(true);
		featureExpressionMap.get("au").setTruthValue(true);
		featureExpressionMap.get("f").setTruthValue(false);
		featureExpressionMap.get("en").setTruthValue(true);
		featureExpressionMap.get("s").setTruthValue(false);
	}

	// addressbook, autoresponder, sign
	private static void p15(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(true);
		featureExpressionMap.get("au").setTruthValue(true);
		featureExpressionMap.get("f").setTruthValue(false);
		featureExpressionMap.get("en").setTruthValue(false);
		featureExpressionMap.get("s").setTruthValue(true);
	}

	// addressbook, forward, sign
	private static void p17(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(true);
		featureExpressionMap.get("au").setTruthValue(false);
		featureExpressionMap.get("f").setTruthValue(true);
		featureExpressionMap.get("en").setTruthValue(false);
		featureExpressionMap.get("s").setTruthValue(true);
	}

	// autoresponder, forward, sign
	private static void p18(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(false);
		featureExpressionMap.get("au").setTruthValue(true);
		featureExpressionMap.get("f").setTruthValue(true);
		featureExpressionMap.get("en").setTruthValue(false);
		featureExpressionMap.get("s").setTruthValue(true);
	}

	// addressbook, encrypt, sign
	private static void p21(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(true);
		featureExpressionMap.get("au").setTruthValue(false);
		featureExpressionMap.get("f").setTruthValue(false);
		featureExpressionMap.get("en").setTruthValue(true);
		featureExpressionMap.get("s").setTruthValue(true);
	}

	// autoresponder, encrypt, sign
	private static void p22(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(false);
		featureExpressionMap.get("au").setTruthValue(true);
		featureExpressionMap.get("f").setTruthValue(false);
		featureExpressionMap.get("en").setTruthValue(true);
		featureExpressionMap.get("s").setTruthValue(true);
	}

	/*
	 * 4 features
	 */

	// addressbook, autoresponder, forward, sign
	private static void p19(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(true);
		featureExpressionMap.get("au").setTruthValue(true);
		featureExpressionMap.get("f").setTruthValue(true);
		featureExpressionMap.get("en").setTruthValue(false);
		featureExpressionMap.get("s").setTruthValue(true);
	}

	// addressbook, autoresponder, encrypt, sign
	private static void p23(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("e").setTruthValue(true);
		featureExpressionMap.get("ad").setTruthValue(true);
		featureExpressionMap.get("au").setTruthValue(true);
		featureExpressionMap.get("f").setTruthValue(false);
		featureExpressionMap.get("en").setTruthValue(true);
		featureExpressionMap.get("s").setTruthValue(true);
	}

}