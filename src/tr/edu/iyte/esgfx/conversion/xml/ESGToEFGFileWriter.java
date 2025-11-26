package tr.edu.iyte.esgfx.conversion.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Event;
import tr.edu.iyte.esg.model.Vertex;

/**
 * Converts an ESG (Event Sequence Graph) object into a GUITAR EFG
 * (Event Flow Graph) file.
 * <p>
 * This specific implementation generates an EFG format tailored to the
 * expectations of the SequenceLengthCoverage plugin, which appears to
 * require the EventGraph matrix and looks for '2' (REACHING_EDGE)
 * instead of '1' (FOLLOW_EDGE) to identify edges.
 * <p>
 * This class ensures:
 * 1. Pseudo-events ('[' and ']') are filtered out.
 * 2. The <Events> list is generated, marking initial events based on
 * the ESG's entryVertexSet.
 * 3. An <EventGraph> matrix is generated, using '2' for edges.
 * 4. The <FollowsRelations> block is *omitted* to avoid conflicts
 * with the plugin's matrix-reading logic.
 */
public class ESGToEFGFileWriter {

    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
    
    // --- NOTE ---
    // We are using the standard action and type, but you can change these
    // if your specific ESG model provides more detail.
    private static final String DEFAULT_ACTION = "edu.umd.cs.guitar.event.WebToggleCheckbox";
    private static final String DEFAULT_TYPE = "SYSTEM INTERACTION";

    /**
     * Public method to write the ESG to an EFG file.
     *
     * @param esg             The input ESG model.
     * @param productID       The name for the output file (e.g., "P01").
     * @param outputDirectory The directory to save the ".EFG" file.
     * @throws IOException If file writing fails.
     */
    public static void writeESGToEFGFile(ESG esg, String productID, String outputDirectory) throws IOException {
        String filename = productID + ".EFG";
        
        File directory = new File(outputDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        String filepath = outputDirectory + File.separator + filename;

        File file = new File(filepath);
        file.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(generateEFGContent(esg));
        }
    }

    /**
     * Main method to orchestrate the EFG XML content generation.
     */
    private static String generateEFGContent(ESG esg) {

        // 0) Find pseudo start/end ('[' and ']') if they exist
        Vertex pseudoStartVertex = null;
        Vertex pseudoEndVertex = null;
        for (Vertex v : esg.getVertexList()) {
            Event e = v.getEvent();
            if (safeIsPseudoStart(e)) pseudoStartVertex = v;
            if (safeIsPseudoEnd(e)) pseudoEndVertex = v;
        }

        // 1) List of vertices to process: excluding '[' and ']'
        List<Vertex> vertices = new ArrayList<>();
        for (Vertex v : esg.getVertexList()) {
            if (v.equals(pseudoStartVertex) || v.equals(pseudoEndVertex)) continue;
            vertices.add(v);
        }

        // 2) Initial set: entryVertexSet (fallback: successors of pseudoStart)
        Set<Vertex> entrySet = safeGetEntrySet(esg);
        if (entrySet.isEmpty() && pseudoStartVertex != null) {
            entrySet = esg.getVertexMap().getOrDefault(pseudoStartVertex, new HashSet<>());
        }
        
        // Filter the entrySet to create the 'initialVertices' set for tagging <Initial>true</Initial>.
        Set<Vertex> initialVertices = new HashSet<>();
        for (Vertex v : entrySet) {
            if (!v.equals(pseudoStartVertex) && !v.equals(pseudoEndVertex)) {
                initialVertices.add(v);
            }
        }

        // 3) F matrix (immediate followed-by) — only between filtered vertices
        int n = vertices.size();
        boolean[][] F = new boolean[n][n];
        Map<Vertex, Set<Vertex>> vmap = esg.getVertexMap();
        for (int i = 0; i < n; i++) {
            Vertex src = vertices.get(i);
            Set<Vertex> succ = vmap.getOrDefault(src, new HashSet<>());
            for (int j = 0; j < n; j++) {
                Vertex dst = vertices.get(j);
                if (succ.contains(dst)) F[i][j] = true;
            }
        }

        // 4) Write XML
        StringBuilder xml = new StringBuilder();
        xml.append(XML_HEADER).append("\n");
        xml.append("<EFG>\n");

        // Add the <Events> block
        xml.append(generateEventsSection(vertices, initialVertices));
        
        // Add the <EventGraph> block (Matrix ONLY)
        // This will satisfy the GraphUtil constructor that crashed.
        // We pass the 'F' matrix to it.
        xml.append(generateEventGraphSection(vertices, F));

        // We explicitly DO NOT generate <FollowsRelations>
        
        xml.append("</EFG>\n");
        return xml.toString();
    }

    /**
     * Generates the <Events> XML block.
     *
     * @param vertexList      The list of non-pseudo vertices.
     * @param initialVertices The set of vertices to be marked <Initial>true</Initial>.
     * @return A string containing the <Events> XML block.
     */
    private static String generateEventsSection(List<Vertex> vertexList, Set<Vertex> initialVertices) {
        StringBuilder events = new StringBuilder();
        events.append("    <Events>\n");
        for (Vertex vertex : vertexList) {
            Event event = vertex.getEvent();
            int eventId = event.getID();
            String eventName = event.getName();
            
            // This check now works based on the ESG.getEntryVertexSet() data.
            boolean isInitial = initialVertices.contains(vertex);

            events.append("        <Event>\n");
            events.append("            <EventId>e").append(eventId).append("</EventId>\n");
            events.append("            <WidgetId>w").append(eventId).append("</WidgetId>\n");
            events.append("            <Type>").append(DEFAULT_TYPE).append("</Type>\n");
            events.append("            <Initial>").append(isInitial).append("</Initial>\n");
            events.append("            <Action>").append(DEFAULT_ACTION).append("</Action>\n");
            events.append("            <Name>").append(escapeXml(eventName)).append("</Name>\n");
            events.append("        </Event>\n");
        }
        events.append("    </Events>\n");
        return events.toString();
    }


    /**
     * Creates the standard <EventGraph> block.
     * This version ONLY generates the <Row> matrix.
     * It uses '2' for edges to satisfy the SequenceLengthCoverage plugin.
     *
     * @param vertexList The list of non-pseudo vertices.
     * @param F          The 'followed-by' adjacency matrix.
     * @return A string containing the <EventGraph> XML block.
     */
    private static String generateEventGraphSection(List<Vertex> vertexList,
                                                  boolean[][] F) {
        StringBuilder graph = new StringBuilder();
        graph.append("    <EventGraph>\n");
        int n = vertexList.size();
        
        // Generate the <Row> matrix (standard)
        for (int i = 0; i < n; i++) {
            graph.append("        <Row>\n");
            for (int j = 0; j < n; j++) {
                // --- CRITICAL CHANGE ---
                // We use '2' (REACHING_EDGE) because the SequenceLengthCoverage
                // plugin appears to look for this value, not '1'.
                int v = F[i][j] ? 2 : 0;
                
                graph.append("            <E>").append(v).append("</E>\n");
            }
            graph.append("        </Row>\n");
        }
        
        graph.append("    </EventGraph>\n");
        return graph.toString();
    }

    // --- Helper Methods ---

    /**
     * Safely checks if an event is a pseudo-start event.
     */
    private static boolean safeIsPseudoStart(Event e) {
        try { return e != null && e.isPseudoStartEvent(); } catch (Throwable t) { return false; }
    }

    /**
     * Safely checks if an event is a pseudo-end event.
     */
    private static boolean safeIsPseudoEnd(Event e) {
        try { return e != null && e.isPseudoEndEvent(); } catch (Throwable t) { return false; }
    }

    /**
     * Safely gets the entry vertex set from the ESG.
     */
    private static Set<Vertex> safeGetEntrySet(ESG esg) {
        try {
            Set<Vertex> s = esg.getEntryVertexSet(); 
            return (s != null) ? s : new HashSet<>();
        } catch (Throwable t) {
            return new HashSet<>();
        }
    }

    /**
     * Safely gets the exit vertex set from the ESG.
     */
    @SuppressWarnings("unused")
    private static Set<Vertex> safeGetExitSet(ESG esg) {
        try {
            // Assuming esg.getExitSet() or esg.getExitVertexSet() exists
            Set<Vertex> s = esg.getExitVertexSet(); 
            return (s != null) ? s : new HashSet<>();
        } catch (Throwable t) {
            return new HashSet<>();
        }
    }

    /**
     * Escapes special XML characters.
     */
    private static String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}