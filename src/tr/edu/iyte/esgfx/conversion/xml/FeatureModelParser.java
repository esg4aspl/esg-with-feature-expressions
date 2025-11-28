package tr.edu.iyte.esgfx.conversion.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import tr.edu.iyte.esgfx.model.featuremodel.Connector;
import tr.edu.iyte.esgfx.model.featuremodel.ConnectorAND;
import tr.edu.iyte.esgfx.model.featuremodel.ConnectorOR;
import tr.edu.iyte.esgfx.model.featuremodel.Feature;
import tr.edu.iyte.esgfx.model.featuremodel.FeatureModel;
import tr.edu.iyte.esgfx.model.featuremodel.Iff;
import tr.edu.iyte.esgfx.model.featuremodel.Implication;
import tr.edu.iyte.esgfx.model.featuremodel.Negation;

import java.util.Stack;

public class FeatureModelParser extends DefaultHandler {

	private FeatureModel featureModel;
	private Connector currentORConnector;
	private Connector currentANDConnector;

	private Stack<Connector> connectorStack;

	private Feature parentFeature;
	private Feature currentFeature;
	private Feature constraintFeature;

	private Implication currentImplication;

	private String varString;

	private boolean isAltEnded = true;
	private boolean isOrEnded = true;
	private boolean isImplicationEnded = true;
	private boolean isDisjEnded = true;
	private boolean isConjEnded = true;
	private boolean isVarEnded = true;
	private boolean isIffEnded = true;
	private boolean isAndEnded = true;
	private boolean isNotEnded = true;

	private boolean isRuleEnded = true;

	public FeatureModelParser() {
		featureModel = new FeatureModel();
		connectorStack = new Stack<Connector>();
	}

	public FeatureModel getFeatureModel() {
		return featureModel;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
//		System.out.println("Start Element :" + qName);

		boolean isAbstract = false;
		boolean isMandatory = false;
		String name;

		switch (qName.toLowerCase()) {
		case "graphics":
			// Ignore graphics
			break;
		case "and":
			isAndEnded = false;
			isAbstract = Boolean.parseBoolean(attributes.getValue("abstract"));

			isMandatory = Boolean.parseBoolean(attributes.getValue("mandatory"));

			name = attributes.getValue("name");
			if (name == null || name.trim().isEmpty()) {
				System.err.println("Warning: " + qName + " element without name attribute, skipping...");
				break;
			}
			currentFeature = new Feature(name, isAbstract, isMandatory);
			parentFeature = currentFeature;

			if (featureModel.getRoot() == null) {
				featureModel.setRoot(currentFeature);
			} else {
				currentFeature.setParent(featureModel.getRoot());
//				System.out.println("HERE");
//				System.out.println("parent Feature: " + featureModel.getRoot());
//				System.out.println("current Feature: " + currentFeature.getName());
				featureModel.addANDFeature(featureModel.getRoot(), currentFeature);
			}
			featureModel.addFeature(currentFeature);
//			System.out.println(currentFeature.getName() + " isAbstract:" + currentFeature.isAbstract() + " isMandatory:"
//					+ currentFeature.isMandatory() + " parent Feature:" + parentFeature.getName());
//			System.out.println("Is Root?" + featureModel.getRoot().equals(currentFeature));
			break;
		case "or":
			isOrEnded = false;
			isAltEnded = true;
			isAndEnded = false;
			isAbstract = Boolean.parseBoolean(attributes.getValue("abstract"));
			isMandatory = Boolean.parseBoolean(attributes.getValue("mandatory"));
			name = attributes.getValue("name");

			if (name == null || name.trim().isEmpty()) {
				System.err.println("Warning: " + qName + " element without name attribute, skipping...");
				break;
			}

			currentFeature = new Feature(name, isAbstract, isMandatory);

			currentFeature.setParent(parentFeature);
			featureModel.addFeature(currentFeature);

//			System.out.println(currentFeature.getName() + " isAbstract:" + currentFeature.isAbstract() + " isMandatory:"
//			+ currentFeature.isMandatory() + " parent Feature:" + parentFeature.getName());

			if (isAbstract) {
				featureModel.addANDFeature(parentFeature, currentFeature);
			} else {
				featureModel.addORFeature(parentFeature, currentFeature);
			}

			if (!isOrEnded) {
				parentFeature = currentFeature;
			} else {
//				parentFeature = featureModel.getRoot();

				if (parentFeature.getParent() != null) {
					parentFeature = parentFeature.getParent();
				} else {
					parentFeature = featureModel.getRoot();
				}

			}
//			System.out.println("parent Feature: " + parentFeature.getName());
//			System.out.println(currentFeature.getName() + " isAbstract:" + currentFeature.isAbstract() + " isMandatory:"
//					+ currentFeature.isMandatory() + " parent Feature:" + currentFeature.getParent().getName());
			break;

		case "alt":
			isAltEnded = false;
			isOrEnded = true;
			isAbstract = Boolean.parseBoolean(attributes.getValue("abstract"));
			isMandatory = Boolean.parseBoolean(attributes.getValue("mandatory"));
			name = attributes.getValue("name");
//			System.out.println(name);
//			System.out.println("ALT");
//			System.out.println("isAbstract: " + isAbstract);
//			System.out.println("isMandatory: " + isMandatory);
//			System.out.println("name: " + name);
//			System.out.println("parentFeature: " + parentFeature.getName());
			currentFeature = new Feature(name, isAbstract, isMandatory);

			if (name == null || name.trim().isEmpty()) {
				System.err.println("Warning: " + qName + " element without name attribute, skipping...");
				break;
			}

			currentFeature.setParent(parentFeature);
			featureModel.addFeature(currentFeature);

			if (isAbstract) {
				featureModel.addANDFeature(parentFeature, currentFeature);
			} else {
				featureModel.addXORFeature(parentFeature, currentFeature);
			}

			if (!isAltEnded) {
				parentFeature = currentFeature;
			} else {
//				parentFeature = featureModel.getRoot();
				if (parentFeature.getParent() != null) {
					parentFeature = parentFeature.getParent();
				} else {
					parentFeature = featureModel.getRoot();
				}

			}
//			System.out.println("parent Feature: " + parentFeature.getName());
//			System.out.println(currentFeature.getName() + " isAbstract:" + currentFeature.isAbstract() + " isMandatory:"
//					+ currentFeature.isMandatory() + " parent Feature:" + currentFeature.getParent().getName());
			break;

		case "feature":
//			isAbstract = false;
			isAbstract = Boolean.parseBoolean(attributes.getValue("abstract"));
			isMandatory = Boolean.parseBoolean(attributes.getValue("mandatory"));
			name = attributes.getValue("name");

			if (name == null || name.trim().isEmpty()) {
				System.err.println("Warning: " + qName + " element without name attribute, skipping...");
				break;
			}

			currentFeature = new Feature(name, isAbstract, isMandatory);
			currentFeature.setParent(parentFeature);
			featureModel.addFeature(currentFeature);

			if (!isOrEnded) {
//				System.out.println("Adding OR feature");
				featureModel.addORFeature(parentFeature, currentFeature);
			} else if (!isAltEnded) {
//				System.out.println("Adding ALT feature");
				featureModel.addXORFeature(parentFeature, currentFeature);
			}

			if (!isAndEnded && isAltEnded && isOrEnded) {
				featureModel.addANDFeature(parentFeature, currentFeature);
			}

//			else if (!isAndEnded) {
//				System.out.println("Adding AND feature");
//				featureModel.addANDFeature(parentFeature, currentFeature);
//			}
//			System.out.println("parent Feature: " + parentFeature.getName());
//			System.out.println(currentFeature.getName() + " isAbstract:" + currentFeature.isAbstract() + " isMandatory:"
//					+ currentFeature.isMandatory() + " parent Feature:" + currentFeature.getParent().getName());
			break;
		case "disj":
			isDisjEnded = false;

			ConnectorOR newOr = new ConnectorOR();
			connectorStack.push(newOr);

			currentORConnector = newOr;
			break;

		case "conj":
			isConjEnded = false;

			ConnectorAND newAnd = new ConnectorAND();
			connectorStack.push(newAnd);
			currentANDConnector = newAnd;
			break;
		case "imp":
			isImplicationEnded = false;
			currentImplication = new Implication();
			break;
		case "var":
			isVarEnded = false;
			break;
		case "not":
			isNotEnded = false;
			break;
		case "rule":
			isRuleEnded = false;
			break;
		case "eq":
			isIffEnded = false;
			currentImplication = new Iff();
			break;
		default:
			break;
		}

//		System.out.println("Is Alt Ended? - 2 " + isAltEnded);
//		System.out.println("Is Or Ended? - 2 " + isOrEnded);

	}

//parser ends parsing the specific element inside the document  
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
//		System.out.println("End Element:" + qName);

		switch (qName.toLowerCase()) {
		case "graphics":
			// Ignore graphics
			break;
		case "and":
			parentFeature = featureModel.getRoot();
			isAndEnded = true;
			break;
		case "alt":
			parentFeature = parentFeature.getParent();
			isAltEnded = true;
			break;
		case "or":
			parentFeature = parentFeature.getParent();
			isOrEnded = true;
			break;
		case "imp":
			isImplicationEnded = true;
			currentImplication.setLeftHandSide();
			currentImplication.setRightHandSide();
			setLHSType(currentImplication);
			setRHSType(currentImplication);

//			for (Implicant implicant : currentImplication.getImplicants()) {
//				System.out.println("Implicant in implicantion: " + implicant.toString());
//			}
			featureModel.addImpConstraint(currentImplication);
//			System.out.println("Number of implications " + featureModel.getImpConstraints().size());

			break;
		case "eq":
			isIffEnded = true;
			currentImplication.setLeftHandSide();
			currentImplication.setRightHandSide();
			setLHSType(currentImplication);
			setRHSType(currentImplication);

			featureModel.addIffConstraint(currentImplication);

			break;
		case "disj":
			isDisjEnded = true;

			if (!connectorStack.isEmpty()) {

				Connector finishedConnector = connectorStack.pop();

				if (!connectorStack.isEmpty()) {

					Connector parentConnector = connectorStack.peek();
					parentConnector.addImplicant(finishedConnector);

					if (parentConnector instanceof ConnectorOR) {
						currentORConnector = (ConnectorOR) parentConnector;
					}
				} else {

					if (!isImplicationEnded || !isIffEnded) {
						currentImplication.addImplicant(finishedConnector);
					}

					else {
						featureModel.addConnConstraint(finishedConnector);
					}
					currentORConnector = null;
				}
			}
			break;
		case "conj":
			isConjEnded = true;

			if (!connectorStack.isEmpty()) {
				Connector finishedConnector = connectorStack.pop();
				if (!connectorStack.isEmpty()) {
					Connector parentConnector = connectorStack.peek();

					parentConnector.addImplicant(finishedConnector);
					if (parentConnector instanceof ConnectorAND) {
						currentANDConnector = (ConnectorAND) parentConnector;
					}
				} else {
					if (!isImplicationEnded || !isIffEnded) {
						currentImplication.addImplicant(finishedConnector);
					}
					currentANDConnector = null;
				}
			}

			break;
		case "var":
			isVarEnded = true;
			break;
		case "rule":
			isRuleEnded = true;
			currentORConnector = null;
			currentANDConnector = null;
			connectorStack.clear();
			break;
		}
	}

	private void setLHSType(Implication currentImplication) {
		if (currentImplication.getLeftHandSide() instanceof ConnectorOR) {
			currentImplication.setLHStype("disj");
		} else if (currentImplication.getLeftHandSide() instanceof ConnectorAND) {
			currentImplication.setLHStype("conj");
		} else {
			currentImplication.setLHStype("var");
		}
	}

	private void setRHSType(Implication currentImplication) {
		if (currentImplication.getRightHandSide() instanceof ConnectorOR) {
			currentImplication.setRHStype("disj");
		} else if (currentImplication.getRightHandSide() instanceof ConnectorAND) {
			currentImplication.setRHStype("conj");
		} else {
			currentImplication.setRHStype("var");
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {

		if (!isVarEnded) {

			varString = new String(ch, start, length);

			// System.out.println("VAR: " + varString);

			constraintFeature = featureModel.findFeatureByName(varString.trim());

			// System.out.println("constraintFeature: " + constraintFeature.getName());

			if (!isNotEnded) {

				constraintFeature = new Negation(constraintFeature);

			}

			if (constraintFeature != null) {

				// System.out.println("!isRuleEnded && !isDisjEnded " + (!isRuleEnded &&
				// !isDisjEnded));

				if (!isRuleEnded && !isDisjEnded) {

					// currentORConnector.addFeature(constraintFeature);

					if (!connectorStack.isEmpty()) {

						Connector topConnector = connectorStack.peek();

						topConnector.addImplicant(constraintFeature); // Add to the top connector

						// System.out.println("Is ConnectorOR? " + (topConnector instanceof
						// ConnectorOR));

						// System.out.println("Top connector: " + topConnector.toString());

					} else if (!isImplicationEnded || !isIffEnded) {

						currentImplication.addImplicant(constraintFeature);

					}

				}

				// System.out.println("!isRuleEnded && !isConjEnded " + (!isRuleEnded &&
				// !isConjEnded));

				if (!isRuleEnded && !isConjEnded) {

					// currentANDConnector.addFeature(constraintFeature);

					if (!connectorStack.isEmpty()) {

						Connector topConnector = connectorStack.peek();

						topConnector.addImplicant(constraintFeature); // Add to the top connector

						// System.out.println("Is ConnectorAND? " + (topConnector instanceof
						// ConnectorAND));

						// System.out.println("Top connector: " + topConnector.toString());

					}

				}

				// System.out.println("!(!isDisjEnded || !isConjEnded) && (!isImplicationEnded
				// || !isIffEnded) "

				// + (!(!isDisjEnded || !isConjEnded) && !isImplicationEnded || !isIffEnded));

				if (!(!isDisjEnded || !isConjEnded) && (!isImplicationEnded || !isIffEnded)) {

					currentImplication.addImplicant(constraintFeature); // Add directly to implication

				}

			}

		}

	}

}
