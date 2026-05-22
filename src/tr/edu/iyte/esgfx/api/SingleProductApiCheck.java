package tr.edu.iyte.esgfx.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SingleProductApiCheck {

	private static final Pattern SUFFIX_PATTERN = Pattern.compile("_\\d+");
	private static final Pattern COVERAGE_PATTERN = Pattern.compile("L\\d\\s+is\\s+([0-9]+(?:\\.[0-9]+)?)%");
	private static final Pattern CONFIG_ID_PATTERN = Pattern.compile("^P(\\d+)$");

	private static final SplCase[] SPLS = new SplCase[] {
			new SplCase("SVM", "files/Cases/SodaVendingMachine/", "SVM_ESGFx.mxe"),
			new SplCase("eM", "files/Cases/eMail/", "eM_ESGFx.mxe"),
			new SplCase("El", "files/Cases/Elevator/", "El_ESGFx.mxe")
	};

	public static void main(String[] args) throws Exception {
		int grandPass = 0;
		int grandFail = 0;

		for (SplCase spl : SPLS) {
			System.out.println("### " + spl.name + " (" + spl.caseFolder + ")");

			LoadedSplModel model = SingleProductTestGenerationAPI.load(spl.caseFolder + "configs/model.xml",
					spl.caseFolder + spl.esgFxFileName);

			File configDir = new File(spl.caseFolder + "productConfigurations/");
			File[] configFiles = configDir.listFiles((dir, name) -> name.endsWith(".config"));
			Arrays.sort(configFiles, (f1, f2) -> f1.getName().compareTo(f2.getName()));

			int splPass = 0;
			int splFail = 0;

			for (int coverageLength = 1; coverageLength <= 4; coverageLength++) {
				int passCount = 0;
				int failCount = 0;

				for (File configFile : configFiles) {
					String productName = configFile.getName().replace(".config", "");
					Matcher idMatcher = CONFIG_ID_PATTERN.matcher(productName);
					if (!idMatcher.matches()) {
						continue;
					}
					int productId = Integer.parseInt(idMatcher.group(1));

					String groundTruthFile = spl.caseFolder + "testsequences/L" + coverageLength + "/" + productName
							+ "_L" + coverageLength + ".txt";
					File gtFile = new File(groundTruthFile);
					if (!gtFile.exists()) {
						continue;
					}

					SingleProductTestResult result = SingleProductTestGenerationAPI.generate(model, productId,
							configFile.getAbsolutePath(), coverageLength);

					GroundTruth gt = parseGroundTruth(gtFile);

					int gtTotalEvents = 0;
					for (List<String> s : gt.sequences) {
						gtTotalEvents += s.size();
					}

					boolean coverageMatch = Math.abs(result.getCoveragePercentage() - gt.coverage) < 0.01;
					boolean sequenceCountMatch = result.getSequenceCount() == gt.sequences.size();
					boolean totalEventCountMatch = result.getTotalEventCount() == gtTotalEvents;

					if (coverageMatch && sequenceCountMatch && totalEventCountMatch) {
						Map<String, Integer> apiMultiset = buildMultiset(result.getTestSequencesAsEventNames());
						Map<String, Integer> gtMultiset = buildMultiset(gt.sequences);
						String note = apiMultiset.equals(gtMultiset) ? ""
								: " (equivalent suite, different partition)";
						System.out.println("PASS " + spl.name + " " + productName + " L" + coverageLength + note);
						passCount++;
					} else {
						StringBuilder reason = new StringBuilder();
						if (!coverageMatch) {
							reason.append("coverage api=").append(result.getCoveragePercentage()).append(" gt=")
									.append(gt.coverage);
						}
						if (!sequenceCountMatch) {
							if (reason.length() > 0) {
								reason.append("; ");
							}
							reason.append("seqCount api=").append(result.getSequenceCount()).append(" gt=")
									.append(gt.sequences.size());
						}
						if (!totalEventCountMatch) {
							if (reason.length() > 0) {
								reason.append("; ");
							}
							reason.append("totalEvents api=").append(result.getTotalEventCount()).append(" gt=")
									.append(gtTotalEvents);
						}
						System.out.println("FAIL " + spl.name + " " + productName + " L" + coverageLength + " : "
								+ reason);
						failCount++;
					}
				}

				System.out.println("--- " + spl.name + " L" + coverageLength + " summary: " + passCount + " PASS, "
						+ failCount + " FAIL");
				splPass += passCount;
				splFail += failCount;
			}

			System.out.println("=== " + spl.name + " total: " + splPass + " PASS, " + splFail + " FAIL");
			grandPass += splPass;
			grandFail += splFail;
		}

		System.out.println("=== Grand total across all SPLs: " + grandPass + " PASS, " + grandFail + " FAIL");
	}

	private static Map<String, Integer> buildMultiset(List<List<String>> sequences) {
		Map<String, Integer> multiset = new LinkedHashMap<String, Integer>();
		for (List<String> seq : sequences) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < seq.size(); i++) {
				if (i > 0) {
					sb.append(",");
				}
				sb.append(stripSuffix(seq.get(i)));
			}
			multiset.merge(sb.toString(), 1, Integer::sum);
		}
		return multiset;
	}

	private static String stripSuffix(String token) {
		return SUFFIX_PATTERN.matcher(token).replaceAll("");
	}

	private static GroundTruth parseGroundTruth(File file) throws Exception {
		GroundTruth gt = new GroundTruth();
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}
				Matcher coverageMatcher = COVERAGE_PATTERN.matcher(line);
				if (coverageMatcher.find()) {
					gt.coverage = Double.parseDouble(coverageMatcher.group(1));
					continue;
				}
				int colon = line.indexOf(" : ");
				if (colon < 0) {
					continue;
				}
				String tail = line.substring(colon + 3).trim();
				if (tail.isEmpty()) {
					gt.sequences.add(new ArrayList<String>());
					continue;
				}
				String[] tokens = tail.split(",\\s*");
				List<String> seq = new ArrayList<String>(tokens.length);
				for (String t : tokens) {
					seq.add(t.trim());
				}
				gt.sequences.add(seq);
			}
		}
		return gt;
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
