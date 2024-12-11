package tr.edu.iyte.esgfx.testgeneration.edgecoverage;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.util.Set;
import java.util.Map.Entry;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

public class EdgeCoverageAnalyser {
	
	public  void esgEventSequenceSetPrinter(Set<EventSequence> composedSequences) {
		for (EventSequence es : composedSequences) {
			System.out.println(/* es.length() + " - " + */es);
		}
		System.out.println();
	}

	public double analyseEdgeCoverage(ESG ESGFx, Set<EventSequence> CESsOfESGFx,
			Map<String, FeatureExpression> featureExpressionMap) {

		List<String> mustCoveredEdges = detectMustCoveredEdges(ESGFx);
//		System.out.println("mustCoveredEdges " + mustCoveredEdges);
		
		List<String> coveredEdges = detectCoveredEdges(CESsOfESGFx);
//		System.out.println("coveredEdges " + coveredEdges);
		
		Map<String, Integer> edgeCoverageMap = edgeCoverageMap(mustCoveredEdges, coveredEdges);
		List<String> uncoveredEdges = detectUncoveredEdges(edgeCoverageMap);
//		System.out.println("uncoveredEdges " + uncoveredEdges);
		
		double coverage = percentageOfCoverage(mustCoveredEdges, uncoveredEdges);
//		System.out.println("coverage " + coverage);
		
		return coverage;

	}
	
	public static List<String> detectUncoveredEdges(Map<String, Integer> edgeCoverageMap) {

		List<String> uncoveredEdgeList = new LinkedList<String>();
		for (Entry<String, Integer> entry : edgeCoverageMap.entrySet()) {

			if (entry.getValue() == 0) {
				if (!entry.getKey().equals("Number of uncovered edges "))
					uncoveredEdgeList.add(entry.getKey());
			}
		}

		return uncoveredEdgeList;
	}
		
	private static List<String> detectMustCoveredEdges(ESG ESGFx) {
		List<String> edgeList = new LinkedList<String>();

		for (Edge edge : ESGFx.getRealEdgeList()) {
			Vertex source = edge.getSource();
			Vertex target = edge.getTarget();
				
			if(!source.isPseudoEndVertex() && !target.isPseudoStartVertex()) {
//				System.out.print("source " + source + " ");System.out.println("target " + target);
				
				VertexRefinedByFeatureExpression sourceVertex = (VertexRefinedByFeatureExpression) source;
				VertexRefinedByFeatureExpression targetVertex = (VertexRefinedByFeatureExpression) target;
				
				if (sourceVertex.getFeatureExpression().evaluate() && targetVertex.getFeatureExpression().evaluate()) {
					String edgeName = edge.getSource().toString() + ", " + edge.getTarget().toString();
					edgeList.add(edgeName);
				}
			}
                
		}
		return edgeList;
	}

	private List<String> detectCoveredEdges(Set<EventSequence> CESsOfESGFx) {
		
		List<String> lineList = new LinkedList<String>();
		Iterator<EventSequence> cesSetIterator = CESsOfESGFx.iterator();
		String line;
		while (cesSetIterator.hasNext()) {
			line = cesSetIterator.next().toString();
			lineList.add(line);
		}

		return lineList;
	}
	
	private static Map<String, Integer> edgeCoverageMap(List<String> edgeList, List<String> lineList) {
		Map<String, Integer> edgeCoverageMap = new LinkedHashMap<String, Integer>();

		int zeroCounter = 0;

		for (String edge : edgeList) {
			int counter = 0;
			//System.out.println("edge " + edge);
			for (String line : lineList) {
				//System.out.println("line " + line);
				if (line.contains(edge)) {
					counter += count(line, edge);
					//System.out.println("counter " + counter);
				}
			}
			if (counter == 0) {
				zeroCounter++;
			}

			edgeCoverageMap.put(edge, counter);
		}
		edgeCoverageMap.put("Number of uncovered edges ", zeroCounter);

		return edgeCoverageMap;
	}
	
	private static int count(String text, String find) {
		int index = 0, count = 0, length = find.length();
		while ((index = text.indexOf(find, index)) != -1) {
			index += length;
			count++;
		}
		return count;
	}

	public static double percentageOfCoverage(List<String> mustCoveredEdgeList, List<String> uncoveredEdgeList) {

		double coverage = ((double) uncoveredEdgeList.size()) / ((double) mustCoveredEdgeList.size()) * 100.0;

		if (uncoveredEdgeList.size() == 0) {
			return 100.0;
		} else {
			// System.out.printf("Coverage %.2f %s\n", 100.0 - coverage, "%");
			return 100.0 - coverage;
		}

	}

}
