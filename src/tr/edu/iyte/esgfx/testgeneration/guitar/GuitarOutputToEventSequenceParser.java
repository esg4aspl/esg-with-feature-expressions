package tr.edu.iyte.esgfx.testgeneration.guitar;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.ESGFx;

public class GuitarOutputToEventSequenceParser {

    /**
     * Reads the test files that GUITAR wrote for one product. An event of a
     * test is found by its identifier, which is its position among the events
     * of the product-level model the Event Flow Graph file was written from;
     * that must be the model given here. The name recorded in the Event Flow Graph
     * file is compared with the name of the vertex, and a difference stops the
     * run, because it means that the file belongs to another model. The files
     * are read in the order of their names, and the suite keeps that order.
     */
    public static Set<EventSequence> parseGuitarTests(String testDirectoryPath, String efgFilePath, ESGFx productESG) throws Exception {
        Set<EventSequence> generatedSequences = new java.util.LinkedHashSet<>();

        Map<String, String> eventIdToNameMap = buildEventIdMap(efgFilePath);

        File dir = new File(testDirectoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return generatedSequences;
        }

        File[] tstFiles = dir.listFiles((d, name) -> name.endsWith(".tst"));
        if (tstFiles == null || tstFiles.length == 0) {
            return generatedSequences;
        }
        java.util.Arrays.sort(tstFiles, (f1, f2) -> f1.getName().compareTo(f2.getName()));

        java.util.List<Vertex> eventsInOrder = tr.edu.iyte.esgfx.conversion.xml.ESGToEFGFileWriter.eventsInOrder(productESG);
        if (eventIdToNameMap.size() != eventsInOrder.size()) {
            throw new IllegalStateException(efgFilePath + " has " + eventIdToNameMap.size()
                    + " events, the product-level model has " + eventsInOrder.size());
        }
        Map<String, Vertex> vertexOfEventId = new HashMap<>();
        for (Map.Entry<String, String> entry : eventIdToNameMap.entrySet()) {
            int position = Integer.parseInt(entry.getKey().substring(1));
            Vertex vertex = position < eventsInOrder.size() ? eventsInOrder.get(position) : null;
            if (vertex == null || !sameName(vertex.getEvent().getName(), entry.getValue())) {
                throw new IllegalStateException("Event " + entry.getKey() + " (" + entry.getValue() + ") of " + efgFilePath
                        + " is not the event at that position in the product-level model"
                        + (vertex == null ? "" : " (" + vertex.getEvent().getName() + ")"));
            }
            vertexOfEventId.put(entry.getKey(), vertex);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        for (File tstFile : tstFiles) {
            Document doc = builder.parse(tstFile);
            doc.getDocumentElement().normalize();

            NodeList stepNodes = doc.getElementsByTagName("Step");
            EventSequence sequence = new EventSequence();

            for (int i = 0; i < stepNodes.getLength(); i++) {
                Element stepElement = (Element) stepNodes.item(i);
                NodeList eventIdNodes = stepElement.getElementsByTagName("EventId");
                if (eventIdNodes == null || eventIdNodes.getLength() == 0) {
                    continue;
                }
                String eventId = eventIdNodes.item(0).getTextContent().trim();
                Vertex vertex = vertexOfEventId.get(eventId);
                if (vertex == null) {
                    throw new IllegalStateException("Test " + tstFile + " uses the event " + eventId
                            + ", which is not in " + efgFilePath);
                }
                sequence.getEventSequence().add(vertex);
            }

            if (sequence.length() > 0) {
                generatedSequences.add(sequence);
            }
        }

        return generatedSequences;
    }

    /** Names are compared with every run of white space, line breaks included, taken as one space. */
    private static boolean sameName(String first, String second) {
        return first != null && second != null
                && first.replaceAll("\\s+", " ").trim().equals(second.replaceAll("\\s+", " ").trim());
    }

    private static Map<String, String> buildEventIdMap(String efgFilePath) {
        Map<String, String> map = new HashMap<>();
        try {
            File efgFile = new File(efgFilePath);
            if (!efgFile.exists()) {
                System.err.println("WARNING: EFG file not found at " + efgFilePath);
                return map;
            }

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(efgFile);
            doc.getDocumentElement().normalize();

            // Extract mappings from the <Events> block in the EFG file
            NodeList eventNodes = doc.getElementsByTagName("Event");
            for (int i = 0; i < eventNodes.getLength(); i++) {
                Element eventElement = (Element) eventNodes.item(i);
                
                String eventId = eventElement.getElementsByTagName("EventId").item(0).getTextContent().trim();
                String eventName = eventElement.getElementsByTagName("Name").item(0).getTextContent().trim();
                
                map.put(eventId, eventName);
            }
        } catch (Exception e) {
            System.err.println("Error parsing EFG mapping for: " + efgFilePath + " - " + e.getMessage());
        }
        return map;
    }
}