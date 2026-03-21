package tr.edu.iyte.esgfx.testgeneration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.cases.CaseStudyUtilities;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.testgeneration.guitar.GuitarOutputToEventSequenceParser;

public class FileToTestSuiteConverter extends CaseStudyUtilities {

    // ---- Original method (unchanged) ----

    public static Set<EventSequence> loadTestSequencesFromFile(String productName, String approach, ESG productESGFx) {
        if (approach.startsWith("EFG")) {
            return loadEFGTestSequences(productName, approach, productESGFx);
        } else {
            return loadESGFxTestSequences(productName, approach, productESGFx);
        }
    }

    // ---- NEW: Seed-aware overload for multi-seed Random Walk ----

    /**
     * Loads test sequences for a specific Random Walk seed.
     * 
     * For approach "ESG-Fx_L0", loads from: testsequences/L0/seed{seed}/{productName}_RandomWalk.txt
     * If the seed folder doesn't exist and seed == 42, falls back to the original location.
     * For all other approaches, delegates to the original method (seed is ignored).
     */
    public static Set<EventSequence> loadTestSequencesFromFile(String productName, String approach, ESG productESGFx, long seed) {
        if (!approach.equals("ESG-Fx_L0")) {
            return loadTestSequencesFromFile(productName, approach, productESGFx);
        }

        String seedPath = testsequencesFolder + "L0/seed" + seed + "/" + productName + "_RandomWalk.txt";
        File seedFile = new File(seedPath);

        // Fallback for seed 42: try the original L0 location if seed folder doesn't exist
        if (!seedFile.exists() && seed == 42L) {
            String originalPath = testsequencesFolder + "L0/" + productName + "_RandomWalk.txt";
            seedFile = new File(originalPath);
        }

        if (!seedFile.exists()) {
            return new LinkedHashSet<>();
        }

        return parseESGFxTestFile(seedFile, productESGFx, "L0");
    }

    // ---- NEW: Damping-factor-aware overload for sensitivity analysis ----

    /**
     * Loads Random Walk test sequences generated with a specific damping factor.
     * 
     * Loads from: testsequences/L0/damping{NNN}/{productName}_RandomWalk.txt
     * where NNN is the damping factor × 100 (e.g., 080, 085, 090).
     */
    public static Set<EventSequence> loadTestSequencesForDamping(String productName, ESG productESGFx, double dampingFactor) {
        String dampingFolder = String.format("%03d", (int) (dampingFactor * 100));
        String dampingPath = testsequencesFolder + "L0/damping" + dampingFolder + "/" + productName + "_RandomWalk.txt";
        File dampingFile = new File(dampingPath);

        if (!dampingFile.exists()) {
            return new LinkedHashSet<>();
        }

        return parseESGFxTestFile(dampingFile, productESGFx, "L0");
    }

    // ---- Existing private methods (refactored to reuse) ----

    private static Set<EventSequence> loadEFGTestSequences(String productName, String approach, ESG productESGFx) {
        Set<EventSequence> sequences = new LinkedHashSet<>();
        String lLevelStr = approach.split("_")[1]; 
        String efgFilePath = EFGFolder + productName + ".EFG";
        String efgTestFolder = efg_testsequencesFolder + productName + "/" + lLevelStr + "/";
        
        try {
            return GuitarOutputToEventSequenceParser.parseGuitarTests(efgTestFolder, efgFilePath, (ESGFx) productESGFx);
        } catch (Exception e) {
            System.err.println("Error reading EFG tests for " + productName + " - " + e.getMessage());
            return sequences;
        }
    }

    private static Set<EventSequence> loadESGFxTestSequences(String productName, String approach, ESG productESGFx) {
        Set<EventSequence> sequences = new LinkedHashSet<>();

        String subFolder = "";
        String fileName = "";

        if (approach.equals("ESG-Fx_L0")) {
            subFolder = "L0";
            fileName = productName + "_RandomWalk.txt";
        } else if (approach.equals("ESG-Fx_L1")) {
            subFolder = "L1";
            fileName = productName + "_L1.txt";
        } else if (approach.equals("ESG-Fx_L2")) {
            subFolder = "L2";
            fileName = productName + "_L2.txt";
        } else if (approach.equals("ESG-Fx_L3")) {
            subFolder = "L3";
            fileName = productName + "_L3.txt";
        } else if (approach.equals("ESG-Fx_L4")) {
            subFolder = "L4";
            fileName = productName + "_L4.txt";
        } else {
            System.err.println("Unknown testing approach provided: " + approach);
            return sequences;
        }

        String fullPath = testsequencesFolder + subFolder + "/" + fileName;
        File file = new File(fullPath);
        
        if (!file.exists()) {
            return sequences;
        }

        return parseESGFxTestFile(file, productESGFx, subFolder);
    }

    /**
     * Parses an ESG-Fx test file. Extracted from loadESGFxTestSequences so it can
     * be reused by the seed-aware overload without duplicating parsing logic.
     */
    private static Set<EventSequence> parseESGFxTestFile(File file, ESG productESGFx, String subFolder) {
        Set<EventSequence> sequences = new LinkedHashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.contains("is ")) continue;

                String[] mainParts = line.split(" : ");
                if (mainParts.length < 2) continue;

                String seqData = mainParts[1].trim();
                
                String[] tokens = seqData.split(", ");

                EventSequence es = new EventSequence();
                List<Vertex> vList = new ArrayList<>();

                if (subFolder.equals("L0") || subFolder.equals("L1") || subFolder.equals("L2")) {
                    for (String token : tokens) {
                        String cleanName = getBaseName(token.trim());
                        Vertex v = findVertexByCleanName(productESGFx, cleanName);
                        if (v != null) vList.add(v);
                    }
                } else {
                    for (int i = 0; i < tokens.length; i++) {
                        String token = tokens[i].trim();
                        String[] parts = token.split(":");

                        if (i == 0) {
                            for (String part : parts) {
                                String cleanName = getBaseName(part.trim());
                                Vertex v = findVertexByCleanName(productESGFx, cleanName);
                                if (v != null) vList.add(v);
                            }
                        } else {
                            String lastPart = parts[parts.length - 1];
                            String cleanName = getBaseName(lastPart.trim());
                            Vertex v = findVertexByCleanName(productESGFx, cleanName);
                            if (v != null) vList.add(v);
                        }
                    }
                }

                if (!vList.isEmpty()) {
                    es.setEventSequence(vList);
                    sequences.add(es);
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading " + file.getAbsolutePath() + " - " + e.getMessage());
        }

        return sequences;
    }

    private static String getBaseName(String part) {
        String cleanStr = part.replaceAll(" ", "_");
        int lastUnderscore = cleanStr.lastIndexOf('_');
        if (lastUnderscore != -1) {
            String possibleNum = cleanStr.substring(lastUnderscore + 1);
            if (possibleNum.matches("\\d+")) {
                return cleanStr.substring(0, lastUnderscore);
            }
        }
        return cleanStr;
    }

    private static Vertex findVertexByCleanName(ESG productESGFx, String cleanName) {
        for (Vertex v : productESGFx.getRealVertexList()) {
            String vName = v.getEvent().getName().trim().replaceAll(" ", "_");
            
            int lastUnderscore = vName.lastIndexOf('_');
            if (lastUnderscore != -1) {
                String possibleNum = vName.substring(lastUnderscore + 1);
                if (possibleNum.matches("\\d+")) {
                    vName = vName.substring(0, lastUnderscore);
                }
            }
            
            if (vName.equalsIgnoreCase(cleanName)) {
                return v;
            }
        }
        return null;
    }
}