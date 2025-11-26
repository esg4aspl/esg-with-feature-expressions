package tr.edu.iyte.esgfx.mutationtesting.resultutils;

import java.nio.channels.SeekableByteChannel;
import java.nio.ByteBuffer;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;

public class FaultDetectionResultRecorder {

	private static final Object LOCK = new Object();
	private static final String NL = System.lineSeparator();

	// Headers
	private static final String[] detailedFaultDetectionResults = { "Product ID", "Features", "Coverage Type",
			"Mutation Operator", "Mutation Element", "Mutant ID", "Is Mutant Valid?", "Is Fault Detected?" };

	private static final String[] detailedFaultDetectionResultsL234 = { "Product ID", "Features", "Mutation Operator",
			"Mutation Element", "Mutant ID", "Is Mutant Valid?", "Is Fault Detected L=2?", "Is Fault Detected L=3?",
			"Is Fault Detected L=4?" };

	private static final String[] oldSummaryColumns = { "Coverage Type", "Mutation Operator", "Product ID",
			"Number of Valid Mutants", "Number of Invalid Mutants", "Number of Detected Valid Mutants",
			"Number of Detected Invalid Mutants", "Total Number of Mutants", "Total Number of Detected Faults" };

	// New per-product summary with per-L percentages and total percentage
	private static final String[] newSummaryColumnsL234_WITH_PCT = { "Coverage Type", "Mutation Operator", "Product ID",
			"Number of Valid Mutants", "Number of Invalid Mutants", "Detected Valid Mutants L=2",
			"Detected Invalid Mutants L=2", "FaultDetectionPercentange L=2", "Detected Valid Mutants L=3",
			"Detected Invalid Mutants L=3", "FaultDetectionPercentange L=3", "Detected Valid Mutants L=4",
			"Detected Invalid Mutants L=4", "FaultDetectionPercentange L=4", "Total Number of Mutants",
			"Total Number of Detected Faults", "Total FaultDetectionPercentange" };

	// SPL summary header
	private static final String[] splSummaryColumns = { "SPL", "Operator", "Total Number of Valid Mutants",
			"Total Number of Invalid Mutants", "Detected Valid Mutants L=2", "Detected Invalid Mutants L=2",
			"FaultDetectionPercentange L=2", "Detected Valid Mutants L=3", "Detected Invalid Mutants L=3",
			"FaultDetectionPercentange L=3", "Detected Valid Mutants L=4", "Detected Invalid Mutants L=4",
			"FaultDetectionPercentange L=4", "Total Number of Mutants", "Total Number of Detected Faults",
			"Total FaultDetectionPercentange" };

	// ---------- Existing public writers (kept) ----------

	public static void writeDetailedFaultDetectionResultL234(String fileNameBase, int productId, String features,
			String mutationOperator, String mutationElement, int mutantId, boolean isMutantValid, String isDetectedL2,
			String isDetectedL3, String isDetectedL4) {

		Path path = toCsvPath(fileNameBase);
		String[] row = { String.valueOf(productId), features, mutationOperator, mutationElement,
				String.valueOf(mutantId), String.valueOf(isMutantValid), isDetectedL2, isDetectedL3, isDetectedL4 };
		writeCsvRow(path, detailedFaultDetectionResultsL234, row);
	}

	// Old summary kept for backward compatibility
	public static void writeFaultDetectionResultsForSPL(String fileNameWithCsvExt, String coverageType,
			String mutationOperator, int productId, int numValidMutants, int numInvalidMutants,
			int numDetectedValidMutants, int numDetectedInvalidMutants, int totalNumMutants,
			int totalNumDetectedFaults) {

		Path path = Path.of(fileNameWithCsvExt);
		String[] row = { coverageType, mutationOperator, String.valueOf(productId), String.valueOf(numValidMutants),
				String.valueOf(numInvalidMutants), String.valueOf(numDetectedValidMutants),
				String.valueOf(numDetectedInvalidMutants), String.valueOf(totalNumMutants),
				String.valueOf(totalNumDetectedFaults) };
		writeCsvRow(path, oldSummaryColumns, row);
	}

	// Per-product summary with per-L counts + per-L percentages for each operator
	public static void writeFaultDetectionResultsForPerProductSPL(String filePath, String mutationOperator,
			int productId, int numValidMutants, int numInvalidMutants, int detValidL2, int detInvalidL2, double perL2,
			int detValidL3, int detInvalidL3, double perL3, int detValidL4, int detInvalidL4, double perL4)
			throws IOException {

		Path path = Path.of(filePath);
		ensureParent(path);

		boolean exists = Files.exists(path);
		boolean empty = !exists || Files.size(path) == 0;

		try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, true))) {
			if (empty) {
				pw.println("Mutation Operator;" + "Product ID;" + "Number of Valid Mutants;"
						+ "Number of Invalid Mutants;" + "Detected Valid Mutants L=2; "
						+ "Detected Invalid Mutants L=2;" + "Fault Detection Percentange L=2;"
						+ "Detected Valid Mutants L=3; " + "Detected Invalid Mutants L=3;"
						+ "Fault Detection Percentange L=3;" + "Detected Valid Mutants L=4; "
						+ "Detected Invalid Mutants L=4;" + "Fault Detection Percentange L=4");
			}
			pw.println(mutationOperator + ";" + productId + ";" + numValidMutants + ";" + numInvalidMutants + ";"
					+ detValidL2 + ";" + detInvalidL2 + ";" + formatDouble_2(perL2) + ";" 
					+ detValidL3 + ";" + detInvalidL3 + ";" + formatDouble_2(perL3) + ";" 
					+ detValidL4 + ";" + detInvalidL4 + ";" + formatDouble_2(perL4));
		}
	}

	// Per-SPL summary with per-L counts + per-L percentages for each operator
	public static void writeFaultDetectionResultsForSPL(String filePath, String SPLName, String mutationOperator,
			int numMutants, int detMutL0, double perL0, double killedPerSecondL0, int detMutL1, double perL1, double killedPerSecondL1,
			int detMutL2, double perL2, double killedPerSecondL2, int detMutL3, double perL3,
			double killedPerSecondL3, int detMutL4, double perL4, double killedPerSecondL4) throws IOException {

		Path path = Path.of(filePath);
		ensureParent(path);

		boolean exists = Files.exists(path);
		boolean empty = !exists || Files.size(path) == 0;

		try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, true))) {
			if (empty) {
				pw.println("SPL;" + "Operator;" + "Number of Mutants;"
			
							+ "Number of Detected  Mutants RandomWalk; "
							+ "Fault Detection Percentange RandomWalk;" 
							+ "Detected Mutants Per Second RandomWalk;"
							
							+ "Number of Detected  Mutants L=1; "
							+ "Fault Detection Percentange L=1;" 
							+ "Detected Mutants Per Second L=1;"
							
							+ "Number of Detected  Mutants L=2; "
							+ "Fault Detection Percentange L=2;" 
							+ "Detected Mutants Per Second L=2;"
							
							+ "Number of Detected  Mutants L=3; " 
							+ "Fault Detection Percentange L=3;"
							+ "Detected Mutants Per Second L=3;" 
							
							+ "Number of Detected  Mutants L=4; "
							+ "Fault Detection Percentange L=4;" 
							+ "Detected Mutants Per Second L=4");
			}
			pw.println(SPLName + ";" + mutationOperator + ";" + numMutants + ";" 
					+ detMutL0 + ";" + formatDouble_2(perL0) + ";" + formatDouble_2(killedPerSecondL0) + ";"
					+ detMutL1 + ";" + formatDouble_2(perL1) + ";" + formatDouble_2(killedPerSecondL1) + ";"
					+ detMutL2 + ";" + formatDouble_2(perL2) + ";" + formatDouble_2(killedPerSecondL2) + ";"
					+ detMutL3 + ";" + formatDouble_2(perL3) + ";" + formatDouble_2(killedPerSecondL3) + ";"
					+ detMutL4 + ";" + formatDouble_2(perL4)+ ";" +  formatDouble_2(killedPerSecondL4));
		}
	}

	private static String formatDouble_2(double d) {
		// Simple formatting without locale commas
		return String.format(Locale.forLanguageTag("tr-TR"), "%.2f", d);
	}

	// ---------- SPL summary writers ----------

	public static void writeSPLSummaryFaultDetectionLine(String fileNameWithCsvExt, String splName, String operatorName,
			int totalNumValid, int totalNumInvalid, int detValidL2, int detInvalidL2, int detValidL3, int detInvalidL3,
			int detValidL4, int detInvalidL4, int totalNumMutants, int totalNumDetectedAny) {

		Path path = Path.of(fileNameWithCsvExt);
		ensureParent(path);

		String[] row = { splName, operatorName, String.valueOf(totalNumValid), String.valueOf(totalNumInvalid),

				String.valueOf(detValidL2), String.valueOf(detInvalidL2),
				formatPercentComma(detValidL2 + detInvalidL2, totalNumMutants), String.valueOf(detValidL3),
				String.valueOf(detInvalidL3), formatPercentComma(detValidL3 + detInvalidL3, totalNumMutants),
				String.valueOf(detValidL4), String.valueOf(detInvalidL4),
				formatPercentComma(detValidL4 + detInvalidL4, totalNumMutants),

				String.valueOf(totalNumMutants), String.valueOf(totalNumDetectedAny),
				formatPercentComma(totalNumDetectedAny, totalNumMutants) };

		writeCsvRow(path, splSummaryColumns, row);
	}

	/**
	 * Recompute and upsert the ALL_OP line for the given SPL by summing all
	 * operator lines.
	 */
	public static void upsertAllOperatorsSummary(String fileNameWithCsvExt, String splName) {
		Path path = Path.of(fileNameWithCsvExt);
		ensureParent(path);

		synchronized (LOCK) {
			try {
				List<String> lines = Files.exists(path) ? Files.readAllLines(path, StandardCharsets.UTF_8)
						: new ArrayList<>();

				Map<String, Integer> idx = headerIndex(lines, splSummaryColumns);
				if (idx == null) {
					// Write header if empty and retry
					writeCsvRow(path, splSummaryColumns, new String[] { splName, "ALL_OP", "0", "0", "0", "0", "0,00%",
							"0", "0", "0,00%", "0", "0", "0,00%", "0", "0", "0,00%" });
					lines = Files.readAllLines(path, StandardCharsets.UTF_8);
					idx = headerIndex(lines, splSummaryColumns);
				}

				// Aggregate across operators for this SPL (exclude ALL_OP)
				int aggValid = 0, aggInvalid = 0;
				int detValidL2 = 0, detInvalidL2 = 0;
				int detValidL3 = 0, detInvalidL3 = 0;
				int detValidL4 = 0, detInvalidL4 = 0;
				int totalMutants = 0, totalDetectedAny = 0;

				int headerRow = 0;
				for (int i = 1; i < lines.size(); i++) {
					String ln = lines.get(i).trim();
					if (ln.isEmpty())
						continue;
					String[] t = splitSemicolon(ln);
					String spl = unquote(t[0]);
					String op = unquote(t[1]);
					if (!splName.equals(spl))
						continue;
					if ("ALL_OP".equals(op))
						continue;

					aggValid += parseIntSafe(t[idx.get("Total Number of Valid Mutants")]);
					aggInvalid += parseIntSafe(t[idx.get("Total Number of Invalid Mutants")]);
					detValidL2 += parseIntSafe(t[idx.get("Detected Valid Mutants L=2")]);
					detInvalidL2 += parseIntSafe(t[idx.get("Detected Invalid Mutants L=2")]);
					detValidL3 += parseIntSafe(t[idx.get("Detected Valid Mutants L=3")]);
					detInvalidL3 += parseIntSafe(t[idx.get("Detected Invalid Mutants L=3")]);
					detValidL4 += parseIntSafe(t[idx.get("Detected Valid Mutants L=4")]);
					detInvalidL4 += parseIntSafe(t[idx.get("Detected Invalid Mutants L=4")]);
					totalMutants += parseIntSafe(t[idx.get("Total Number of Mutants")]);
					totalDetectedAny += parseIntSafe(t[idx.get("Total Number of Detected Faults")]);
				}

				// Build ALL_OP row
				String[] allOpRow = { splName, "ALL_OP", String.valueOf(aggValid), String.valueOf(aggInvalid),

						String.valueOf(detValidL2), String.valueOf(detInvalidL2),
						formatPercentComma(detValidL2 + detInvalidL2, totalMutants), String.valueOf(detValidL3),
						String.valueOf(detInvalidL3), formatPercentComma(detValidL3 + detInvalidL3, totalMutants),
						String.valueOf(detValidL4), String.valueOf(detInvalidL4),
						formatPercentComma(detValidL4 + detInvalidL4, totalMutants),

						String.valueOf(totalMutants), String.valueOf(totalDetectedAny),
						formatPercentComma(totalDetectedAny, totalMutants) };

				// Upsert: if ALL_OP exists for this SPL, replace. Else append.
				int allOpIndex = -1;
				for (int i = 1; i < lines.size(); i++) {
					String[] t = splitSemicolon(lines.get(i).trim());
					if (t.length == 0)
						continue;
					if (splName.equals(unquote(t[0])) && "ALL_OP".equals(unquote(t[1]))) {
						allOpIndex = i;
						break;
					}
				}

				if (allOpIndex >= 0) {
					lines.set(allOpIndex, joinCsv(allOpRow));
					Files.write(path, String.join(NL, lines).concat(NL).getBytes(StandardCharsets.UTF_8),
							StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
				} else {
					writeCsvRow(path, splSummaryColumns, allOpRow);
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Allow MutantGenerator to extract SPL name from results filename, e.g.
	// "eM_faultDetectionResultsForSPL.csv" -> "eM"
	public static String deriveSPLNameFromResultsFile(String fileName) {
		if (fileName == null || fileName.isEmpty())
			return "UNKNOWN_SPL";
		String base = fileName;
		int dot = base.lastIndexOf('.');
		if (dot > 0)
			base = base.substring(0, dot);
		int us = base.indexOf('_');
		if (us > 0)
			return base.substring(0, us);
		return base;
	}

	// ---------- CSV core ----------

	private static Path toCsvPath(String fileNameBase) {
		Path p = Path.of(fileNameBase + (fileNameBase.endsWith(".csv") ? "" : ".csv"));
		ensureParent(p);
		return p;
	}

	private static void writeCsvRow(Path path, String[] header, String[] values) {
		ensureParent(path);
		synchronized (LOCK) {
			try {
				boolean exists = Files.exists(path);
				boolean empty = !exists || Files.size(path) == 0;

				if (!empty && !endsWithNewline(path)) {
					Files.write(path, NL.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
							StandardOpenOption.APPEND);
				}

				boolean writeHeader = empty;
				try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
						StandardOpenOption.APPEND)) {

					if (writeHeader) {
						writer.append(joinCsv(header)).append(NL);
					}
					writer.append(joinCsv(values)).append(NL);
					writer.flush();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void migrateSummaryIfNeeded(Path path) {
		synchronized (LOCK) {
			try {
				if (!Files.exists(path) || Files.size(path) == 0)
					return;

				List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
				if (lines.isEmpty())
					return;

				String header = lines.get(0).trim();
				if (header.equals(joinCsv(oldSummaryColumns))) {
					List<String> out = new ArrayList<>();
					out.add(joinCsv(newSummaryColumnsL234_WITH_PCT));
					for (int i = 1; i < lines.size(); i++) {
						String line = lines.get(i).trim();
						if (line.isEmpty())
							continue;
						String[] t = splitSemicolon(line); // legacy might be semicolon already
						// Old: 9 columns
						String coverageType = unquote(safeGet(t, 0));
						String mutationOperator = unquote(safeGet(t, 1));
						String productId = unquote(safeGet(t, 2));
						String numValid = unquote(safeGet(t, 3));
						String numInvalid = unquote(safeGet(t, 4));
						String totalMutants = unquote(safeGet(t, 7));
						String totalDetected = unquote(safeGet(t, 8));

						String[] row = { coverageType, mutationOperator, productId, numValid, numInvalid, "0", "0",
								"0,00%", "0", "0", "0,00%", "0", "0", "0,00%", totalMutants, totalDetected,
								formatPercentComma(parseIntSafe(totalDetected), parseIntSafe(totalMutants)) };
						out.add(joinCsv(row));
					}
					Files.write(path, String.join(NL, out).concat(NL).getBytes(StandardCharsets.UTF_8),
							StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void ensureParent(Path path) {
		File parent = path.toFile().getParentFile();
		if (parent != null && !parent.exists())
			parent.mkdirs();
	}

	// Always semicolon-separated
	private static String joinCsv(String[] cells) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < cells.length; i++) {
			if (i > 0)
				sb.append(';');
			sb.append(csvEscape(cells[i]));
		}
		return sb.toString();
	}

	private static String csvEscape(String s) {
		if (s == null)
			return "";
		if (s.matches("[0-9.,]+")) {
			return s; // keep numeric-like tokens as-is to preserve comma decimals
		}
		String t = s.replace("\r", " ").replace("\n", " ").replace("\"", "\"\"");
		return "\"" + t + "\"";
	}

	private static String unquote(String s) {
		if (s == null)
			return "";
		String t = s.trim();
		if (t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2) {
			t = t.substring(1, t.length() - 1).replace("\"\"", "\"");
		}
		return t;
	}

	private static boolean endsWithNewline(Path path) {
		try {
			long size = Files.size(path);
			if (size == 0)
				return true;
			try (SeekableByteChannel ch = Files.newByteChannel(path, StandardOpenOption.READ)) {
				ch.position(size - 1);
				ByteBuffer buf = ByteBuffer.allocate(1);
				int n = ch.read(buf);
				if (n != 1)
					return true;
				byte last = buf.array()[0];
				return last == (byte) '\n' || last == (byte) '\r';
			}
		} catch (IOException e) {
			return true;
		}
	}

	// ---------- Helpers ----------

	private static boolean isMutantDetected(boolean isMutantValid, boolean isFaultDetected) {
		return isFaultDetected;
	}

	private static int parseIntSafe(String s) {
		try {
			return Integer.parseInt(unquote(s));
		} catch (Exception e) {
			return 0;
		}
	}

	private static String safeGet(String[] arr, int i) {
		return i < arr.length ? arr[i] : "";
	}

	private static String[] splitSemicolon(String line) {
		// Very simple split for our generated CSV
		return line.split(";", -1);
	}

	// Percent format with comma decimals and trailing %
	private static final ThreadLocal<DecimalFormat> PCT_FMT_COMMA = ThreadLocal.withInitial(() -> {
		DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.ROOT);
		sym.setDecimalSeparator(',');
		DecimalFormat df = new DecimalFormat("0.00'%'");
		df.setDecimalFormatSymbols(sym);
		df.setGroupingUsed(false);
		return df;
	});

	private static String formatPercentComma(int detected, int total) {
		if (total <= 0)
			return "0,00%";
		double p = (detected * 100.0) / (double) total;
		return PCT_FMT_COMMA.get().format(p);
	}

	// Build column indices from header for robust parsing
	private static Map<String, Integer> headerIndex(List<String> lines, String[] expectedHeader) {
		if (lines == null || lines.isEmpty())
			return null;
		String head = lines.get(0).trim();
		String[] cols = splitSemicolon(head);
		Map<String, Integer> idx = new LinkedHashMap<>();
		for (int i = 0; i < cols.length; i++) {
			idx.put(unquote(cols[i]), i);
		}
		// Best effort; do not strictly validate equality
		return idx;
	}
}
