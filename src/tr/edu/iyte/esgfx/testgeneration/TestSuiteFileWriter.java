package tr.edu.iyte.esgfx.testgeneration;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import java.util.Set;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.Vertex;

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

	public static void writeEventSequenceSetAndEdgeCoverageAnalysisToFile(String filePath, String productConfiguration,
			int numberOfFeatures, Set<EventSequence> eventSequenceSet, double edgeCoverage) throws IOException {
		FileWriter fileWriter = new FileWriter(filePath, true); // set second parameter to true for append mode
		PrintWriter printWriter = new PrintWriter(fileWriter);
		printWriter.println(productConfiguration /*+ " : " + numberOfFeatures + " features"*/);
		for (EventSequence es : eventSequenceSet) {
			String eventSequence = "";

			for (int i = 0; i < es.length(); i++) {
				Vertex event = es.getEventSequence().get(i);

				String eventName = event.getEvent().getName().trim().replaceAll(" ", "_");
				if (i == es.length() - 1) {
					eventSequence += eventName;
				} else {
					eventSequence += eventName + ", ";
				}
			}

			printWriter.println(es.length() + " : " + eventSequence);
		}
		printWriter.println("Edge coverage is " + Double.toString(edgeCoverage) + "%");
		printWriter.print("\n");
		printWriter.close();
	}

}
