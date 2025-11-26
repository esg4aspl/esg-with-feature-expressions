package tr.edu.iyte.esgfx.testgeneration;

import java.io.FileWriter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.nio.channels.SeekableByteChannel;
import java.nio.ByteBuffer;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.Vertex;

import tr.edu.iyte.esgfx.model.sequenceesgfx.EventSequenceUtilities;

public class TestSuiteFileWriter {

	public static void writeEventSequenceSetAndEventCoverageAnalysisToFile(String filePath,
			Set<EventSequence> eventSequenceSet, double eventCoverage) throws IOException {
		Writer fileWriter = new FileWriter(filePath);
		PrintWriter printWriter = new PrintWriter(fileWriter);
		for (EventSequence es : eventSequenceSet) {
			// System.out.println("ES " + es.toString());
			String eventSequence = "";

			for (int i = 0; i < es.length(); i++) {
				Vertex event = es.getEventSequence().get(i);

				String eventName = event.getEvent().getName().trim().replaceAll(" ", "_");
				;
				if (i == es.length() - 1) {
					eventSequence += eventName;
				} else {
					eventSequence += eventName + ", ";
				}

			}

			printWriter.println(es.length() + " : " + eventSequence);
		}
		printWriter.print("Event coverage is " + Double.toString(eventCoverage) + "%");
		printWriter.close();
	}

	public static void writeSPLModelTestSuiteSummary(String filePath, String SPLName, int numberOfProducts,
			int ESGFx_numberOfVertices, int ESGFx_numberOfEdges, int totalNumberOfVertices, int totalNumberOfEdges,
			int totalNumberOfSequences_L0, int totalNumberOfEvents_L0, double avgCoverageL0,
			int totalNumberOfSequences_L1, int totalNumberOfEvents_L1, double avgCoverageL1,
			int totalNumberOfSequences_L2, int totalNumberOfEvents_L2, double avgCoverageL2,
			int totalNumberOfSequences_L3, int totalNumberOfEvents_L3, double avgCoverageL3,
			int totalNumberOfSequences_L4, int totalNumberOfEvents_L4, double avgCoverageL4) throws IOException {

		Path path = Path.of(filePath);
		ensureParent(path);

		boolean exists = Files.exists(path);
		boolean empty = !exists || Files.size(path) == 0;

		// If file exists and last byte is not newline, append one
		if (!empty && !endsWithNewline(path)) {
			Files.write(path, System.lineSeparator().getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND,
					StandardOpenOption.CREATE);
		}

		double avgNumberOfVertices = (totalNumberOfVertices / (double) numberOfProducts);
		double avgNumberOfEdges = (totalNumberOfEdges / (double) numberOfProducts);
		double avgNumberOfSequences_L0 = (totalNumberOfSequences_L0 / (double) numberOfProducts);
		double avgNumberOfEvents_L0 = (totalNumberOfEvents_L0 / (double) numberOfProducts);
		double avgNumberOfSequences_L1 = (totalNumberOfSequences_L1 / (double) numberOfProducts);
		double avgNumberOfEvents_L1 = (totalNumberOfEvents_L1 / (double) numberOfProducts);
		double avgNumberOfSequences_L2 = (totalNumberOfSequences_L2 / (double) numberOfProducts);
		double avgNumberOfEvents_L2 = (totalNumberOfEvents_L2 / (double) numberOfProducts);
		double avgNumberOfSequences_L3 = (totalNumberOfSequences_L3 / (double) numberOfProducts);
		double avgNumberOfEvents_L3 = (totalNumberOfEvents_L3 / (double) numberOfProducts);
		double avgNumberOfSequences_L4 = (totalNumberOfSequences_L4 / (double) numberOfProducts);
		double avgNumberOfEvents_L4 = (totalNumberOfEvents_L4 / (double) numberOfProducts);

		try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, true))) {
			if (empty) {
				pw.println("SPL;Number of Products;ESG-Fx Number of Vertices;ESG-Fx Number of Edges; "
						+ "Avg Number of Vertices; Avg Number of Edges;"
						
						+ "Total Number of Sequences: randomwalk;" 
						+ "Total Number of Events: randomwalk;"
						+ "Avg Number of Sequences: randomwalk;"
						+ "Avg Number of Events: randomwalk;"
						+ "Average randomwalkcoverage;"
						
						+ "Total Number of Sequences: eventcoverage;" 
						+ "Total Number of Events: eventcoverage;"
						+ "Avg Number of Sequences: eventcoverage;"
						+ "Avg Number of Events: eventcoverage;"
						+ "Average eventcoverage;"
						
						+ "Total Number of Sequences: eventcouplecoverage;" 
						+ "Total Number of Events: eventcouplecoverage;"
						+ "Avg Number of Sequences: eventcouplecoverage;"
						+ "Avg Number of Events: eventcouplecoverage;"
						+ "Average eventcouplecoverage;"
						
						+ "Total Number of Sequences: eventtriplecoverage;"
						+ "Total Number of Events: eventtriplecoverage;"
						+ "Avg Number of Sequences: eventtriplecoverage;"
						+ "Avg Number of Events: eventtriplecoverage;"
						+ "Average eventtriplecoverage;"
						
						+ "Total Number of Sequences: eventquadruplecoverage;"
						+ "Total Number of Events: eventquadruplecoverage;"
						+ "Avg Number of Sequences: eventquadruplecoverage;"
						+ "Avg Number of Events: eventquadruplecoverage;"
						+ "Average eventquadruplecoverage");
			}
			pw.println(escapeCsv(SPLName) + ";" + numberOfProducts + ";" + ESGFx_numberOfVertices + ";"
					+ ESGFx_numberOfEdges + ";" + formatDouble_2(avgNumberOfVertices) + ";" + formatDouble_2(avgNumberOfEdges) + ";" 
					
					+ totalNumberOfSequences_L0 + ";" 
					+ totalNumberOfEvents_L0 + ";" 
					+ formatDouble_2(avgNumberOfSequences_L0) + ";" 
					+ formatDouble_2(avgNumberOfEvents_L0) + ";"
					+ formatDouble_2(avgCoverageL0) + ";"
					
					+ totalNumberOfSequences_L1 + ";" 
					+ totalNumberOfEvents_L1 + ";" 
					+ formatDouble_2(avgNumberOfSequences_L1) + ";" 
					+ formatDouble_2(avgNumberOfEvents_L1) + ";"
					+ formatDouble_2(avgCoverageL1) + ";"
					
					+ totalNumberOfSequences_L2 + ";" 
					+ totalNumberOfEvents_L2 + ";" 
					+ formatDouble_2(avgNumberOfSequences_L2) + ";" 
					+ formatDouble_2(avgNumberOfEvents_L2) + ";"
					+ formatDouble_2(avgCoverageL2) + ";"
					
					+ totalNumberOfSequences_L3 + ";" 
					+ totalNumberOfEvents_L3 + ";"
					+ formatDouble_2(avgNumberOfSequences_L3) + ";" 
					+ formatDouble_2(avgNumberOfEvents_L3) + ";"
					+ formatDouble_2(avgCoverageL3) + ";"
					
					+ totalNumberOfSequences_L4 + ";" 
					+ totalNumberOfEvents_L4 + ";"
					+ formatDouble_2(avgNumberOfSequences_L4) + ";" 
					+ formatDouble_2(avgNumberOfEvents_L4) + ";" 
					+ formatDouble_2(avgCoverageL4));
		}
	}

	public static void writeEventSequenceSetAndCoverageAnalysisToFilePerProduct(String filePath,
			String productConfiguration, int numberOfVertices, int numberOfEdges, int totalNumberOfSequences,
			int totalNumberOfEvents, Set<EventSequence> eventSequenceSet, int coverageLength, String coverageType,
			double edgeCoverage) throws IOException {
		FileWriter fileWriter = new FileWriter(filePath, true); // set second parameter to true for append mode
		PrintWriter printWriter = new PrintWriter(fileWriter);
		printWriter.println(productConfiguration /* + " : " + numberOfFeatures + " features" */);

		printWriter.println("Number of vertices: " + numberOfVertices);
		printWriter.println("Number of edges: " + numberOfEdges);

		printWriter.println("Number of sequences: " + totalNumberOfSequences);
		printWriter.println("Number of events: " + totalNumberOfEvents);
		printWriter.println("Event Sequences: ");

		for (EventSequence es : eventSequenceSet) {
			String eventSequence = "";

			for (int i = 0; i < es.length(); i++) {
				Vertex event = es.getEventSequence().get(i);

				String eventName = event.getEvent().getName().trim().replaceAll(" ", "_");
//				System.out.println("TO FILE" + eventName);
				if (i == es.length() - 1) {
					eventSequence += eventName;
				} else {
					eventSequence += eventName + ", ";
				}
			}

			printWriter.println(es.length() + " : " + eventSequence);
		}
		printWriter.println(coverageType + " " + Double.toString(edgeCoverage) + "%");
		printWriter.println(
				"---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
		printWriter.print("\n");
		printWriter.close();
	}

	/**
	 * This method writes the test sequences without modifying transformed vertices
	 * 
	 * @param filePath
	 * @param productConfiguration
	 * @param numberOfFeatures
	 * @param eventSequenceSet
	 * @param coverageType
	 * @param edgeCoverage
	 * @throws IOException
	 */
	public static void writeEventSequenceSetAndCoverageAnalysisToFile(String filePath, String productConfiguration,
			int numberOfFeatures, Set<EventSequence> eventSequenceSet, String coverageType, double edgeCoverage)
			throws IOException {
		FileWriter fileWriter = new FileWriter(filePath, true); // set second parameter to true for append mode
		PrintWriter printWriter = new PrintWriter(fileWriter);
		printWriter.println(productConfiguration /* + " : " + numberOfFeatures + " features" */);

		for (EventSequence es : eventSequenceSet) {
			String eventSequence = "";

			for (int i = 0; i < es.length(); i++) {
				Vertex event = es.getEventSequence().get(i);

				String eventName = event.getEvent().getName().trim().replaceAll(" ", "_");
//				System.out.println("TO FILE" + eventName);
				if (i == es.length() - 1) {
					eventSequence += eventName;
				} else {
					eventSequence += eventName + ", ";
				}
			}

			printWriter.println(es.length() + " : " + eventSequence);
		}
		printWriter.println(coverageType + " " + Double.toString(edgeCoverage) + "%");
		printWriter.print("\n");
		printWriter.close();
	}

	// --- NEW: Append global TestSuite size summary to CSV ---
	public static void appendTestSuiteSizeSummary(String filePath, String splName, String coverageType,
			int numberOfProducts, int totalNumberOfSequences, int totalNumberOfEvents) throws IOException {
		Path path = Path.of(filePath);
		ensureParent(path);

		boolean exists = Files.exists(path);
		boolean empty = !exists || Files.size(path) == 0;

		// If file exists and last byte is not newline, append one
		if (!empty && !endsWithNewline(path)) {
			Files.write(path, System.lineSeparator().getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND,
					StandardOpenOption.CREATE);
		}

		try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, true))) {
			if (empty) {
				pw.println(
						"SPL;Coverage Type;Number of Products,Total Number of Sequences,Total Number of Events,Avg Sequences per Product,Avg Events per Product");
			}
			pw.println(escapeCsv(splName) + ";" + escapeCsv(coverageType) + ";" + numberOfProducts + ";"
					+ totalNumberOfSequences + ";" + totalNumberOfEvents + ";"
					+ formatDouble(totalNumberOfSequences / numberOfProducts) + ";"
					+ formatDouble(totalNumberOfEvents / numberOfProducts));
		}
	}

	public static void appendModelSizeSummary(String filePath, String splName, String coverageType,
			int numberOfProducts, int totalNumberOfSequences, int totalNumberOfEvents) throws IOException {
		Path path = Path.of(filePath);
		ensureParent(path);

		boolean exists = Files.exists(path);
		boolean empty = !exists || Files.size(path) == 0;

		// If file exists and last byte is not newline, append one
		if (!empty && !endsWithNewline(path)) {
			Files.write(path, System.lineSeparator().getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND,
					StandardOpenOption.CREATE);
		}

		try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, true))) {
			if (empty) {
				pw.println(
						"SPL,Coverage Type,Number of Products,Total Number of Sequences,Total Number of Events,Avg Sequences per Product,Avg Events per Product");
			}
			pw.println(escapeCsv(splName) + ";" + escapeCsv(coverageType) + ";" + numberOfProducts + ";"
					+ totalNumberOfSequences + ";" + totalNumberOfEvents + ";"
					+ formatDouble(totalNumberOfSequences / numberOfProducts) + ";"
					+ formatDouble(totalNumberOfEvents / numberOfProducts));
		}
	}

	private static String formatDouble(double d) {
		// Simple formatting without locale commas
		return String.format(java.util.Locale.ROOT, "%.6f", d);
	}

	private static String formatDouble_2(double d) {
		// Simple formatting without locale commas
		return String.format(Locale.forLanguageTag("tr-TR"), "%.2f", d);
	}

	private static String escapeCsv(String s) {
		if (s == null)
			return "";
		String t = s.replace("\"", "\"\"");
		if (t.contains(";") || t.contains("\n") || t.contains("\r")) {
			return "\"" + t + "\"";
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

	private static void ensureParent(Path path) throws IOException {
		File parent = path.toFile().getParentFile();
		if (parent != null && !parent.exists()) {
			if (!parent.mkdirs() && !parent.exists()) {
				throw new IOException("Failed to create parent directory: " + parent);
			}
		}
	}

	// existing methods (writeEventSequenceSetAndEventCoverageAnalysisToFile,
	// writeEventSequenceSetAndCoverageAnalysisToFile,
	// removeRepetitionsFromEventSequence) stay as-is
}
