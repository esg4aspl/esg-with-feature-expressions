package tr.edu.iyte.esgfx.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import tr.edu.iyte.esgfx.productconfigurationgeneration.ProductConfigurationValidator;

/**
 * Samples configurations with UniGen, a SAT-based almost-uniform sampler.
 *
 * <p>Unlike {@link UniformEnumerationSampler} this does not walk the
 * configuration space, so it keeps working where enumeration stops being
 * possible. UniGen is a native tool rather than a library, so it runs as a
 * separate process over a deliberately small protocol:
 *
 * <ul>
 * <li>the DIMACS CNF, including its {@code c ind} sampling set, on stdin;
 * <li>{@code <sample count> <seed>} as arguments;
 * <li>one sample per line on stdout, as space-separated signed literals.
 * </ul>
 *
 * <p>Keeping the protocol here rather than parsing a particular UniGen release's
 * console output means a new release, or a different sampler entirely, only
 * needs a different bridge command.
 *
 * <p>UniGen samples the CNF; the feature model is still the authority on what
 * is valid, so every sample is validated before it is returned.
 */
public final class UniGenSampler implements ProductConfigurationSampler {

	public static final long DEFAULT_TIMEOUT_SECONDS = 120;

	private final List<String> command;
	private final long timeoutSeconds;

	public UniGenSampler(List<String> command) {
		this(command, DEFAULT_TIMEOUT_SECONDS);
	}

	public UniGenSampler(List<String> command, long timeoutSeconds) {
		if (command == null || command.isEmpty()) {
			throw new IllegalArgumentException("A sampler command is required");
		}
		this.command = new ArrayList<String>(command);
		this.timeoutSeconds = timeoutSeconds;
	}

	/** Whether the bridge answers, so the caller can offer this sampler or explain its absence. */
	public boolean isAvailable() {
		try {
			List<String> probe = new ArrayList<String>(command);
			probe.add("--probe");
			Process process = new ProcessBuilder(probe).redirectErrorStream(true).start();
			process.getOutputStream().close();
			String output = readFully(process);
			if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
				process.destroyForcibly();
				return false;
			}
			return process.exitValue() == 0 && output.contains("ok");
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public List<SampledConfiguration> sample(LoadedSplModel model, int sampleSize, long seed) throws Exception {
		if (sampleSize < 1) {
			throw new IllegalArgumentException("sampleSize must be at least 1, got " + sampleSize);
		}

		FeatureModelCnf cnf = FeatureModelCnf.of(model);
		List<int[]> assignments = runBridge(cnf.toDimacs(), sampleSize, seed);

		synchronized (model) {
			Map<String, tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression> featureExpressionMap =
					model.getFeatureExpressionMap();
			ProductConfigurationValidator validator = new ProductConfigurationValidator();

			List<SampledConfiguration> sampled = new ArrayList<SampledConfiguration>(assignments.size());
			Set<String> alreadyDrawn = new LinkedHashSet<String>();

			for (int[] assignment : assignments) {
				Map<String, Boolean> selection = cnf.selectionOf(assignment);

				// UniGen draws with replacement, so the same configuration can come
				// up more than once. Generating its tests again would produce the
				// same suite, so a repeat is dropped and the sample comes back
				// smaller rather than padded out, which would skew the draw.
				if (!alreadyDrawn.add(selection.toString())) {
					continue;
				}

				for (Map.Entry<String, Boolean> entry : selection.entrySet()) {
					tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression featureExpression =
							featureExpressionMap.get(entry.getKey());
					if (featureExpression != null) {
						featureExpression.setTruthValue(entry.getValue());
					}
				}
				if (!validator.validate(model.getFeatureModel(), featureExpressionMap)) {
					continue;
				}

				// UniGen does not enumerate, so it cannot know a configuration's
				// position in the enumeration; the sample's own ordinal stands in.
				sampled.add(new SampledConfiguration(sampled.size() + 1, selection));
			}
			return sampled;
		}
	}

	private List<int[]> runBridge(String dimacs, int sampleSize, long seed) throws Exception {
		List<String> invocation = new ArrayList<String>(command);
		invocation.addAll(Arrays.asList(String.valueOf(sampleSize), String.valueOf(seed)));

		Process process;
		try {
			process = new ProcessBuilder(invocation).start();
		} catch (IOException e) {
			throw new SamplerUnavailableException(
					"Could not start the UniGen bridge (" + command.get(0) + "): " + e.getMessage());
		}

		try (OutputStream input = process.getOutputStream()) {
			input.write(dimacs.getBytes(StandardCharsets.UTF_8));
		}

		String output = readFully(process);
		String errors = readErrors(process);

		if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
			process.destroyForcibly();
			throw new SamplerUnavailableException(
					"The UniGen bridge did not finish within " + timeoutSeconds + " seconds.");
		}
		if (process.exitValue() != 0) {
			throw new SamplerUnavailableException("The UniGen bridge failed: "
					+ (errors.isEmpty() ? output : errors).trim());
		}

		return parseSamples(output);
	}

	static List<int[]> parseSamples(String output) {
		List<int[]> assignments = new ArrayList<int[]>();
		for (String line : output.split("\n")) {
			String trimmed = line.trim();
			if (trimmed.isEmpty() || trimmed.startsWith("c")) {
				continue;
			}
			String[] tokens = trimmed.split("\\s+");
			List<Integer> literals = new ArrayList<Integer>(tokens.length);
			for (String token : tokens) {
				int literal = Integer.parseInt(token);
				// A trailing 0 terminates a DIMACS-style assignment.
				if (literal != 0) {
					literals.add(literal);
				}
			}
			if (literals.isEmpty()) {
				continue;
			}
			int[] assignment = new int[literals.size()];
			for (int i = 0; i < literals.size(); i++) {
				assignment[i] = literals.get(i);
			}
			assignments.add(assignment);
		}
		return assignments;
	}

	private static String readFully(Process process) throws IOException {
		StringBuilder text = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				text.append(line).append('\n');
			}
		}
		return text.toString();
	}

	private static String readErrors(Process process) throws IOException {
		StringBuilder text = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				text.append(line).append('\n');
			}
		}
		return text.toString();
	}
}
