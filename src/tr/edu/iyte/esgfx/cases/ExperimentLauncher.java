package tr.edu.iyte.esgfx.cases;

/**
 * Starts one experiment driver for one case study.
 *
 * Arguments: the package of the case study, its short name and the driver,
 * for example "eMail eM RQ1_ESGFx_L234". The settings of a run (shard, run
 * number, level, seed, time budget) are read from the environment by the
 * drivers themselves.
 *
 * Drivers: ProductModels, RQ1_ESGFx_L1, RQ1_ESGFx_L234, RQ1_EFG_L234,
 * RQ1_RandomWalk, RQ2_L1, RQ2_L234, RQ2_EFG_L234, RQ2_RandomWalk,
 * RQ3_EdgeOmitter, RQ3_EventOmitter, RQ3_SequenceOmitter.
 */
public final class ExperimentLauncher {

	private ExperimentLauncher() {
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			throw new IllegalArgumentException("Arguments: <case package> <short name> <driver>");
		}
		String casePackage = args[0];
		String shortName = args[1];
		String driver = args[2];

		RunMeasurements.markStart();
		CaseStudyUtilities.coverageLength = 2;
		Class.forName("tr.edu.iyte.esgfx.cases." + casePackage + ".CaseStudyUtilities_" + shortName)
				.getMethod("initializeFilePaths").invoke(null);

		switch (driver) {
		case "ProductModels":
			// Tesla and Hockerty Shirts: the products of the stored UniGen sample.
			// The other lines: all products.
			if (shortName.equals("Te") || shortName.equals("HS")) {
				new SamplesToProductESGFx().writeProductESGFxToFile();
			} else {
				new ProductESGFxToEFGAndDOTFileWriter().writeProductESGFxToFile();
			}
			break;
		case "RQ1_ESGFx_L1":
			new RQ1_ComparativeEfficiency_ESGFx_L1().measurePipelineForEventCoverage();
			break;
		case "RQ1_ESGFx_L234":
			new RQ1_ComparativeEfficiency_ESGFx_L234().measurePipelineForEdgeCoverage();
			break;
		case "RQ1_EFG_L234":
			new RQ1_ComparativeEfficiency_EFG_L234().measureTotalTimeForEFGPipeline();
			break;
		case "RQ1_RandomWalk":
			new RQ1_ComparativeEfficiency_RandomWalk().measurePipeLineForRandomWalk();
			break;
		case "RQ2_L1":
			new RQ2_ExtremeScalability_L1().measurePipelineForESGFxEventCoverage();
			break;
		case "RQ2_L234":
			new RQ2_ExtremeScalability_L234().measureToTalTimeForEdgeCoverage();
			break;
		case "RQ2_EFG_L234":
			new RQ2_ExtremeScalability_EFG_L234().measureEFGScalability();
			break;
		case "RQ2_RandomWalk":
			new RQ2_ExtremeScalability_RandomWalk().measureRandomWalkScalability();
			break;
		case "RQ3_EdgeOmitter":
			new RQ3_FaultDetection_EdgeOmitter().evaluateFaultDetection();
			break;
		case "RQ3_EventOmitter":
			new RQ3_FaultDetection_EventOmitter().evaluateFaultDetection();
			break;
		case "RQ3_SequenceOmitter":
			new RQ3_FaultDetection_SequenceOmitter().evaluateFaultDetection();
			break;
		default:
			throw new IllegalArgumentException("Unknown driver: " + driver);
		}
	}
}
