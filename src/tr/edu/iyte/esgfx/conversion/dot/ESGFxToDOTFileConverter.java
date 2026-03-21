package tr.edu.iyte.esgfx.conversion.dot;

import java.io.File;

import java.io.FileWriter;

import java.io.IOException;

import java.io.PrintWriter;

import java.util.LinkedHashSet;

import java.util.Set;

import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.conversion.dot.ESGToDOTFileConverter;
import tr.edu.iyte.esg.model.ESG;


public class ESGFxToDOTFileConverter extends ESGToDOTFileConverter {

	
	public static void buildDOTFileFromESGFx(ESG esgfx, String folderPath, String fileName) {

		File file = new File(folderPath, fileName + ".DOT");

		if (!file.getParentFile().exists()) {

			file.getParentFile().mkdirs();

		}

		try (PrintWriter printWriter = new PrintWriter(new FileWriter(file))) {

			printWriter.println("digraph G {\n" + "rankdir = LR");

			Set<String> esgContent = getProperESGContentForDOTFormat(esgfx);

			for (String content : esgContent) {

				printWriter.println(content);

			}

			printWriter.println("}");

		} catch (IOException e) {

			System.err.println("ESGFx to DOT file has reached an error: " + e.getMessage());

			e.printStackTrace();

		}

	}

	public static Set<String> getProperESGContentForDOTFormat(ESG ESG){
		Set<String> vertexSet = new LinkedHashSet<String>();
		Set<String> edgeSet = new LinkedHashSet<String>();
		
		String label = "label = \"" + ESG.getName() + "\";";
		edgeSet.add(label);
		
		String vertexName = "esg"+ ESG.getID() + "_vertex";
		String vertexLabelBeginning = " [label = \"";
		String vertexLabelEnd = "]";
		
		for (Edge edge : ESG.getEdgeList()) {
			
			String source = vertexName + edge.getSource().getID();
			String target = vertexName + edge.getTarget().getID();
			
			// FIX: Replace \n and \r with a space to prevent DOT file corruption.
			String cleanSourceLabel = edge.getSource().getDotLanguageFormat().replaceAll("[\\r\\n]+", " ");
			String sourceNamePrefix = source + vertexLabelBeginning + getProperStringForDotFormat(cleanSourceLabel);
			String sourceName = 
					sourceNamePrefix +
					edge.getSource().getShape() + 
					getVertexColor(edge.getSource()) +
					vertexLabelEnd;

			// FIX: Replace \n and \r with a space to prevent DOT file corruption.
			String cleanTargetLabel = edge.getTarget().getDotLanguageFormat().replaceAll("[\\r\\n]+", " ");
			String targetNamePrefix = target + vertexLabelBeginning + getProperStringForDotFormat(cleanTargetLabel);
			String targetName =
					targetNamePrefix +
					edge.getTarget().getShape() + 
					getVertexColor(edge.getTarget()) +
					vertexLabelEnd;
			
			vertexSet.add(sourceName);
			vertexSet.add(targetName);

			String edgeName = source + " -> " + target + getEdgeColor(edge) + ";";
			edgeSet.add(edgeName);
		
		}
		edgeSet.addAll(vertexSet);
		
		return edgeSet;
	}
	
}