package tr.edu.iyte.esgfx.testgeneration.guitar;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import java.io.File;

public class EFGMetricsExtractor {

    public static EFGMetrics getMetrics(String efgFilePath) {
        int vertexCount = 0;
        int edgeCount = 0;

        try {
            File fXmlFile = new File(efgFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            Document doc = dbFactory.newDocumentBuilder().parse(fXmlFile);
            doc.getDocumentElement().normalize();

            // 1. Count Vertices (Each <Event> is a vertex)
            vertexCount = doc.getElementsByTagName("Event").getLength();

            // 2. Count Edges (Each <E> in <EventGraph> that is NOT "0")
            NodeList edgeNodes = doc.getElementsByTagName("E");
            for (int i = 0; i < edgeNodes.getLength(); i++) {
                String val = edgeNodes.item(i).getTextContent();
                if (!val.equals("0")) {
                    edgeCount++;
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing EFG metrics: " + e.getMessage());
        }

        return new EFGMetrics(vertexCount, edgeCount);
    }

    public static class EFGMetrics {
        public final int vertices;
        public final int edges;

        public EFGMetrics(int vertices, int edges) {
            this.vertices = vertices;
            this.edges = edges;
        }
    }
}