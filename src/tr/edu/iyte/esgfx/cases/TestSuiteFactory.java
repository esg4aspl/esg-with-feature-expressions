package tr.edu.iyte.esgfx.cases;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esg.model.sequenceesg.Sequence;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.sequenceesgfx.VertexRefinedBySequence;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EulerCycleGeneratorForEdgeCoverage;
import tr.edu.iyte.esgfx.testgeneration.eventcoverage.EulerCycleGeneratorForEventCoverage;
import tr.edu.iyte.esgfx.testgeneration.eventtriplecoverage.TransformedESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.randomwalktesting.RandomWalkTestGenerator;
import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;

// =====================================================================
// IN-MEMORY MODE — SHARED HELPER BEGIN
// =====================================================================
//
// Produces ESG-Fx (L=1..L=4) and Random-Walk test suites directly from
// the in-memory MOGA graph. Vertex references are preserved end-to-end:
// every Vertex in the returned EventSequences is an original MOGA vertex
// instance (the same VertexRefinedByFeatureExpression objects parsed
// from DOT). This bypasses the on-disk name-lookup layer entirely and
// eliminates the name-collision failure mode.
//
// EFG suites cannot be produced in-memory (GUITAR is an external tool);
// they are still loaded from .tst files in both file and memory modes.
//
// Generator wiring mirrors the RQ1 pipeline classes:
//   - L = 1 : RQ1_ComparativeEfficiency_ESGFx_L1
//             EulerCycleGeneratorForEventCoverage on the original
//             (balanced) productESGFx.  The Euler cycle visits original
//             MOGA vertices directly, so no decomposition is needed.
//
//   - L >= 2: RQ1_ComparativeEfficiency_ESGFx_L234
//             TransformedESGFxGenerator -> balanced ->
//             EulerCycleGeneratorForEdgeCoverage.  The Euler cycle
//             visits VertexRefinedBySequence instances of the
//             transformed graph; each carries an inner Sequence<Vertex>
//             of original MOGA vertices.  We unfold those inner
//             sequences into a flat MOGA-vertex chain using the same
//             rule that FileToTestSuiteConverter.parseESGFxTestFile
//             applies on disk:
//               * first sequence vertex contributes its full inner
//                 sequence,
//               * subsequent sequence vertices contribute only the
//                 last inner element (the rest overlaps).
//             The result is the same MOGA-grounded chain the file
//             pipeline would have produced, with object identity
//             preserved.
//
// =====================================================================
public final class TestSuiteFactory {

    private TestSuiteFactory() { /* static helpers only */ }

    /**
     * Default safety limit for Random-Walk generation, matching
     * RQ3_RandomWalkMultiSeed_TestGenerator and the damping generator.
     */
    public static int defaultRwSafetyLimit(ESG productESGFx) {
        int vertexCount = productESGFx.getVertexList().size();
        return Math.min(5 * vertexCount * vertexCount * vertexCount, 2_000_000);
    }

    /**
     * Generate an ESG-Fx test suite for a given coverage length L >= 1.
     * The feature-expression map must be the per-product map (i.e. after
     * updateFeatureExpressionMapFromConfigFile has been applied for the
     * current product), otherwise feature-aware compatibility checks
     * inside the Euler-cycle generators will not match the on-disk run.
     */
    public static Set<EventSequence> generateESGFxSuite(
            ESG productESGFx,
            int L,
            Map<String, FeatureExpression> featureExpressionMap) {

        if (L < 1) {
            return new LinkedHashSet<>();
        }

        if (L == 1) {
            // Mirrors RQ1_ComparativeEfficiency_ESGFx_L1.
            ESG balanced = StronglyConnectedBalancedESGFxGeneration
                    .getStronglyConnectedBalancedESGFxGeneration(productESGFx);

            EulerCycleGeneratorForEventCoverage eulerGen =
                    new EulerCycleGeneratorForEventCoverage(featureExpressionMap);
            eulerGen.generateEulerCycle(balanced);
            List<Vertex> eulerCycle = eulerGen.getEulerCycle();

            EulerCycleToTestSequenceGenerator cesGen = new EulerCycleToTestSequenceGenerator();
            Set<EventSequence> suite = cesGen.CESgenerator(eulerCycle);

            cesGen.reset();
            eulerGen.reset();
            balanced = null;
            // L=1 visits original MOGA vertices directly; no decomposition.
            return suite != null ? suite : new LinkedHashSet<>();
        }

        // L >= 2 : transformed graph + edge-coverage Euler cycle.
        // Mirrors RQ1_ComparativeEfficiency_ESGFx_L234.
        TransformedESGFxGenerator transformer = new TransformedESGFxGenerator();
        ESG transformed = transformer.generateTransformedESGFx(L, productESGFx);
        ESG balanced = StronglyConnectedBalancedESGFxGeneration
                .getStronglyConnectedBalancedESGFxGeneration(transformed);
        transformed = null;

        EulerCycleGeneratorForEdgeCoverage eulerGen = new EulerCycleGeneratorForEdgeCoverage();
        eulerGen.generateEulerCycle(balanced);
        balanced = null;

        List<Vertex> eulerCycle = eulerGen.getEulerCycle();

        EulerCycleToTestSequenceGenerator cesGen = new EulerCycleToTestSequenceGenerator();
        Set<EventSequence> sequenceVertexSuite = cesGen.CESgenerator(eulerCycle);

        cesGen.reset();
        eulerGen.reset();

        if (sequenceVertexSuite == null || sequenceVertexSuite.isEmpty()) {
            return new LinkedHashSet<>();
        }

        return decomposeSequenceVertexSuite(sequenceVertexSuite);
    }

    /**
     * Generate a Random-Walk test suite for a given seed and damping factor.
     * Random Walk operates on the original MOGA graph, so the resulting
     * EventSequences already reference original MOGA vertices.
     */
    public static Set<EventSequence> generateRandomWalkSuite(
            ESG productESGFx, double dampingFactor, long seed) {
        int safetyLimit = defaultRwSafetyLimit(productESGFx);
        RandomWalkTestGenerator rw = new RandomWalkTestGenerator(
                (ESGFx) productESGFx, dampingFactor, seed);
        Set<EventSequence> suite = rw.generateWalkUntilEdgeCoverage(100.0, safetyLimit);
        return suite != null ? suite : new LinkedHashSet<>();
    }

    /**
     * Resolve the ESG-Fx coverage length from an approach label.
     * Returns -1 if the approach is not an in-memory-supported ESG-Fx variant.
     */
    public static int approachToLevel(String approach) {
        if (approach == null) return -1;
        switch (approach) {
            case "ESG-Fx_L1": return 1;
            case "ESG-Fx_L2": return 2;
            case "ESG-Fx_L3": return 3;
            case "ESG-Fx_L4": return 4;
            default: return -1;
        }
    }

    /**
     * Reads RQ3_MODE env var. Returns true if in-memory mode is active.
     * Default ("file") preserves the original disk-based behavior.
     */
    public static boolean isInMemoryMode() {
        String mode = System.getenv().getOrDefault("RQ3_MODE", "memory").trim().toLowerCase();
        return "memory".equals(mode) || "in-memory".equals(mode) || "in_memory".equals(mode);
    }

    // -----------------------------------------------------------------
    // Decomposition helper for L >= 2
    // -----------------------------------------------------------------

    /**
     * Convert each EventSequence-of-VertexRefinedBySequence into an
     * EventSequence-of-original-MOGA-Vertex.
     *
     * Rule (matches FileToTestSuiteConverter.parseESGFxTestFile):
     *   - First sequence vertex contributes ALL its inner sequence elements.
     *   - Subsequent sequence vertices contribute ONLY the last inner element
     *     (the rest overlaps with the previous sequence vertex's tail).
     */
    private static Set<EventSequence> decomposeSequenceVertexSuite(
            Set<EventSequence> sequenceVertexSuite) {
        Set<EventSequence> out = new LinkedHashSet<>();
        for (EventSequence es : sequenceVertexSuite) {
            if (es == null) continue;
            List<Vertex> seqVertices = es.getEventSequence();
            if (seqVertices == null || seqVertices.isEmpty()) continue;

            List<Vertex> mogaChain = new ArrayList<>();
            boolean first = true;
            for (Vertex v : seqVertices) {
                if (!(v instanceof VertexRefinedBySequence)) {
                    // Defensive fallback (shouldn't happen for L>=2, but harmless).
                    mogaChain.add(v);
                    first = false;
                    continue;
                }
                Sequence<Vertex> inner = ((VertexRefinedBySequence) v).getSequence();
                if (inner == null || inner.getSize() == 0) continue;

                if (first) {
                    for (int i = 0; i < inner.getSize(); i++) {
                        mogaChain.add(inner.getElement(i));
                    }
                    first = false;
                } else {
                    mogaChain.add(inner.getElement(inner.getSize() - 1));
                }
            }

            if (!mogaChain.isEmpty()) {
                EventSequence flat = new EventSequence();
                flat.setEventSequence(mogaChain);
                out.add(flat);
            }
        }
        return out;
    }
}
// =====================================================================
// IN-MEMORY MODE — SHARED HELPER END
// =====================================================================
