package tr.edu.iyte.esgfx.productconfigurationgeneration.dimacs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tr.edu.iyte.esgfx.cases.CaseStudyUtilities;
import tr.edu.iyte.esgfx.model.featuremodel.Connector;
import tr.edu.iyte.esgfx.model.featuremodel.ConnectorAND;
import tr.edu.iyte.esgfx.model.featuremodel.ConnectorOR;
import tr.edu.iyte.esgfx.model.featuremodel.Feature;
import tr.edu.iyte.esgfx.model.featuremodel.Implicant;
import tr.edu.iyte.esgfx.model.featuremodel.Implication;

public class FeatureModelToDimacsExporter extends CaseStudyUtilities {

    public void exportToDIMACS() throws Exception {
        generateFeatureExpressionMapFromFeatureModel(featureModelFile, ESGFxFile);

        Map<Feature, Integer> featureToId = new LinkedHashMap<>();
        List<Feature> concreteFeatures = new ArrayList<>();
        int idCounter = 1;
        
        for (Feature f : featureModel.getFeatureSet()) {
            featureToId.put(f, idCounter++);
            if (!f.isAbstract()) {
                concreteFeatures.add(f);
            }
        }

        List<String> clauses = new ArrayList<>();

        Feature root = featureModel.getRoot();
        if (root != null && featureToId.containsKey(root)) {
            clauses.add(featureToId.get(root) + " 0");
        }

        for (Feature child : featureModel.getFeatureSet()) {
            Feature parent = child.getParent();

            if (parent != null && featureToId.containsKey(parent)) {
                int cId = featureToId.get(child);
                int pId = featureToId.get(parent);

                clauses.add("-" + cId + " " + pId + " 0");

                if (child.isMandatory() && !isChildInGroup(child, featureModel.getORFeatures()) && !isChildInGroup(child, featureModel.getXORFeatures())) {
                    clauses.add("-" + pId + " " + cId + " 0");
                }
            }
        }

        for (Map.Entry<Feature, Set<Feature>> entry : featureModel.getORFeatures().entrySet()) {
            Feature parent = entry.getKey();
            Set<Feature> children = entry.getValue();

            if (featureToId.containsKey(parent) && children != null && !children.isEmpty()) {
                StringBuilder orClause = new StringBuilder("-" + featureToId.get(parent));
                for (Feature child : children) {
                    if (featureToId.containsKey(child)) {
                        orClause.append(" ").append(featureToId.get(child));
                    }
                }
                orClause.append(" 0");
                clauses.add(orClause.toString());
            }
        }

        for (Map.Entry<Feature, Set<Feature>> entry : featureModel.getXORFeatures().entrySet()) {
            Feature parent = entry.getKey();
            Set<Feature> children = entry.getValue();

            if (featureToId.containsKey(parent) && children != null && !children.isEmpty()) {
                StringBuilder xorClause = new StringBuilder("-" + featureToId.get(parent));
                List<Feature> childList = new ArrayList<>();

                for (Feature child : children) {
                    if (featureToId.containsKey(child)) {
                        xorClause.append(" ").append(featureToId.get(child));
                        childList.add(child);
                    }
                }
                xorClause.append(" 0");
                clauses.add(xorClause.toString());

                for (int i = 0; i < childList.size(); i++) {
                    for (int j = i + 1; j < childList.size(); j++) {
                        clauses.add("-" + featureToId.get(childList.get(i)) + " -" + featureToId.get(childList.get(j)) + " 0");
                    }
                }
            }
        }

        addConnectorConstraintClauses(clauses, featureToId);
        addImplicationConstraintClauses(clauses, featureToId);

        writeDimacsFile(DIMACSFile, featureToId.size(), clauses, concreteFeatures, featureToId);
        writeMappingFile(featureToId);
    }
    
    private boolean isChildInGroup(Feature child, Map<Feature, Set<Feature>> groupMap) {
        for (Set<Feature> children : groupMap.values()) {
            if (children.contains(child)) {
                return true;
            }
        }
        return false;
    }

    private void addConnectorConstraintClauses(List<String> clauses, Map<Feature, Integer> featureToId) {
        Set<Connector> connConstraints = featureModel.getConnConstraints();
        if (connConstraints == null) return;

        for (Connector connConstraint : connConstraints) {
            Set<Feature> featureSet = collectLeafFeatures(connConstraint);
            String operator = connConstraint.getOperator();

            if (operator.equals("AND")) {
                for (Feature feature : featureSet) {
                    if (featureToId.containsKey(feature)) {
                        clauses.add(featureToId.get(feature) + " 0");
                    }
                }
            } else if (operator.equals("OR")) {
                StringBuilder orClause = new StringBuilder();
                for (Feature feature : featureSet) {
                    if (featureToId.containsKey(feature)) {
                        orClause.append(featureToId.get(feature)).append(" ");
                    }
                }
                if (orClause.length() > 0) {
                    orClause.append("0");
                    clauses.add(orClause.toString());
                }
            }
        }
    }

    private void addImplicationConstraintClauses(List<String> clauses, Map<Feature, Integer> featureToId) {
        Set<Implication> impConstraints = featureModel.getImpConstraints();
        if (impConstraints == null) return;
        
        addIffConstraintClauses(impConstraints);

        for (Implication impConstraint : impConstraints) {
            String lhsType = impConstraint.getLHStype();
            String rhsType = impConstraint.getRHStype();
            Implicant lhsImplicant = impConstraint.getLeftHandSide();
            Implicant rhsImplicant = impConstraint.getRightHandSide();

            if (lhsType.equals("var") && rhsType.equals("var")) {
                Feature lhsFeature = (Feature) lhsImplicant;
                Feature rhsFeature = (Feature) rhsImplicant;
                clauses.add("-" + featureToId.get(lhsFeature) + " " + featureToId.get(rhsFeature) + " 0");
            } else if (lhsType.equals("var") && rhsType.equals("disj")) {
                Feature lhsFeature = (Feature) lhsImplicant;
                Connector rhsDisjunction = (ConnectorOR) rhsImplicant;
                Set<Feature> disjunctionFeatures = collectLeafFeatures(rhsDisjunction);
                StringBuilder clause = new StringBuilder("-" + featureToId.get(lhsFeature) + " ");
                for (Feature f : disjunctionFeatures) {
                    clause.append(featureToId.get(f)).append(" ");
                }
                clause.append("0");
                clauses.add(clause.toString());
            } else if (lhsType.equals("var") && rhsType.equals("conj")) {
                Feature lhsFeature = (Feature) lhsImplicant;
                Connector rhsConjunction = (ConnectorAND) rhsImplicant;
                Set<Feature> conjunctionFeatures = collectLeafFeatures(rhsConjunction);
                for (Feature f : conjunctionFeatures) {
                    clauses.add("-" + featureToId.get(lhsFeature) + " " + featureToId.get(f) + " 0");
                }
            } else if (lhsType.equals("disj") && rhsType.equals("var")) {
                Connector lhsDisjunction = (ConnectorOR) lhsImplicant;
                Set<Feature> disjunctionFeatures = collectLeafFeatures(lhsDisjunction);
                Feature rhsFeature = (Feature) rhsImplicant;
                for (Feature f : disjunctionFeatures) {
                    clauses.add("-" + featureToId.get(f) + " " + featureToId.get(rhsFeature) + " 0");
                }
            } else if (lhsType.equals("disj") && rhsType.equals("disj")) {
                Connector lhsDisjunction = (ConnectorOR) lhsImplicant;
                Set<Feature> lhsDisjunctionFeatures = collectLeafFeatures(lhsDisjunction);
                Connector rhsDisjunction = (ConnectorOR) rhsImplicant;
                Set<Feature> rhsDisjunctionFeatures = collectLeafFeatures(rhsDisjunction);
                for (Feature lhsF : lhsDisjunctionFeatures) {
                    StringBuilder clause = new StringBuilder("-" + featureToId.get(lhsF) + " ");
                    for (Feature rhsF : rhsDisjunctionFeatures) {
                        clause.append(featureToId.get(rhsF)).append(" ");
                    }
                    clause.append("0");
                    clauses.add(clause.toString());
                }
            } else if (lhsType.equals("disj") && rhsType.equals("conj")) {
                Connector lhsDisjunction = (ConnectorOR) lhsImplicant;
                Set<Feature> lhsDisjunctionFeatures = collectLeafFeatures(lhsDisjunction);
                Connector rhsConjunction = (ConnectorAND) rhsImplicant;
                Set<Feature> rhsConjunctionFeatures = collectLeafFeatures(rhsConjunction);
                for (Feature lhsF : lhsDisjunctionFeatures) {
                    for (Feature rhsF : rhsConjunctionFeatures) {
                        clauses.add("-" + featureToId.get(lhsF) + " " + featureToId.get(rhsF) + " 0");
                    }
                }
            } else if (lhsType.equals("conj") && rhsType.equals("var")) {
                Connector lhsConjunction = (ConnectorAND) lhsImplicant;
                Set<Feature> lhsConjunctionFeatures = collectLeafFeatures(lhsConjunction);
                Feature rhsFeature = (Feature) rhsImplicant;
                StringBuilder clause = new StringBuilder();
                for (Feature lhsF : lhsConjunctionFeatures) {
                    clause.append("-").append(featureToId.get(lhsF)).append(" ");
                }
                clause.append(featureToId.get(rhsFeature)).append(" 0");
                clauses.add(clause.toString());
            } else if (lhsType.equals("conj") && rhsType.equals("disj")) {
                Connector lhsConjunction = (ConnectorAND) lhsImplicant;
                Set<Feature> lhsConjunctionFeatures = collectLeafFeatures(lhsConjunction);
                Connector rhsDisjunction = (ConnectorOR) rhsImplicant;
                Set<Feature> rhsDisjunctionFeatures = collectLeafFeatures(rhsDisjunction);
                StringBuilder clause = new StringBuilder();
                for (Feature lhsF : lhsConjunctionFeatures) {
                    clause.append("-").append(featureToId.get(lhsF)).append(" ");
                }
                for (Feature rhsF : rhsDisjunctionFeatures) {
                    clause.append(featureToId.get(rhsF)).append(" ");
                }
                clause.append("0");
                clauses.add(clause.toString());
            } else if (lhsType.equals("conj") && rhsType.equals("conj")) {
                Connector lhsConjunction = (ConnectorAND) lhsImplicant;
                Set<Feature> lhsConjunctionFeatures = collectLeafFeatures(lhsConjunction);
                Connector rhsConjunction = (ConnectorAND) rhsImplicant;
                Set<Feature> rhsConjunctionFeatures = collectLeafFeatures(rhsConjunction);
                for (Feature rhsF : rhsConjunctionFeatures) {
                    StringBuilder clause = new StringBuilder();
                    for (Feature lhsF : lhsConjunctionFeatures) {
                        clause.append("-").append(featureToId.get(lhsF)).append(" ");
                    }
                    clause.append(featureToId.get(rhsF)).append(" 0");
                    clauses.add(clause.toString());
                }
            }
        }
    }

    private void addIffConstraintClauses(Set<Implication> impConstraints) {
        Set<Implication> iffConstraints = featureModel.getIffConstraints();
        if (iffConstraints == null || iffConstraints.isEmpty()) return;

        for (Implication iffConstraint : iffConstraints) {
            String iffLHSType = iffConstraint.getLHStype();
            String iffRHSType = iffConstraint.getRHStype();
            Implicant lhsImplicant = iffConstraint.getLeftHandSide();
            Implicant rhsImplicant = iffConstraint.getRightHandSide();

            Implication implication1 = new Implication(lhsImplicant, rhsImplicant);
            implication1.setLHStype(iffLHSType);
            implication1.setRHStype(iffRHSType);

            Implication implication2 = new Implication(rhsImplicant, lhsImplicant);
            implication2.setLHStype(iffRHSType);
            implication2.setRHStype(iffLHSType);

            impConstraints.add(implication1);
            impConstraints.add(implication2);
        }
    }

    private Set<Feature> collectLeafFeatures(Implicant implicant) {
        Set<Feature> leaves = new LinkedHashSet<>();
        if (implicant instanceof Feature) {
            leaves.add((Feature) implicant);
        } else if (implicant instanceof Connector) {
            Connector connector = (Connector) implicant;
            for (Implicant child : connector.getImplicantSet()) { 
                if (child instanceof Feature) {
                    leaves.add((Feature) child);
                } else if (child instanceof Connector) {
                    leaves.addAll(collectLeafFeatures(child));
                }
            }
        }
        return leaves;
    }

    private void writeDimacsFile(String path, int numVars, List<String> clauses, List<Feature> concreteFeatures,
            Map<Feature, Integer> featureToId) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(path))) {
            writer.write("c DIMACS Generated directly from Feature Model with Cross-Tree Constraints\n");
            
            writer.write("p cnf " + numVars + " " + clauses.size() + "\n");
            for (String clause : clauses) {
                writer.write(clause + "\n");
            }
        }
    }
    
    private void writeMappingFile(Map<Feature, Integer> featureToId) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(DIMACSMappingFile))) {
            writer.write("ID:FeatureName\n");
            for (Map.Entry<Feature, Integer> entry : featureToId.entrySet()) {
                writer.write(entry.getValue() + ":" + entry.getKey().getName() + "\n");
            }
        }
    }
}