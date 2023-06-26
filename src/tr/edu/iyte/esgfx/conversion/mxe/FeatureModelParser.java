package tr.edu.iyte.esgfx.conversion.mxe;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import tr.edu.iyte.esgfx.model.featuremodel.*;




public class FeatureModelParser {
	
	public FeatureModel parseXMLFileForFeatureModelCreation(String filePath) {

		FeatureModel featureModel = new FeatureModel();
		try {
			File XMLFile = new File(filePath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(XMLFile);
			doc.getDocumentElement().normalize();
			
			NodeList struct = doc.getElementsByTagName("struct");
			if(struct.getLength() > 0)
				parseFeatureModelStruct(featureModel, struct);
			NodeList constraints = doc.getElementsByTagName("constraints");
			if(constraints.getLength() > 0)
				parseFeatureModelConstraints(featureModel, constraints);

		} catch (

		ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException | IOException e) {
			e.printStackTrace();
		}

		return featureModel;

	}

	public void parseFeatureModelStruct(FeatureModel featureModel, NodeList nodeList) {

		Node struct = nodeList.item(0);

		Element element = (Element) struct;

		NodeList featureList = element.getElementsByTagName("feature");
		parseNodeList(featureModel, featureList);
		NodeList andList = element.getElementsByTagName("and");
		parseNodeList(featureModel, andList);
		NodeList altList = element.getElementsByTagName("alt");
		parseNodeList(featureModel, altList);
		NodeList orList = element.getElementsByTagName("or");
		parseNodeList(featureModel, orList);

	}

	public void parseNodeList(FeatureModel featureModel, NodeList nodeList) {

		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			Element element = (Element) node;
			Feature feature = createFeatureFromXMLElement(element);
			featureModel.addFeature(feature);

			if (node.getNodeName().equals("and") && i == 0) {
				featureModel.setRoot(feature);
				//System.out.println("ROOT " + feature.getName());
			} else {
				Node parentNode = node.getParentNode();
				Element parentElement = (Element) parentNode;
				String parentName = parentElement.getAttribute("name");
				Feature parentFeature = featureModel.findFeatureByName(parentName);
				feature.setParent(parentFeature);
			}
			NodeList childNodes = element.getElementsByTagName("feature");
			for (int j = 0; j < childNodes.getLength(); j++) {
				Node childNode = childNodes.item(j);
				Element childElement = (Element) childNode;
				String childName = childElement.getAttribute("name");
				Feature childFeature = featureModel.findFeatureByName(childName);
				childFeature.setParent(feature);
				if (node.getNodeName().equals("or")) {
					featureModel.addORFeature(feature, childFeature);
				} else if (node.getNodeName().equals("alt")) {
					featureModel.addXORFeature(feature, childFeature);
				}

			}
		}

	}

	private Feature createFeatureFromXMLElement(Element element) {
		String abs = element.getAttribute("abstract");
		boolean isAbstract = !abs.equals("");
		// System.out.println("isAbstract " + isAbstract);
		String mandatory = element.getAttribute("mandatory");
		boolean isMandatory = !mandatory.equals("");
		// System.out.println("isMandatory " + isMandatory);
		String name = element.getAttribute("name");
		Feature feature = new Feature(name, isAbstract, isMandatory);

		return feature;
	}

	public void parseFeatureModelConstraints(FeatureModel featureModel, NodeList nodeList) {
		
		Node constraints = nodeList.item(0);
		Element element = (Element) constraints;

		NodeList disjList = element.getElementsByTagName("disj");
		parseConstraint("disj", featureModel, disjList);
		NodeList conjList = element.getElementsByTagName("conj");
		parseConstraint("conj", featureModel, conjList);
		NodeList impList = element.getElementsByTagName("imp");
		parseConstraint("imp", featureModel, impList);

	}

	private void parseConstraint(String s, FeatureModel featureModel, NodeList nodeList) {

		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			Element element = (Element) node;
			if (node.getNodeName().equals("disj")) {
				NodeList varList = element.getElementsByTagName("var");
				Connector disjunction = new ConnectorOR();
				parseVar(featureModel, disjunction, varList);
				featureModel.addConnConstraint(disjunction);
			}else if (node.getNodeName().equals("conj")) {
				NodeList varList = element.getElementsByTagName("var");
				Connector conjunction = new ConnectorAND();
				parseVar(featureModel, conjunction, varList);
				featureModel.addConnConstraint(conjunction);
			}else if (node.getNodeName().equals("imp")) {				
				NodeList allChildNodeList = node.getChildNodes();
				Implication implication = new Implication();
				setLHSandRHSOfImplication(implication, allChildNodeList);
			
				if(implication.getLHStype() == "var") {
					NodeList varList = element.getElementsByTagName("var");
					parseVar(featureModel, implication, varList);
					if(implication.getRHStype()  == "disj") {
						//System.out.println("var => disj");
						NodeList disjList = element.getElementsByTagName("disj");
						String connectorKey = parseConnector(disjList);
						Implicant connector = featureModel.findConnectorByFeatureName(connectorKey);
						implication.setRightHandSide(connector);
					}
					else if(implication.getRHStype() == "conj") {
						//System.out.println("var => conj");
						NodeList conjList = element.getElementsByTagName("conj");
						String connectorKey = parseConnector(conjList);
						Implicant connector = featureModel.findConnectorByFeatureName(connectorKey);
						implication.setRightHandSide(connector);
					}
					featureModel.addImpConstraint(implication);
				}else if(implication.getLHStype()  == "disj") {
					NodeList varList = element.getElementsByTagName("var");
					NodeList disjList = element.getElementsByTagName("disj");
					parseVar(featureModel, implication, varList);
					String connectorKey = parseConnector(disjList);
					Implicant connector = featureModel.findConnectorByFeatureName(connectorKey);
					implication.setLeftHandSide(connector);
					/*
					if(implication.getRHStype() == "disj") {
						System.out.println("disj => disj");
					}else if(implication.getRHStype() == "conj") {
						System.out.println("disj => conj");
					}
					*/
					featureModel.addImpConstraint(implication);
				}else if(implication.getLHStype()  == "conj") {
					NodeList varList = element.getElementsByTagName("var");
					NodeList conjList = element.getElementsByTagName("conj");
					parseVar(featureModel, implication, varList);
					String connectorKey = parseConnector(conjList);
					Implicant connector = featureModel.findConnectorByFeatureName(connectorKey);
					implication.setLeftHandSide(connector);
					/*if(RHS == "disj") {
						System.out.println("conj => disj");

					}else if(RHS == "conj") {
						System.out.println("conj => conj");
					}*/
					featureModel.addImpConstraint(implication);
				}
//System.out.println(implication.toString());
			}

		}

	}
	
	private void setLHSandRHSOfImplication(Implication implication,NodeList allChildNodeList) {
		List<String> nodeNameList = new LinkedList<String>();
		for(int k = 0; k < allChildNodeList.getLength(); k++) {
			Node child = allChildNodeList.item(k);
			if(child.getNodeType() == Node.ELEMENT_NODE){
				if(!child.getNodeName().equals(null))
					nodeNameList.add(child.getNodeName());
		        
			}
		}
		String LHS = nodeNameList.get(0);
		String RHS = nodeNameList.get(1);
		
		implication.setLHStype(LHS);
		implication.setRHStype(RHS);
	}

	private String parseConnector(NodeList nodeList) {
		String str = "";
		Set<String> varSet = new LinkedHashSet<String>();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			Element element = (Element) node;
			NodeList varList = element.getElementsByTagName("var");
			for (int j = 0; j < varList.getLength(); j++) {
				Node var = varList.item(j);
				Element e = (Element) var;
				String value = e.getTextContent();
				varSet.add(value);
			}
		}
		for(String var : varSet) {
			str += var.toLowerCase();
		}
//System.out.println(str);
		return str;
	}

	private void parseVar(FeatureModel featureModel, Connector connector, NodeList nodeList) {

		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			Element element = (Element) node;
			String value = element.getTextContent();
			Feature feature = featureModel.findFeatureByName(value);
			connector.addFeature(feature);
		}
	}
	
	private void parseVar(FeatureModel featureModel, Implication imp, NodeList nodeList) {

		if(imp.getLHStype().equals("var")){
			Node lhsNode = nodeList.item(0);
			Element lhsElement = (Element) lhsNode;
			String lhsValue = lhsElement.getTextContent();
			Feature lhsFeature = featureModel.findFeatureByName(lhsValue);
			imp.setLeftHandSide(lhsFeature);
			
			if(imp.getRHStype().equals("var")) {
				Node rhsNode = nodeList.item(1);
				Element rhsElement = (Element) rhsNode;
				String rhsValue = rhsElement.getTextContent();
				Feature feature = featureModel.findFeatureByName(rhsValue);
				imp.setRightHandSide(feature);
			}

		}else if (imp.getRHStype().equals("var")) {
			Node rhsNode = nodeList.item(nodeList.getLength() - 1);
			Element rhsElement = (Element) rhsNode;
			String rhsValue = rhsElement.getTextContent();
			Feature feature = featureModel.findFeatureByName(rhsValue);
			imp.setRightHandSide(feature);
		}
		
	}

}
