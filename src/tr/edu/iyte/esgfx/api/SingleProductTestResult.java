package tr.edu.iyte.esgfx.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.Vertex;

public final class SingleProductTestResult {

	private final int productId;
	private final Map<String, Boolean> selection;
	private final int coverageLength;
	private final String coverageType;
	private final double coveragePercentage;
	private final List<EventSequence> testSequences;
	private final int sequenceCount;
	private final int totalEventCount;
	private final long generationTimeMs;

	public SingleProductTestResult(int productId, Map<String, Boolean> selection, int coverageLength,
			String coverageType, double coveragePercentage, List<EventSequence> testSequences, int sequenceCount,
			int totalEventCount, long generationTimeMs) {
		this.productId = productId;
		this.selection = selection;
		this.coverageLength = coverageLength;
		this.coverageType = coverageType;
		this.coveragePercentage = coveragePercentage;
		this.testSequences = testSequences;
		this.sequenceCount = sequenceCount;
		this.totalEventCount = totalEventCount;
		this.generationTimeMs = generationTimeMs;
	}

	public int getProductId() {
		return productId;
	}

	public Map<String, Boolean> getSelection() {
		return selection;
	}

	public int getCoverageLength() {
		return coverageLength;
	}

	public String getCoverageType() {
		return coverageType;
	}

	public double getCoveragePercentage() {
		return coveragePercentage;
	}

	public List<EventSequence> getTestSequences() {
		return testSequences;
	}

	public List<List<String>> getTestSequencesAsEventNames() {
		List<List<String>> eventNameSequences = new ArrayList<List<String>>(testSequences.size());
		for (EventSequence es : testSequences) {
			List<String> eventNames = new ArrayList<String>(es.length());
			for (Vertex v : es.getEventSequence()) {
				eventNames.add(v.getEvent().getName().trim().replaceAll(" ", "_"));
			}
			eventNameSequences.add(eventNames);
		}
		return eventNameSequences;
	}

	public int getSequenceCount() {
		return sequenceCount;
	}

	public int getTotalEventCount() {
		return totalEventCount;
	}

	public long getGenerationTimeMs() {
		return generationTimeMs;
	}
}
