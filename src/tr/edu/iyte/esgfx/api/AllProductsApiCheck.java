package tr.edu.iyte.esgfx.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replays {@link AllProductsTestGenerationAPI} against the per-product test
 * sequences the RQ1 pipelines recorded. Beyond the sequences themselves this
 * checks the product numbering: the API has to reach the same configuration
 * under the same P#### id as the pipelines did, or the two cannot be compared
 * at all.
 */
public class AllProductsApiCheck {

	private static final Pattern SUFFIX_PATTERN = Pattern.compile("_\\d+");
	private static final Pattern COVERAGE_PATTERN = Pattern.compile("L\\d\\s+is\\s+([0-9]+(?:\\.[0-9]+)?)%");

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
				List<SingleProductTestResult> results = AllProductsTestGenerationAPI.generateForAllProducts(model,
						coverageLength);

				int passCount = 0;
				int failCount = 0;

				for (SingleProductTestResult result : results) {
					String productName = String.format("P%04d", result.getProductId());
					File groundTruthFile = new File(spl.caseFolder + "testsequences/L" + coverageLength + "/"
							+ productName + "_L" + coverageLength + ".txt");

					if (!groundTruthFile.exists()) {
						System.out.println("MISSING " + spl.name + " " + productName + " L" + coverageLength);
						failCount++;
						continue;
					}

					GroundTruth groundTruth = parseGroundTruth(groundTruthFile);
					int groundTruthEvents = 0;
					for (List<String> sequence : groundTruth.sequences) {
						groundTruthEvents += sequence.size();
					}

					boolean matches = Math.abs(result.getCoveragePercentage() - groundTruth.coverage) < 0.01
							&& result.getSequenceCount() == groundTruth.sequences.size()
							&& result.getTotalEventCount() == groundTruthEvents
							&& buildMultiset(result.getTestSequencesAsEventNames())
									.equals(buildMultiset(groundTruth.sequences));

					if (matches) {
						passCount++;
					} else {
						System.out.println("FAIL " + spl.name + " " + productName + " L" + coverageLength
								+ " : coverage " + result.getCoveragePercentage() + " vs " + groundTruth.coverage
								+ "; seqCount " + result.getSequenceCount() + " vs " + groundTruth.sequences.size()
								+ "; totalEvents " + result.getTotalEventCount() + " vs " + groundTruthEvents);
						failCount++;
					}
				}

				System.out.println("--- " + spl.name + " L" + coverageLength + " summary: " + results.size()
						+ " products, " + passCount + " PASS, " + failCount + " FAIL");
				grandPass += passCount;
				grandFail += failCount;
			}
		}

		System.out.println("=== Grand total across all SPLs: " + grandPass + " PASS, " + grandFail + " FAIL");
	}

	private static Map<String, Integer> buildMultiset(List<List<String>> sequences) {
		Map<String, Integer> multiset = new LinkedHashMap<String, Integer>();
		for (List<String> sequence : sequences) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < sequence.size(); i++) {
				if (i > 0) {
					sb.append(",");
				}
				sb.append(SUFFIX_PATTERN.matcher(sequence.get(i)).replaceAll(""));
			}
			multiset.merge(sb.toString(), 1, Integer::sum);
		}
		return multiset;
	}

	private static GroundTruth parseGroundTruth(File file) throws Exception {
		GroundTruth groundTruth = new GroundTruth();
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}
				Matcher coverageMatcher = COVERAGE_PATTERN.matcher(line);
				if (coverageMatcher.find()) {
					groundTruth.coverage = Double.parseDouble(coverageMatcher.group(1));
					continue;
				}
				int colon = line.indexOf(" : ");
				if (colon < 0) {
					continue;
				}
				String tail = line.substring(colon + 3).trim();
				List<String> sequence = new ArrayList<String>();
				if (!tail.isEmpty()) {
					for (String token : tail.split(",\\s*")) {
						sequence.add(token.trim());
					}
				}
				groundTruth.sequences.add(sequence);
			}
		}
		return groundTruth;
	}

	private static class GroundTruth {
		final List<List<String>> sequences = new ArrayList<List<String>>();
		double coverage;
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
