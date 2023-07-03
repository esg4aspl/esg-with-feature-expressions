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
		case 24:
			p24(featureExpressionMap);
			break;
		case 25:
			p25(featureExpressionMap);
			break;
		case 26:
			p26(featureExpressionMap);
			break;
		case 27:
			p27(featureExpressionMap);
			break;
		case 28: 
			p28(featureExpressionMap);
			break;
		case 29:
			p29(featureExpressionMap);
			break;
		case 30:
			p30(featureExpressionMap);
			break;
		case 31:
			p31(featureExpressionMap);
			break;
		case 32:
			p32(featureExpressionMap);
			break;
		case 33:
			p33(featureExpressionMap);
			break;
		case 34:
			p34(featureExpressionMap);
			break;
		case 35:
			p35(featureExpressionMap);
			break;
		case 36:
			p36(featureExpressionMap);
			break;
		case 37:
			p37(featureExpressionMap);
			break;
		case 38: 
			p38(featureExpressionMap);
			break;
		case 39:
			p39(featureExpressionMap);
			break;
		case 40:
			p40(featureExpressionMap);
			break;
		case 41:
			p41(featureExpressionMap);
			break;
		}

	}
	/*
	 * 2 features
	 */
	//deposit, withdraw
	private static void p0(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(false);
		featureExpressionMap.get("cw").setTruthValue(false);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(false);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}

	/*
	 * 3 features
	 */
	//deposit, withdraw, interest
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
	
	//deposit, withdraw, cancelDeposit
	private static void p3(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(true);
		featureExpressionMap.get("cw").setTruthValue(false);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(false);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}
	
	//deposit, withdraw, cancelWithdraw
	private static void p6(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(false);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(false);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}
	
	//deposit, withdraw, credit
	private static void p24(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(false);
		featureExpressionMap.get("cw").setTruthValue(false);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(true);
		
		featureExpressionMap.get("i").setTruthValue(false);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}
	
	
	/*
	 * 4 features
	 */
	
	//deposit, withdraw, interest, interestEstimation
	private static void p2(Map<String, FeatureExpression> featureExpressionMap) {	
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(false);
		featureExpressionMap.get("cw").setTruthValue(false);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(true);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}
	
	//deposit, withdraw, cancelDeposit, interest
	private static void p4(Map<String, FeatureExpression> featureExpressionMap) {	
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(true);
		featureExpressionMap.get("cw").setTruthValue(false);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}
	
	//deposit, withdraw, interest, cancelWithdraw
	private static void p7(Map<String, FeatureExpression> featureExpressionMap) {	
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(false);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}
	
	
	//deposit, withdraw, cancelWithdraw, dailyLimit
	private static void p9(Map<String, FeatureExpression> featureExpressionMap) {	
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
	
	//deposit, withdraw, cancelDeposit, cancelWithdraw
	private static void p12(Map<String, FeatureExpression> featureExpressionMap) {	
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(true);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(false);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}

	//deposit, withdraw, credit, interest
	private static void p25(Map<String, FeatureExpression> featureExpressionMap) {
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
	
	//deposit, withdraw, cancelDeposit, credit
	private static void p27(Map<String, FeatureExpression> featureExpressionMap) {	
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(true);
		featureExpressionMap.get("cw").setTruthValue(false);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(true);
		
		featureExpressionMap.get("i").setTruthValue(false);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}
	
	//deposit, withdraw, credit, cancelWithdraw
	private static void p30(Map<String, FeatureExpression> featureExpressionMap) {	
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(false);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(true);
		
		featureExpressionMap.get("i").setTruthValue(false);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}
	
	/*
	 * 5 features
	 */
	
	//deposit, withdraw, cancelDeposit, interest, interestEstimation
	private static void p5(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(true);
		featureExpressionMap.get("cw").setTruthValue(false);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(true);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}
	
	//deposit, withdraw, interest, cancelWithdraw, interestEstimation
	private static void p8(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(false);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(true);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}
	
	//deposit, withdraw, interest, cancelWithdraw, dailyLimit
	private static void p10(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(false);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(true);
	}
	
	//deposit, withdraw, cancelDeposit, interest, cancelWithdraw
	private static void p13(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(true);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}
	
	//deposit, withdraw, cancelDeposit, cancelWithdraw, dailyLimit
	private static void p15(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(true);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(false);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(true);
	}
	
	//deposit, withdraw, cancelWithdraw, dailyLimit, overdraft
	private static void p18(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(false);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(true);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(false);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(true);
	}
		
	//deposit, withdraw, credit, interest, interestEstimation
	private static void p26(Map<String, FeatureExpression> featureExpressionMap) {
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
	
	//deposit, withdraw, cancelDeposit, credit, interest
	private static void p28(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(true);
		featureExpressionMap.get("cw").setTruthValue(false);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(true);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}
	
	//deposit, withdraw, credit, interest, cancelWithdraw
	private static void p31(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(false);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(true);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}
	
	//	deposit, withdraw, credit, cancelWithdraw, dailyLimit
	private static void p33(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(false);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(true);
		
		featureExpressionMap.get("i").setTruthValue(false);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(true);
	}

	//	deposit, withdraw, cancelDeposit, credit, cancelWithdraw
	private static void p36(Map<String, FeatureExpression> featureExpressionMap) {
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

	
	/*
	 * 6 features
	 */
	//deposit, withdraw, interest, cancelWithdraw, dailyLimit, interestEstimation
	private static void p11(Map<String, FeatureExpression> featureExpressionMap) {		
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(false);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(true);
		
		featureExpressionMap.get("dl").setTruthValue(true);
	}

	//deposit, withdraw, cancelDeposit, interest, cancelWithdraw, interestEstimation
	private static void p14(Map<String, FeatureExpression> featureExpressionMap) {
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
	
	//deposit, withdraw, cancelDeposit, interest, cancelWithdraw, dailyLimit
	private static void p16(Map<String, FeatureExpression> featureExpressionMap) {		
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
	
	//deposit, withdraw, interest, cancelWithdraw, dailyLimit, overdraft
	private static void p19(Map<String, FeatureExpression> featureExpressionMap) {		
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
	
	//deposit, withdraw, cancelDeposit, cancelWithdraw, dailyLimit, overdraft
	private static void p21(Map<String, FeatureExpression> featureExpressionMap) {		
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(true);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(true);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(false);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(true);
	}
	
	//deposit, withdraw, cancelDeposit, credit, interest, interestEstimation
	private static void p29(Map<String, FeatureExpression> featureExpressionMap) {		
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(true);
		featureExpressionMap.get("cw").setTruthValue(false);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(true);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(true);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}
	
	//deposit, withdraw, credit, interest, cancelWithdraw, interestEstimation
	private static void p32(Map<String, FeatureExpression> featureExpressionMap) {		
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(false);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(true);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(true);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}
	
	//deposit, withdraw, credit, interest, cancelWithdraw, dailyLimit
	private static void p34(Map<String, FeatureExpression> featureExpressionMap) {		
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(false);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(true);
	}
	
	//deposit, withdraw, cancelDeposit, credit, interest, cancelWithdraw
	private static void p37(Map<String, FeatureExpression> featureExpressionMap) {		
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(true);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(true);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}
	
	//deposit, withdraw, cancelDeposit, credit, cancelWithdraw, dailyLimit
	private static void p39(Map<String, FeatureExpression> featureExpressionMap) {		
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(true);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(true);
		
		featureExpressionMap.get("i").setTruthValue(false);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(true);
	}
	
	/*
	 * 7 features
	 */

	//deposit, withdraw, cancelDeposit, interest, cancelWithdraw, dailyLimit, interestEstimation
	private static void p17(Map<String, FeatureExpression> featureExpressionMap) {
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
	
	//deposit, withdraw, interest, cancelWithdraw, dailyLimit, interestEstimation, overdraft
	private static void p20(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(false);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(true);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(true);
		
		featureExpressionMap.get("dl").setTruthValue(true);
	}
	
	//deposit, withdraw, cancelDeposit, interest, cancelWithdraw, dailyLimit, overdraft
	private static void p22(Map<String, FeatureExpression> featureExpressionMap) {
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
	
	//deposit, withdraw, credit, interest, cancelWithdraw, dailyLimit, interestEstimation
	private static void p35(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(false);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(true);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(true);
		
		featureExpressionMap.get("dl").setTruthValue(true);
	}
	
	//deposit, withdraw, cancelDeposit, credit, interest, cancelWithdraw, interestEstimation
	private static void p38(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(true);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(true);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(true);
		
		featureExpressionMap.get("dl").setTruthValue(false);
	}
	
	//deposit, withdraw, cancelDeposit, credit, interest, cancelWithdraw, dailyLimit
	private static void p40(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(true);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(false);
		featureExpressionMap.get("c").setTruthValue(true);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(false);
		
		featureExpressionMap.get("dl").setTruthValue(true);
	}
	
	/*
	 * 8 features
	 */
	
	//deposit, withdraw, cancelDeposit, interest, cancelWithdraw, dailyLimit, interestEstimation, overdraft
	private static void p23(Map<String, FeatureExpression> featureExpressionMap) {
		featureExpressionMap.get("b").setTruthValue(true);
		featureExpressionMap.get("d").setTruthValue(true);
		featureExpressionMap.get("w").setTruthValue(true);
		
		featureExpressionMap.get("cd").setTruthValue(true);
		featureExpressionMap.get("cw").setTruthValue(true);
		
		featureExpressionMap.get("o").setTruthValue(true);
		featureExpressionMap.get("c").setTruthValue(false);
		
		featureExpressionMap.get("i").setTruthValue(true);
		featureExpressionMap.get("ie").setTruthValue(true);
		
		featureExpressionMap.get("dl").setTruthValue(true);
	}
	
	//deposit, withdraw, cancelDeposit, credit, interest, cancelWithdraw, dailyLimit, interestEstimation
	private static void p41(Map<String, FeatureExpression> featureExpressionMap) {
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
