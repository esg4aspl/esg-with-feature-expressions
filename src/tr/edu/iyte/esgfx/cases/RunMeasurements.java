package tr.edu.iyte.esgfx.cases;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.testgeneration.SequenceCoverageAnalyser;

/**
 * Measurements that every experiment driver records in the same way: the
 * replay check and the sequence coverage of a suite, the real elapsed time
 * of the JVM, the peak resident memory of the process, and the suites
 * themselves.
 */
public final class RunMeasurements {

	/** Taken when the class is loaded, which the drivers force at their first line. */
	private static final long START_NANOS = System.nanoTime();

	private RunMeasurements() {
	}

	public static void markStart() {
		// Loading the class sets START_NANOS.
	}

	public static double elapsedMs() {
		return (System.nanoTime() - START_NANOS) / 1_000_000.0;
	}

	// ------------------------------------------------------------------
	// Replay check and sequence coverage of one suite
	// ------------------------------------------------------------------

	public static final String PRODUCT_COLUMNS = "Sequences;Events;ReplayViolations;InfeasibleSequences;"
			+ "EventsRequired;EventsCovered;CouplesRequired;CouplesCovered;"
			+ "TriplesRequired;TriplesCovered;QuadruplesRequired;QuadruplesCovered;CheckTime(us)";

	/** Sums of the per-product values, for drivers that do not write one row per product. */
	public static class Totals {
		public long products;
		public long sequences;
		public long events;
		public long replayViolations;
		public long infeasibleSequences;
		/** -1 for a length that no product of this JVM was measured at. */
		public final long[] required = { -1, -1, -1, -1, -1 };
		public final long[] covered = { -1, -1, -1, -1, -1 };
		public long checkTimeNanos;

		public static final String COLUMNS = "CheckedProducts;" + PRODUCT_COLUMNS;

		public String toRow() {
			return products + ";" + sequences + ";" + events + ";" + replayViolations + ";" + infeasibleSequences + ";"
					+ required[1] + ";" + covered[1] + ";" + required[2] + ";" + covered[2] + ";" + required[3] + ";"
					+ covered[3] + ";" + required[4] + ";" + covered[4] + ";" + (checkTimeNanos / 1000);
		}
	}

	/**
	 * Replays an unfolded suite on the product-level model and measures its
	 * coverage of the events, event couples, event triples and event
	 * quadruples of that model. Returns the values in the order of
	 * PRODUCT_COLUMNS and adds them to the totals if totals are given. A
	 * length that is not in 'lengths' is written as -1.
	 */
	public static String check(ESG productESGFx, Set<EventSequence> unfoldedSuite, Totals totals,
			int... requestedLengths) {
		// Every length once, so that a level named twice is not counted twice.
		int[] lengths = java.util.Arrays.stream(requestedLengths).distinct().toArray();
		long start = System.nanoTime();
		long[] required = { -1, -1, -1, -1, -1 };
		long[] covered = { -1, -1, -1, -1, -1 };
		int sequences = 0, events = 0, violations = 0, infeasible = 0;
		if (productESGFx != null && unfoldedSuite != null) {
			SequenceCoverageAnalyser analyser = new SequenceCoverageAnalyser(productESGFx);
			SequenceCoverageAnalyser.Replay replay = analyser.replay(unfoldedSuite);
			sequences = replay.sequences;
			events = replay.events;
			violations = replay.violations();
			infeasible = replay.infeasibleSequences;
			for (int k : lengths) {
				SequenceCoverageAnalyser.Coverage coverage = analyser.coverage(unfoldedSuite, k);
				required[k] = coverage.required;
				covered[k] = coverage.covered;
			}
		}
		long nanos = System.nanoTime() - start;
		lastCheckNanos = nanos;
		if (totals != null) {
			totals.products++;
			totals.sequences += sequences;
			totals.events += events;
			totals.replayViolations += violations;
			totals.infeasibleSequences += infeasible;
			for (int k : lengths) {
				if (required[k] >= 0) {
					totals.required[k] = Math.max(0, totals.required[k]) + required[k];
					totals.covered[k] = Math.max(0, totals.covered[k]) + covered[k];
				}
			}
			totals.checkTimeNanos += nanos;
		}
		return sequences + ";" + events + ";" + violations + ";" + infeasible + ";" + required[1] + ";" + covered[1]
				+ ";" + required[2] + ";" + covered[2] + ";" + required[3] + ";" + covered[3] + ";" + required[4] + ";"
				+ covered[4] + ";" + (nanos / 1000);
	}

	private static long lastCheckNanos;

	/** Duration of the last call of check(), for drivers that keep it out of a time budget. */
	public static long lastCheckNanos() {
		return lastCheckNanos;
	}

	/** Total time of all garbage collections of this JVM so far, in milliseconds. */
	public static long gcTimeMs() {
		long total = 0;
		for (java.lang.management.GarbageCollectorMXBean bean : java.lang.management.ManagementFactory
				.getGarbageCollectorMXBeans()) {
			total += Math.max(0, bean.getCollectionTime());
		}
		return total;
	}

	/** Heap in use now, in bytes. */
	public static long usedHeapBytes() {
		return java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
	}

	/**
	 * Stops the run if a per-product log of an earlier version is in the way.
	 * The drivers append to their log, so rows with the present columns under
	 * a header without them would make the file unreadable. Results of earlier
	 * runs must be moved away before a run starts.
	 */
	public static void refuseLogOfEarlierVersion(File logFile) {
		if (!logFile.exists() || logFile.length() == 0) {
			return;
		}
		String firstLine = null;
		try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
			firstLine = reader.readLine();
		} catch (IOException e) {
			throw new IllegalStateException("Cannot read the existing log " + logFile.getAbsolutePath(), e);
		}
		if (firstLine == null || !firstLine.contains(PRODUCT_COLUMNS)) {
			throw new IllegalStateException("The log " + logFile.getAbsolutePath()
					+ " was written by an earlier version and has other columns. Move the results of earlier runs"
					+ " away before starting this run.");
		}
	}

	// ------------------------------------------------------------------
	// Process memory
	// ------------------------------------------------------------------

	/**
	 * Peak resident set size of this process in kilobytes (VmHWM of
	 * /proc/self/status), or -1 where that file does not exist.
	 */
	public static long processPeakRssKb() {
		return readStatusKb("/proc/self/status", "VmHWM:");
	}

	private static long readStatusKb(String path, String key) {
		File status = new File(path);
		if (!status.exists()) {
			return -1;
		}
		try (BufferedReader reader = new BufferedReader(new FileReader(status))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith(key)) {
					return Long.parseLong(line.substring(key.length()).replace("kB", "").trim());
				}
			}
		} catch (IOException | NumberFormatException e) {
			return -1;
		}
		return -1;
	}

	private static String cpuModel() {
		File cpuinfo = new File("/proc/cpuinfo");
		if (!cpuinfo.exists()) {
			return System.getProperty("os.arch");
		}
		try (BufferedReader reader = new BufferedReader(new FileReader(cpuinfo))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("model name")) {
					return line.substring(line.indexOf(':') + 1).trim().replace(';', ',');
				}
			}
		} catch (IOException e) {
			return "unknown";
		}
		return "unknown";
	}

	// ------------------------------------------------------------------
	// One row per JVM
	// ------------------------------------------------------------------

	/**
	 * Appends one row for this JVM to a CSV file next to the summary file of
	 * the driver: real elapsed time, peak resident memory of the process,
	 * peak resident memory of the largest child process (-1 if none was
	 * measured), the machine, and any further columns of the driver.
	 */
	public static void writeJvmRow(String summaryResultPath, int runID, int shard, int shards, String approach,
			String level, int handledProducts, int failedProducts, long childPeakRssKb, String extraHeader,
			String extraRow) {
		try {
			File summary = new File(summaryResultPath);
			String name = summary.getName().replaceAll("(?i)\\.csv$", "") + "_jvm.csv";
			File file = new File(summary.getParentFile(), name);
			if (file.getParentFile() != null) {
				file.getParentFile().mkdirs();
			}
			boolean header = !file.exists() || file.length() == 0;
			try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
				if (header) {
					writer.println("RunID;Shard;Shards;Approach;Level;HandledProducts;FailedProducts;"
							+ "ElapsedTime(ms);ProcessPeakRSS(kB);ChildPeakRSS(kB);AvailableProcessors;CpuModel;Host;"
							+ "Commit;JavaVersion;MaxHeap(MB);GcTime(ms)"
							+ (extraHeader == null || extraHeader.isEmpty() ? "" : ";" + extraHeader));
				}
				String host;
				try {
					host = java.net.InetAddress.getLocalHost().getHostName();
				} catch (Exception e) {
					host = "unknown";
				}
				writer.println(runID + ";" + shard + ";" + shards + ";" + approach + ";" + level + ";" + handledProducts
						+ ";" + failedProducts + ";" + String.format(Locale.ROOT, "%.3f", elapsedMs()) + ";"
						+ processPeakRssKb() + ";" + childPeakRssKb + ";"
						+ Runtime.getRuntime().availableProcessors() + ";" + cpuModel() + ";" + host + ";"
						+ System.getenv().getOrDefault("GIT_COMMIT", "unknown") + ";"
						+ System.getProperty("java.version") + ";" + (Runtime.getRuntime().maxMemory() / (1024 * 1024))
						+ ";" + gcTimeMs()
						+ (extraRow == null || extraRow.isEmpty() ? "" : ";" + extraRow));
			}
		} catch (IOException e) {
			System.err.println("Could not write the JVM row: " + e.getMessage());
		}
	}

	// ------------------------------------------------------------------
	// Stored suites
	// ------------------------------------------------------------------

	/**
	 * Writer for the suites of one JVM: one compressed file with two kinds of
	 * lines. "V;product;vertex ID;event name" gives the event of every vertex
	 * ID of a product-level model as the generator numbers it (this is not the
	 * number in the vertex name of the DOT file). "S;product;sequence
	 * number;vertex IDs separated by spaces" gives one sequence. The file is
	 * flushed after every product, so a JVM that stops early leaves the
	 * products it finished.
	 */
	public static PrintWriter openSuiteStore(String folder, String fileName) throws IOException {
		File directory = new File(folder);
		directory.mkdirs();
		final PrintWriter writer = new PrintWriter(new OutputStreamWriter(
				new GZIPOutputStream(new FileOutputStream(new File(directory, fileName + ".txt.gz")), true),
				StandardCharsets.UTF_8));
		Runtime.getRuntime().addShutdownHook(new Thread(writer::close));
		return writer;
	}

	public static void storeSuite(PrintWriter store, String productName, ESG productESGFx,
			Set<EventSequence> unfoldedSuite) {
		if (store == null || unfoldedSuite == null) {
			return;
		}
		if (productESGFx != null) {
			for (Vertex vertex : productESGFx.getVertexList()) {
				store.println("V;" + productName + ";" + vertex.getID() + ";"
						+ vertex.getEvent().getName().replace('\n', ' ').replace('\r', ' '));
			}
		}
		int number = 0;
		for (EventSequence sequence : unfoldedSuite) {
			number++;
			StringBuilder line = new StringBuilder("S;").append(productName).append(';').append(number).append(';');
			List<Vertex> events = sequence.getEventSequence();
			for (int i = 0; i < events.size(); i++) {
				if (i > 0) {
					line.append(' ');
				}
				line.append(events.get(i).getID());
			}
			store.println(line);
		}
		store.flush();
		if (store.checkError()) {
			throw new IllegalStateException("The suite store could not be written (disk full?)");
		}
	}

	// ------------------------------------------------------------------
	// One compressed row per product, for drivers without a per-product log
	// ------------------------------------------------------------------

	/**
	 * Opens the per-product record of one JVM of an RQ2 driver. The driver
	 * flushes it every 200 rows, and it is closed when the JVM ends.
	 */
	public static PrintWriter openProductRecord(String folder, String fileName, String header) throws IOException {
		File directory = new File(folder);
		directory.mkdirs();
		final PrintWriter writer = new PrintWriter(new OutputStreamWriter(
				new GZIPOutputStream(new FileOutputStream(new File(directory, fileName + "_products.csv.gz")), true),
				StandardCharsets.UTF_8));
		writer.println(header);
		Runtime.getRuntime().addShutdownHook(new Thread(writer::close));
		return writer;
	}

	/** Names of the features of a list, separated by commas, in the order of selectionBits(). */
	public static String featureNames(List<tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression> features) {
		StringBuilder names = new StringBuilder();
		for (tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression feature : features) {
			if (names.length() > 0) {
				names.append(',');
			}
			names.append(feature.getFeature().getName());
		}
		return names.toString();
	}

	/** The selected concrete features of a product as a string of 0 and 1, in the order of the list. */
	public static String selectionBits(List<tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression> features) {
		StringBuilder bits = new StringBuilder(features.size());
		for (tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression feature : features) {
			bits.append(feature.evaluate() ? '1' : '0');
		}
		return bits.toString();
	}

	/** Time budget of a run in nanoseconds, from TIMEOUT_MINUTES or TIMEOUT_HOURS; no budget if neither is set. */
	public static long timeBudgetNanos() {
		long minutes = Long.parseLong(System.getenv().getOrDefault("TIMEOUT_MINUTES", "0"));
		long hours = Long.parseLong(System.getenv().getOrDefault("TIMEOUT_HOURS", "0"));
		long total = minutes * 60L + hours * 3600L;
		return total > 0 ? total * 1_000_000_000L : Long.MAX_VALUE;
	}
}
