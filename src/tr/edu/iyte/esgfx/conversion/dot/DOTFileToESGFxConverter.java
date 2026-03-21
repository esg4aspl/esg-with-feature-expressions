package tr.edu.iyte.esgfx.conversion.dot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.EdgeSimple;
import tr.edu.iyte.esg.model.Event;
import tr.edu.iyte.esg.model.EventSimple;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.Negation;
import tr.edu.iyte.esgfx.model.featuremodel.Feature;

public class DOTFileToESGFxConverter {

    public static ESGFx parseDOTFileForESGFxCreation(String filePath, Map<String, FeatureExpression> featureExpressionMap) {

        File file = new File(filePath);
        String fileName = file.getName().replaceAll("(?i)\\.dot", "");
        ESGFx esgfx = new ESGFx(0, fileName);
        Map<String, Vertex> tagVertexMap = new LinkedHashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                
                if (line.startsWith("esg") && !line.contains("->")) {
                    String vertexTag = line.split("\\[")[0].trim(); 
                    
                    if (line.contains("label=\"") || line.contains("label = \"")) {
                        int labelStart = line.indexOf("label") + line.substring(line.indexOf("label")).indexOf("\"") + 1;
                        int labelEnd = line.indexOf("\"", labelStart);
                        
                        if (labelStart > 0 && labelEnd > labelStart) {
                            String rawEventName = line.substring(labelStart, labelEnd).trim();
                            Vertex vertex = createVertexRefinedByFeatureExpression(esgfx, rawEventName, featureExpressionMap);
                            tagVertexMap.put(vertexTag, vertex);
                        }
                    }
                }
            }            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedReader br2 = new BufferedReader(new FileReader(file))) {
            String line2;
            while ((line2 = br2.readLine()) != null) {
                line2 = line2.trim();
                
                if (line2.startsWith("esg") && line2.contains("->")) {
                    String[] lineArray = line2.split("->");
                    String sourceTag = lineArray[0].trim();
                    
                    String targetPart = lineArray[1].trim();
                    String targetTag = targetPart.split("\\[")[0].replace(";", "").trim();
                    
                    Vertex source = tagVertexMap.get(sourceTag);
                    Vertex target = tagVertexMap.get(targetTag);
                    
                    if (source != null && target != null) {
                        Edge edge = new EdgeSimple(esgfx.getNextEdgeID(), source, target);
                        esgfx.addEdge(edge);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return esgfx;
    }

    private static Vertex createVertexRefinedByFeatureExpression(ESGFx esgfx, String rawEventName, Map<String, FeatureExpression> featureExpressionMap) {
        Vertex vertex;
        
        if (!rawEventName.equals("[") && !rawEventName.equals("]")) {
            String eventName = rawEventName;
            String featureName = "";
            boolean isNegation = false;
            
            if (rawEventName.contains("/")) {
                String[] eventNameArray = rawEventName.split("/");
                eventName = eventNameArray[0].trim();
                if (eventNameArray.length > 1) {
                    featureName = eventNameArray[1].trim();
                    if (featureName.startsWith("!")) {
                        isNegation = true;
                    }
                }
            }
            
            Event event = esgfx.getEventByEventName(eventName);
            if (event == null) {
                event = new EventSimple(esgfx.getNextEventID(), eventName);
                esgfx.addEvent(event);
            }
            
            FeatureExpression featureExpression = null;
            
            if (!featureName.isEmpty() && featureExpressionMap != null) {
                featureExpression = featureExpressionMap.get(featureName);
            }
            
            if (featureExpression == null) {
                if(isNegation) {
                    Feature feature = new Feature(featureName.substring(1));
                    featureExpression = new Negation(new FeatureExpression(feature));
                } else {
                    Feature feature = new Feature(featureName);
                    featureExpression = new FeatureExpression(feature);
                }
            }
            
            vertex = new VertexRefinedByFeatureExpression(esgfx.getNextVertexID(), event, featureExpression);
            
        } else {
            Event event = esgfx.getEventByEventName(rawEventName);
            if (event == null) {
                event = new EventSimple(esgfx.getNextEventID(), rawEventName);
                esgfx.addEvent(event);
            }
            
            Feature feature = new Feature("");
            FeatureExpression featureExpression = new FeatureExpression(feature, true);
            vertex = new VertexRefinedByFeatureExpression(esgfx.getNextVertexID(), event, featureExpression);
        }
        
        esgfx.addVertex(vertex);
        return vertex;
    }
}