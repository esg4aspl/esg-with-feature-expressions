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

    public static Set<EventSequence> parseGuitarTests(String testDirectoryPath, String efgFilePath, ESGFx productESG) throws Exception {
        Set<EventSequence> generatedSequences = new HashSet<>();
        
        // 1. Build a mapping from GUITAR EventId (e.g., "e2") to actual Event Name (e.g., "pay")
        Map<String, String> eventIdToNameMap = buildEventIdMap(efgFilePath);

        File dir = new File(testDirectoryPath);
        
        if (!dir.exists() || !dir.isDirectory()) {
            return generatedSequences;
        }
        
        File[] tstFiles = dir.listFiles((d, name) -> name.endsWith(".tst"));
        if (tstFiles == null || tstFiles.length == 0) {
            return generatedSequences;
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        for (File tstFile : tstFiles) {
            try {
                Document doc = builder.parse(tstFile);
                doc.getDocumentElement().normalize();

                NodeList stepNodes = doc.getElementsByTagName("Step");
                EventSequence sequence = new EventSequence();

                for (int i = 0; i < stepNodes.getLength(); i++) {
                    Element stepElement = (Element) stepNodes.item(i);
                    NodeList eventIdNodes = stepElement.getElementsByTagName("EventId");
                    
                    if (eventIdNodes != null && eventIdNodes.getLength() > 0) {
                        String eventId = eventIdNodes.item(0).getTextContent().trim();
                        
                        // 2. Map "e2" to "pay"
                        String actualEventName = eventIdToNameMap.get(eventId);
                        
                        if (actualEventName != null) {
                            Vertex foundVertex = null;
                            // 3. Find the corresponding vertex in ESGFx by its real name
                            for (Vertex v : productESG.getVertexList()) {
                                if (v.getEvent().getName().equals(actualEventName)) {
                                    foundVertex = v;
                                    break;
                                }
                            }
                            
                            if (foundVertex != null) {
                                sequence.getEventSequence().add(foundVertex);
                            }
                        }
                    }
                }
                
                if (sequence.length() > 0) {
                    generatedSequences.add(sequence);
                }
            } catch (Exception e) {
                System.err.println("Failed to parse .tst file: " + tstFile.getName() + " - " + e.getMessage());
            }
        }
        
        return generatedSequences;
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