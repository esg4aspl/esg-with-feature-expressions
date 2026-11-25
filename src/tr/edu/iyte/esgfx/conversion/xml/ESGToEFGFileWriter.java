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
     * @param ESGFx             The input ESG model.
     * @param productID       The name for the output file (e.g., "P01").
     * @param outputDirectory The directory to save the ".EFG" file.
     * @throws IOException If file writing fails.
     */
    public static void writeESGFxToEFGFile(ESG ESGFx, String productID, String outputDirectory) throws IOException {
        String filename = productID + ".EFG";
        
        File directory = new File(outputDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        String filepath = outputDirectory + File.separator + filename;

        File file = new File(filepath);
        file.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(generateEFGContent(ESGFx));
        }
    }

    /**
     * Main method to orchestrate the EFG XML content generation.
     */
    /**
     * The events of a model in the order in which they are written to the
     * Event Flow Graph file: the vertices of the model without the pseudo-start
     * and the pseudo-end vertex. Event e<i> of the file is element i.
     */
    public static List<Vertex> eventsInOrder(ESG esg) {
        List<Vertex> vertices = new ArrayList<>();
        for (Vertex v : esg.getVertexList()) {
            if (v.isPseudoStartVertex() || v.isPseudoEndVertex()) continue;
            vertices.add(v);
        }
        return vertices;
    }

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
        List<Vertex> vertices = eventsInOrder(esg);

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
        // Every edge between two events, taken from the list of edges, so that an edge from an
        // event to itself is part of the event graph as well.
        Map<Vertex, Integer> positionOf = new java.util.HashMap<>();
        for (int i = 0; i < n; i++) {
            positionOf.put(vertices.get(i), i);
        }
        for (tr.edu.iyte.esg.model.Edge edge : esg.getEdgeList()) {
            Integer i = positionOf.get(edge.getSource());
            Integer j = positionOf.get(edge.getTarget());
            if (i != null && j != null) {
                F[i][j] = true;
            }
        }
        // GUITAR distinguishes two kinds of edges. Every edge says that the second event may
        // follow the first. The edges of kind "reaching" say in addition how an event is
        // reached from an initial event: GUITAR puts a path of such edges in front of a test.
        // Its search for that path goes backwards and gives up on some graphs with cycles, so
        // exactly one way is marked for every event: the edges of a breadth-first tree from
        // the initial events. The path GUITAR finds is then a shortest one, and it exists for
        // every event that can be reached.
        boolean[][] reaching = new boolean[n][n];
        boolean[] reached = new boolean[n];
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            if (initialVertices.contains(vertices.get(i))) {
                reached[i] = true;
                queue.add(i);
            }
        }
        while (!queue.isEmpty()) {
            int i = queue.poll();
            for (int j = 0; j < n; j++) {
                if (F[i][j] && !reached[j]) {
                    reached[j] = true;
                    reaching[i][j] = true;
                    queue.add(j);
                }
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
        xml.append(generateEventGraphSection(vertices, F, reaching));

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
        for (int position = 0; position < vertexList.size(); position++) {
            Vertex vertex = vertexList.get(position);
            Event event = vertex.getEvent();
            // The identifier of an event is its position in this list, which is also its row
            // and column in the matrix of the event graph: e0 is the first event, e1 the
            // second. GUITAR writes tests for every edge only when identifiers and positions
            // agree. A test is read back against the same model, whose events are listed in
            // the same order (eventsInOrder), so an event is found by its position and not by
            // its name (names may repeat in a model and may contain line breaks).
            int eventId = position;
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
                                                  boolean[][] F, boolean[][] reaching) {
        StringBuilder graph = new StringBuilder();
        graph.append("    <EventGraph>\n");
        int n = vertexList.size();
        
        // Generate the <Row> matrix (standard)
        for (int i = 0; i < n; i++) {
            graph.append("        <Row>\n");
            for (int j = 0; j < n; j++) {
                // 0: no edge. 1: the second event may follow the first. 2: it may follow, and
                // this edge is on the way from an initial event to the second event.
                int v = !F[i][j] ? 0 : (reaching[i][j] ? 2 : 1);
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