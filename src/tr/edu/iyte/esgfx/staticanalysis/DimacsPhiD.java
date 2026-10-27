package tr.edu.iyte.esgfx.staticanalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Reads Phi_d from the DIMACS file of the feature model (standard semantics;
 * see {@link PhiDSource#DIMACS} for which exporter wrote each file):
 * {@code <abbr>.dimacs} with {@code <abbr>_dimacsmapping.txt}, and takes the
 * abstract features from {@code model.xml}. DIMACS variable {@code i} is
 * variable {@code i} of the result, so UniGen samples drawn from the same file
 * can be read without renaming.
 *
 * <p>This is a separate code path from {@code FeatureModelCnf}; the generation
 * code is not involved.
 */
public final class DimacsPhiD {

	private DimacsPhiD() {
	}

	public static PhiD read(File dimacs, File mapping, File modelXml) throws IOException {
		TreeMap<Integer, String> names = readMapping(mapping);
		int declared = -1;
		List<int[]> clauses = new ArrayList<int[]>();
		List<Integer> pending = new ArrayList<Integer>();
		try (BufferedReader reader = new BufferedReader(new FileReader(dimacs))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("c")) {
					continue;
				}
				if (trimmed.startsWith("p")) {
					String[] header = trimmed.split("\\s+");
					declared = Integer.parseInt(header[2]);
					continue;
				}
				// A clause may span lines; it ends at the literal 0.
				for (String token : trimmed.split("\\s+")) {
					int literal = Integer.parseInt(token);
					if (literal == 0) {
						int[] clause = new int[pending.size()];
						for (int i = 0; i < clause.length; i++) {
							clause[i] = pending.get(i);
						}
						clauses.add(clause);
						pending.clear();
					} else {
						pending.add(literal);
					}
				}
			}
		}
		if (!pending.isEmpty()) {
			throw new IllegalStateException(dimacs + ": last clause is not terminated by 0");
		}
		if (declared != names.size() || names.firstKey() != 1 || names.lastKey() != declared) {
			throw new IllegalStateException(dimacs + " declares " + declared + " variables, the mapping names "
					+ names.size() + " (ids " + names.firstKey() + ".." + names.lastKey() + ")");
		}
		Set<String> abstractFeatures = abstractFeatures(modelXml);
		for (String name : abstractFeatures) {
			if (!names.containsValue(name)) {
				throw new IllegalStateException("Abstract feature " + name + " of " + modelXml + " is not in " + mapping);
			}
		}
		return new PhiD(PhiDSource.DIMACS.label(), new ArrayList<String>(names.values()), clauses, abstractFeatures);
	}

	/** {@code ID:FeatureName} lines (FeatureIDE) or {@code ID FeatureName} lines; other lines are skipped. */
	static TreeMap<Integer, String> readMapping(File mapping) throws IOException {
		TreeMap<Integer, String> names = new TreeMap<Integer, String>();
		try (BufferedReader reader = new BufferedReader(new FileReader(mapping))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String trimmed = line.trim();
				int separator = 0;
				while (separator < trimmed.length() && Character.isDigit(trimmed.charAt(separator))) {
					separator++;
				}
				if (separator == 0 || separator == trimmed.length()) {
					continue;
				}
				char after = trimmed.charAt(separator);
				if (after != ':' && !Character.isWhitespace(after)) {
					continue;
				}
				int id = Integer.parseInt(trimmed.substring(0, separator));
				String name = trimmed.substring(separator + 1).trim();
				if (names.put(id, name) != null) {
					throw new IllegalStateException(mapping + ": id " + id + " occurs twice");
				}
			}
		}
		return names;
	}

	/** Names of the features marked {@code abstract="true"} in the struct of a FeatureIDE model.xml. */
	public static Set<String> abstractFeatures(File modelXml) throws IOException {
		try {
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(modelXml);
			Set<String> result = new LinkedHashSet<String>();
			NodeList structs = document.getElementsByTagName("struct");
			for (int i = 0; i < structs.getLength(); i++) {
				collectAbstract(structs.item(i), result);
			}
			return result;
		} catch (IOException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new IOException("Cannot read " + modelXml, ex);
		}
	}

	private static void collectAbstract(Node node, Set<String> result) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				Element element = (Element) child;
				if (element.hasAttribute("name") && "true".equals(element.getAttribute("abstract"))) {
					result.add(element.getAttribute("name"));
				}
				collectAbstract(element, result);
			}
		}
	}
}
