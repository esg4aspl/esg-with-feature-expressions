package tr.edu.iyte.esgfx.conversion.mxe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import tr.edu.iyte.esg.conversion.mxe.MXEFiletoESGConverter;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.EdgeSimple;
import tr.edu.iyte.esg.model.Event;
import tr.edu.iyte.esg.model.EventSimple;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.Negation;
import tr.edu.iyte.esgfx.model.featuremodel.Feature;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;
import tr.edu.iyte.esgfx.conversion.xml.FeatureModelParser;
import tr.edu.iyte.esgfx.model.ESGFx;

import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;

public class MXEFileToESGFxConverter extends MXEFiletoESGConverter {

	private Map<String, FeatureExpression> featureExpressionMap;
	private FeatureModel featureModel;

	public MXEFileToESGFxConverter() {
		featureExpressionMap = new LinkedHashMap<>();

	}

	public Map<String, FeatureExpression> getFeatureExpressionMap() {
		return featureExpressionMap;
	}

	/**
	 * Parse the given file to create a simple ESG
	 * 
	 * @param filePath
	 * @return
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	public ESG parseMXEFileForESGFxCreation(String filePath)
			throws SAXException, IOException, ParserConfigurationException {
		ESG ESGFx = new ESGFx(-1, filePath);
		createESGFx(filePath, ESGFx);
		return ESGFx;
	}

	/**
	 * 
	 * @param filePath
	 * @param esgName
	 * @param esgID
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public ESG parseMXEFileForESGFxCreation(String filePath, String esgName, int esgID)
			throws SAXException, IOException, ParserConfigurationException {
		ESG ESGFx = new ESGFx(esgID, esgName);
		createESGFx(filePath, ESGFx);
		return ESGFx;
	}

	private void createESGFx(String filePath, ESG ESGFx)
			throws SAXException, IOException, ParserConfigurationException {
		try {

			File fXmlFile = new File(filePath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			doc.getDocumentElement().normalize();

			NodeList mxCellList = doc.getElementsByTagName("mxCell");
			NodeList eventNodeList = doc.getElementsByTagName("de.upb.adt.tsd.EventNode");

			List<String> vertexIdList = parseNodeList(mxCellList, "vertex", "id");
			List<String> vertexNameList = parseNodeList(eventNodeList, "name");
			List<String> edgeList = parseNodeList(mxCellList, "edge", "source", "target");

			Map<String, Vertex> vertexIDvertexMap = new HashMap<String, Vertex>();

			//System.out.println("vertexIdList.size() " + vertexIdList.size());
			int i = 0;
			for (int temp = 0; temp < vertexIdList.size(); temp++) {
				String eventName = vertexNameList.get(temp);
				eventName = EventNameModifier.modifyEventName(eventName);
//				System.out.println("eventName in file " + eventName);

				Vertex vertex = null;
				if (!eventName.equals("[") && !eventName.equals("]")) {
					//System.out.println("unsplitted eventName in file " + eventName);
					String[] eventNameArray = eventName.split("/");
					eventName = eventNameArray[0].trim();
					String featureName = eventNameArray[1].trim();
					//System.out.println("eventName in file " + eventName);
					//System.out.println("featureName in file " + featureName);
					Event event = new EventSimple(ESGFx.getNextEventID(), eventName);
					ESGFx.addEvent(event);

					FeatureExpression featureExpression = new FeatureExpression();
					featureExpression = parseFeatureExpression(featureName);
					//System.out.println("featureExpression: "+featureExpression.toString());
					//System.out.println("--------------------------");
					vertex = new VertexRefinedByFeatureExpression(ESGFx.getNextVertexID(), event, featureExpression);
				} else {
//					System.out.println(eventName);
					Event event = new EventSimple(ESGFx.getNextEventID(), eventName);
					ESGFx.addEvent(event);
//					vertex = new VertexSimple(ESGFx.getNextVertexID(), event);
					vertex = new VertexRefinedByFeatureExpression(ESGFx.getNextVertexID(), event,
							new FeatureExpression(new Feature(""), true));
				}

				ESGFx.addVertex(vertex);
				vertexIDvertexMap.put(vertexIdList.get(temp), vertex);
//				i++;
			}
//			//System.out.println("vertexIdList.size() " + i);

			for (int temp = 0; temp < edgeList.size(); temp++) {
				String[] edges = edgeList.get(temp).split(",");
				String source = edges[0];
				String target = edges[1];

				Vertex sourceVertex = vertexIDvertexMap.get(source);
				
				Vertex targetVertex = vertexIDvertexMap.get(target);
				//System.out.println("sourceVertex " + sourceVertex);
				//System.out.println("targetVertex " + targetVertex);

				Edge edge = new EdgeSimple(ESGFx.getNextEdgeID(), sourceVertex, targetVertex);

				((ESGFx) ESGFx).addEdge(edge);

			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		addAbstractFeatureExpressionsNecessaryForProductConfiguration(featureModel.getORFeatures());
		addAbstractFeatureExpressionsNecessaryForProductConfiguration(featureModel.getXORFeatures());

		
//		  //System.out.println("ESGFx.getEventList().size() " +
//		  ESGFx.getEventList().size());
//		  
//		  for(String key : featureExpressionMap.keySet()) { //System.out.println(key +"-"
//		  + featureExpressionMap.get(key).getFeature().getName());
//		  
//		  }
		 
	}

	private Feature searchFeature(String featureName) {

		Feature feature = featureModel.findFeatureByName(featureName);
		if (feature == null) {
			//System.out.println("Feature not found in the feature model");
		}
		return feature;

	}

	public FeatureModel parseFeatureModel(String featureModelPath) throws Exception {

		try {
			// Initialize the SAX parser
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();

			// Initialize the handler
			FeatureModelParser parser = new FeatureModelParser();

			// Parse the XML file
			File inputFile = new File(featureModelPath);
			saxParser.parse(inputFile, parser);

			// Get and print the populated FeatureModel
			featureModel = parser.getFeatureModel();
//			//System.out.println(featureModel);

		} catch (Exception e) {
			e.printStackTrace();
		}

//		FeatureModelParser featureModelParser = new FeatureModelParser();
//		featureModel = featureModelParser.parseFeatureModel(featureModelPath);
//		//System.out.println(featureModel);
		

		return featureModel;
	}
	
	private FeatureExpression parseFeatureExpression(String featureName) {

//			//System.out.println("Feature name " + featureName);

		Feature feature = new Feature();
		if (featureName.contains("!")) {
			String updatedFeatureName = featureName.substring(1);
			feature = searchFeature(updatedFeatureName);
//				//System.out.println("Found feature's name " + feature.getName());

		} else {
			feature = searchFeature(featureName);
//				//System.out.println("Found feature's name "+ feature.getName());
		}

		if (featureExpressionMap.containsKey(featureName)) {
//				//System.out.println("1st IF");
			
//				for(String s : featureExpressionMap.keySet()) {
//					//System.out.println("1st IF " + s + " " + featureExpressionMap.get(s).getFeature().getName());
//				}

			return featureExpressionMap.get(featureName);
		} else {
//				//System.out.println("1st ELSE");
			if (featureName.contains("!")) {
//					//System.out.println("2nd IF");
				String updatedFeatureName = featureName.substring(1);

				if (featureExpressionMap.containsKey(updatedFeatureName)) {
//						//System.out.println("3rd IF");
					FeatureExpression existing = featureExpressionMap.get(updatedFeatureName);
//						//System.out.println("existing.equals(null) " + existing.equals(null));
					FeatureExpression negation = new Negation(existing);
					featureExpressionMap.put(featureName, negation);

//						for(String s : featureExpressionMap.keySet()) {
//							//System.out.println("3rd IF " + s + " " + featureExpressionMap.get(s).getFeature().getName());
//						}

					return negation;
				} else {
//					//System.out.println("3rd ELSE");						
//						//System.out.println("feature.equals(null) " + feature.equals(null));
					FeatureExpression featureExpression = new FeatureExpression(feature);
//						//System.out.println("featureExpression.equals(null) " + featureExpression.equals(null));
					featureExpressionMap.put(updatedFeatureName, featureExpression);

					FeatureExpression negation = new Negation(featureExpression);
//						//System.out.println("negation.equals(null) " + negation.equals(null));
					featureExpressionMap.put(featureName, negation);

//						for(String s : featureExpressionMap.keySet()) {
//							//System.out.println("3rd ELSE " + s + " " + featureExpressionMap.get(s).getFeature().getName());
//						}

					return negation;
				}
			} else {
//					//System.out.println("2nd ELSE");
//					//System.out.println(feature);
				FeatureExpression featureExpression = new FeatureExpression(feature);
				featureExpressionMap.put(featureName, featureExpression);

//					for(String s : featureExpressionMap.keySet()) {
//						//System.out.println("2nd ELSE " + s + " " + featureExpressionMap.get(s).getFeature().getName());
//					}

				return featureExpression;
			}
		}

	}
	
	private void addAbstractFeatureExpressionsNecessaryForProductConfiguration(Map<Feature, Set<Feature>> featureMap) {

		Iterator<Feature> keySeyIterator = featureMap.keySet().iterator();

		while (keySeyIterator.hasNext()) {
			Feature keyFeature = keySeyIterator.next();

			if (keyFeature.isAbstract() && keyFeature.isMandatory()) {
				Iterator<Feature> valueSetIterator = featureMap.get(keyFeature).iterator();

				while (valueSetIterator.hasNext()) {
					Feature value = valueSetIterator.next();
					
//					//System.out.println("FEATURE " + value.getName() + "isAbstract "+ value.isAbstract() + "isLeaf "+ featureModel.isLeaf(value));
					
					if (value.isAbstract() && featureModel.isLeaf(value)) {
						if (!featureExpressionMap.containsKey(value.getName())) {
							FeatureExpression featureExpression = new FeatureExpression(value);
							featureExpressionMap.put(value.getName(), featureExpression);
						}
					}
				}
			}
		}

	}
	


	public static List<String> parseNodeList(NodeList nodeList, String... attribute) {

		List<String> attributeList = new ArrayList<String>();

		for (int i = 0; i < nodeList.getLength(); i++) {

			Node node = nodeList.item(i);

			if (node.getNodeType() == Node.ELEMENT_NODE) {

				Element element = (Element) node;

				if (attribute[0].equals("vertex")) {
					if (element.getAttribute(attribute[0]).equals("1")) {
						String id = element.getAttribute(attribute[1]);
						attributeList.add(id);

					}
				} else if (attribute[0].equals("edge")) {
					if (element.getAttribute(attribute[0]).equals("1")) {
						String source = element.getAttribute(attribute[1]);
						String target = element.getAttribute(attribute[2]);
						String edge = source + "," + target;
						attributeList.add(edge);

					}
				} else {
					String eventName = element.getAttribute(attribute[0]);
					attributeList.add(eventName);
				}

			}
		}

		return attributeList;

	}

}
