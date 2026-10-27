package tr.edu.iyte.esgfx.staticanalysis;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Writes the CNF of a FeatureIDE model.xml with the standard feature-model
 * semantics as DIMACS, with a mapping file in FeatureIDE's {@code ID:FeatureName}
 * form. Used for the product lines that have no DIMACS in the engine
 * repository (eMail, Elevator, Bank Account, Student Attendance): the copies in
 * the vibes repository add the engine's at-least-one-optional-feature clause,
 * and the engine's exporter writes {@code null} for a negated constraint
 * operand. The output is checked for equivalence against the engine
 * repository's DIMACS of the other four product lines ({@code --check}).
 *
 * <p>Semantics: the root is selected; a selected child selects its parent; a
 * mandatory child of an AND feature is selected with its parent; an OR group
 * needs at least one child, an alternative group exactly one child, when its
 * parent is selected; cross-tree constraints (imp, eq, disj, conj, not, var)
 * are converted to CNF by distribution, without auxiliary variables, so every
 * variable is a feature. Variables are numbered in depth-first order of the
 * feature tree.
 *
 * <p>Usage: {@code FeatureModelDimacsExport model.xml out.dimacs out_mapping.txt}
 * or {@code FeatureModelDimacsExport --check model.xml reference.dimacs reference_mapping.txt}.
 */
public final class FeatureModelDimacsExport {

	private final Map<String, Integer> ids = new LinkedHashMap<String, Integer>();
	private final List<int[]> clauses = new ArrayList<int[]>();

	public static void main(String[] args) throws Exception {
		if (args.length == 4 && args[0].equals("--check")) {
			FeatureModelDimacsExport export = FeatureModelDimacsExport.of(new File(args[1]));
			PhiD reference = DimacsPhiD.read(new File(args[2]), new File(args[3]), new File(args[1]));
			System.out.println(export.equivalenceReport(reference));
			return;
		}
		if (args.length != 3) {
			System.err.println("Usage: FeatureModelDimacsExport model.xml out.dimacs out_mapping.txt");
			System.exit(1);
		}
		FeatureModelDimacsExport export = FeatureModelDimacsExport.of(new File(args[0]));
		export.write(new File(args[1]), new File(args[2]), args[0]);
		System.out.println("Wrote " + args[1] + " (" + export.ids.size() + " variables, " + export.clauses.size()
				+ " clauses) and " + args[2]);
	}

	public static FeatureModelDimacsExport of(File modelXml) throws IOException {
		try {
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(modelXml);
			FeatureModelDimacsExport export = new FeatureModelDimacsExport();
			Element struct = (Element) document.getElementsByTagName("struct").item(0);
			List<Element> roots = featureChildren(struct);
			if (roots.size() != 1) {
				throw new IOException(modelXml + ": expected one root feature, found " + roots.size());
			}
			export.number(roots.get(0));
			export.clauses.add(new int[] { export.id(roots.get(0)) });
			export.structure(roots.get(0));
			NodeList rules = document.getElementsByTagName("rule");
			for (int i = 0; i < rules.getLength(); i++) {
				List<Element> expression = elementChildren(rules.item(i));
				if (expression.size() != 1) {
					throw new IOException(modelXml + ": rule with " + expression.size() + " expressions");
				}
				export.clauses.addAll(export.cnf(expression.get(0), true));
			}
			return export;
		} catch (IOException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new IOException("Cannot read " + modelXml, ex);
		}
	}

	// ------------------------------------------------------------ structure

	private void number(Element feature) {
		String name = feature.getAttribute("name");
		if (ids.containsKey(name)) {
			throw new IllegalStateException("Feature " + name + " occurs twice");
		}
		ids.put(name, ids.size() + 1);
		for (Element child : featureChildren(feature)) {
			number(child);
		}
	}

	private void structure(Element parent) {
		int p = id(parent);
		List<Element> children = featureChildren(parent);
		int[] group = new int[children.size() + 1];
		group[0] = -p;
		for (int i = 0; i < children.size(); i++) {
			Element child = children.get(i);
			int c = id(child);
			group[i + 1] = c;
			clauses.add(new int[] { -c, p });
			if (parent.getTagName().equals("and") && "true".equals(child.getAttribute("mandatory"))) {
				clauses.add(new int[] { -p, c });
			}
			structure(child);
		}
		if (children.isEmpty()) {
			return;
		}
		if (parent.getTagName().equals("or") || parent.getTagName().equals("alt")) {
			clauses.add(group);
		}
		if (parent.getTagName().equals("alt")) {
			for (int i = 1; i < group.length; i++) {
				for (int j = i + 1; j < group.length; j++) {
					clauses.add(new int[] { -group[i], -group[j] });
				}
			}
		}
	}

	private int id(Element feature) {
		return id(feature.getAttribute("name"));
	}

	private int id(String name) {
		Integer id = ids.get(name);
		if (id == null) {
			throw new IllegalStateException("Constraint names feature " + name + ", which is not in the tree");
		}
		return id;
	}

	// ---------------------------------------------------------- constraints

	/** CNF of the expression (positive) or of its negation, by distribution. */
	private List<int[]> cnf(Element node, boolean positive) {
		String tag = node.getTagName();
		List<Element> operands = elementChildren(node);
		switch (tag) {
		case "var":
			return single(positive ? id(node.getTextContent().trim()) : -id(node.getTextContent().trim()));
		case "not":
			return cnf(operands.get(0), !positive);
		case "conj":
			return positive ? and(operands, true) : or(operands, false);
		case "disj":
			return positive ? or(operands, true) : and(operands, false);
		case "imp": {
			// a -> b  ==  !a | b ;  !(a -> b)  ==  a & !b
			List<int[]> a = cnf(operands.get(0), !positive);
			List<int[]> b = cnf(operands.get(1), positive);
			return positive ? distribute(a, b) : concat(a, b);
		}
		case "eq": {
			// a <-> b  ==  (!a | b) & (a | !b) ;  !(a <-> b)  ==  (a | b) & (!a | !b)
			Element a = operands.get(0);
			Element b = operands.get(1);
			if (positive) {
				return concat(distribute(cnf(a, false), cnf(b, true)), distribute(cnf(a, true), cnf(b, false)));
			}
			return concat(distribute(cnf(a, true), cnf(b, true)), distribute(cnf(a, false), cnf(b, false)));
		}
		default:
			throw new IllegalStateException("Unsupported constraint element <" + tag + ">");
		}
	}

	private List<int[]> and(List<Element> operands, boolean positive) {
		List<int[]> result = new ArrayList<int[]>();
		for (Element operand : operands) {
			result.addAll(cnf(operand, positive));
		}
		return result;
	}

	private List<int[]> or(List<Element> operands, boolean positive) {
		List<int[]> result = null;
		for (Element operand : operands) {
			List<int[]> next = cnf(operand, positive);
			result = result == null ? next : distribute(result, next);
		}
		return result;
	}

	/** CNF of (A | B) for CNFs A and B. */
	private static List<int[]> distribute(List<int[]> a, List<int[]> b) {
		List<int[]> result = new ArrayList<int[]>();
		for (int[] x : a) {
			for (int[] y : b) {
				int[] clause = Arrays.copyOf(x, x.length + y.length);
				System.arraycopy(y, 0, clause, x.length, y.length);
				result.add(clause);
			}
		}
		return result;
	}

	private static List<int[]> concat(List<int[]> a, List<int[]> b) {
		List<int[]> result = new ArrayList<int[]>(a);
		result.addAll(b);
		return result;
	}

	private static List<int[]> single(int literal) {
		List<int[]> result = new ArrayList<int[]>();
		result.add(new int[] { literal });
		return result;
	}

	// ---------------------------------------------------------------- output

	public void write(File dimacs, File mapping, String source) throws IOException {
		try (PrintWriter out = new PrintWriter(dimacs, "UTF-8")) {
			out.println("c DIMACS of " + source + ", standard feature-model semantics, written by "
					+ getClass().getName());
			out.println("p cnf " + ids.size() + " " + clauses.size());
			for (int[] clause : clauses) {
				StringBuilder line = new StringBuilder();
				for (int literal : clause) {
					line.append(literal).append(' ');
				}
				out.println(line.append('0'));
			}
		}
		try (PrintWriter out = new PrintWriter(mapping, "UTF-8")) {
			out.println("ID:FeatureName");
			for (Map.Entry<String, Integer> entry : ids.entrySet()) {
				out.println(entry.getValue() + ":" + entry.getKey());
			}
		}
	}

	/**
	 * Same variable names, and every clause of each formula implied by the other
	 * (one satisfiability call per clause): the two formulas are equivalent.
	 */
	public String equivalenceReport(PhiD reference) throws Exception {
		if (!new java.util.HashSet<String>(reference.getFeatureNames()).equals(ids.keySet())) {
			return "NOT EQUIVALENT: the variable names differ";
		}
		int[] toReference = new int[ids.size() + 1];
		for (Map.Entry<String, Integer> entry : ids.entrySet()) {
			toReference[entry.getValue()] = reference.variableOf(entry.getKey());
		}
		List<int[]> translated = new ArrayList<int[]>();
		for (int[] clause : clauses) {
			int[] t = new int[clause.length];
			for (int i = 0; i < clause.length; i++) {
				t[i] = clause[i] > 0 ? toReference[clause[i]] : -toReference[-clause[i]];
			}
			translated.add(t);
		}
		PhiD mine = new PhiD("export", reference.getFeatureNames(), translated, java.util.Collections.<String>emptySet());
		int mineNotImplied = notImplied(reference, translated);
		int referenceNotImplied = notImplied(mine, reference.getClauses());
		return (mineNotImplied == 0 && referenceNotImplied == 0 ? "EQUIVALENT" : "NOT EQUIVALENT") + ": "
				+ ids.size() + " variables; export " + clauses.size() + " clauses, " + mineNotImplied
				+ " not implied by the reference; reference " + reference.getClauses().size() + " clauses, "
				+ referenceNotImplied + " not implied by the export";
	}

	private static int notImplied(PhiD by, List<int[]> clauses) throws Exception {
		org.sat4j.specs.ISolver solver = ProductEnumerator.solverFor(by);
		int count = 0;
		for (int[] clause : clauses) {
			int[] negated = new int[clause.length];
			for (int i = 0; i < clause.length; i++) {
				negated[i] = -clause[i];
			}
			if (solver != null && solver.isSatisfiable(new org.sat4j.core.VecInt(negated))) {
				count++;
			}
		}
		return count;
	}

	// ------------------------------------------------------------------- xml

	private static List<Element> featureChildren(Node node) {
		List<Element> result = new ArrayList<Element>();
		for (Element child : elementChildren(node)) {
			String tag = child.getTagName();
			if (tag.equals("and") || tag.equals("or") || tag.equals("alt") || tag.equals("feature")) {
				result.add(child);
			}
		}
		return result;
	}

	private static List<Element> elementChildren(Node node) {
		List<Element> result = new ArrayList<Element>();
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element && !((Element) child).getTagName().equals("graphics")
					&& !((Element) child).getTagName().equals("description")) {
				result.add((Element) child);
			}
		}
		return result;
	}
}
