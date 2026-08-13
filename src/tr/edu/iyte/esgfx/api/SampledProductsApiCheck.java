package tr.edu.iyte.esgfx.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks {@link SampledProductsTestGenerationAPI} against
 * {@link AllProductsTestGenerationAPI}: a sampled product carries its position
 * in the full enumeration, so its test suite has to equal the suite the
 * all-products run produced for that same position. Also checks that a seed
 * reproduces its sample.
 */
public class SampledProductsApiCheck {

	private static final int SAMPLE_SIZE = 5;

	private static final SplCase[] SPLS = new SplCase[] {
			new SplCase("SVM", "files/Cases/SodaVendingMachine/", "SVM_ESGFx.mxe"),
			new SplCase("eM", "files/Cases/eMail/", "eM_ESGFx.mxe"),
			new SplCase("El", "files/Cases/Elevator/", "El_ESGFx.mxe")
	};

	public static void main(String[] args) throws Exception {
		int grandPass = 0;
		int grandFail = 0;

		for (SplCase spl : SPLS) {
			LoadedSplModel model = SingleProductTestGenerationAPI.load(spl.caseFolder + "configs/model.xml",
					spl.caseFolder + spl.esgFxFileName);

			for (int coverageLength = 1; coverageLength <= 4; coverageLength++) {
				Map<Integer, SingleProductTestResult> allProducts = byProductId(
						AllProductsTestGenerationAPI.generateForAllProducts(model, coverageLength));

				List<SingleProductTestResult> sampled = SampledProductsTestGenerationAPI.generateForSample(
						model, SAMPLE_SIZE, coverageLength);

				int passCount = 0;
				int failCount = 0;

				for (SingleProductTestResult result : sampled) {
					SingleProductTestResult expected = allProducts.get(result.getProductId());
					if (expected == null) {
						System.out.println("FAIL " + spl.name + " L" + coverageLength + " : sampled product "
								+ result.getProductId() + " is not a valid configuration");
						failCount++;
						continue;
					}

					boolean matches = Math.abs(result.getCoveragePercentage()
							- expected.getCoveragePercentage()) < 0.01
							&& result.getSequenceCount() == expected.getSequenceCount()
							&& result.getTotalEventCount() == expected.getTotalEventCount()
							&& result.getTestSequencesAsEventNames()
									.equals(expected.getTestSequencesAsEventNames())
							&& result.getSelection().equals(expected.getSelection());

					if (matches) {
						passCount++;
					} else {
						System.out.println("FAIL " + spl.name + " L" + coverageLength + " : sampled product "
								+ result.getProductId() + " differs from the all-products run");
						failCount++;
					}
				}

				System.out.println("--- " + spl.name + " L" + coverageLength + " summary: " + sampled.size()
						+ " sampled of " + allProducts.size() + " products, " + passCount + " PASS, "
						+ failCount + " FAIL");
				grandPass += passCount;
				grandFail += failCount;
			}

			// Same seed, same sample; a different seed is free to differ.
			List<Integer> first = productIds(SampledProductsTestGenerationAPI.generateForSample(model,
					new UniformEnumerationSampler(), SAMPLE_SIZE, 42L, 1));
			List<Integer> repeat = productIds(SampledProductsTestGenerationAPI.generateForSample(model,
					new UniformEnumerationSampler(), SAMPLE_SIZE, 42L, 1));
			List<Integer> other = productIds(SampledProductsTestGenerationAPI.generateForSample(model,
					new UniformEnumerationSampler(), SAMPLE_SIZE, 7L, 1));

			if (first.equals(repeat)) {
				System.out.println("--- " + spl.name + " seed 42 reproduces its sample " + first);
				grandPass++;
			} else {
				System.out.println("FAIL " + spl.name + " : seed 42 gave " + first + " then " + repeat);
				grandFail++;
			}
			System.out.println("--- " + spl.name + " seed 7 drew " + other);
		}

		System.out.println("=== Grand total across all SPLs: " + grandPass + " PASS, " + grandFail + " FAIL");
	}

	private static Map<Integer, SingleProductTestResult> byProductId(List<SingleProductTestResult> results) {
		Map<Integer, SingleProductTestResult> byId = new LinkedHashMap<Integer, SingleProductTestResult>();
		for (SingleProductTestResult result : results) {
			byId.put(result.getProductId(), result);
		}
		return byId;
	}

	private static List<Integer> productIds(List<SingleProductTestResult> results) {
		List<Integer> ids = new ArrayList<Integer>(results.size());
		for (SingleProductTestResult result : results) {
			ids.add(result.getProductId());
		}
		return ids;
	}

	private static class SplCase {
		final String name;
		final String caseFolder;
		final String esgFxFileName;

		SplCase(String name, String caseFolder, String esgFxFileName) {
			this.name = name;
			this.caseFolder = caseFolder;
			this.esgFxFileName = esgFxFileName;
		}
	}
}
