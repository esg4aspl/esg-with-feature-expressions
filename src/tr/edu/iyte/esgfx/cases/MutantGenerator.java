package tr.edu.iyte.esgfx.cases;

import java.util.List;
import java.util.Set;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;
import org.sat4j.tools.ModelIterator;

import tr.edu.iyte.esg.eventsequence.EventSequence;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.sequenceesgfx.EventSequenceUtilities;
import tr.edu.iyte.esgfx.mutationtesting.faultdetection.FaultDetector;
import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.EulerCycleToTestSequenceGenerator;
import tr.edu.iyte.esgfx.testgeneration.edgecoverage.EulerCycleGeneratorForEdgeCoverage;
import tr.edu.iyte.esgfx.testgeneration.eventtriplecoverage.TransformedESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.util.StronglyConnectedBalancedESGFxGeneration;

public class MutantGenerator extends CaseStudyUtilities {

	protected FaultDetector generateFaultDetector(ESG productESGFx, int covarage) {

		Set<EventSequence> tests = buildTests(productESGFx, covarage);
		Set<EventSequence> newTests = EventSequenceUtilities.removeRepetitionsFromEventSequenceSet(covarage, tests);
		FaultDetector detector = new FaultDetector(newTests);

		return detector;

	}

	private Set<EventSequence> buildTests(ESG productESGFx, int L) {
		TransformedESGFxGenerator t = new TransformedESGFxGenerator();
		ESG transformed = t.generateTransformedESGFx(L, productESGFx);
		ESG scb = StronglyConnectedBalancedESGFxGeneration.getStronglyConnectedBalancedESGFxGeneration(transformed);
		EulerCycleGeneratorForEdgeCoverage ec = new EulerCycleGeneratorForEdgeCoverage();
		ec.generateEulerCycle(scb);
		List<Vertex> cycle = ec.getEulerCycle();
		EulerCycleToTestSequenceGenerator ts = new EulerCycleToTestSequenceGenerator();
		return ts.CESgenerator(cycle);
	}

	public static double percentageOfFaultDetection(int numberOfMutants, int numberOfDetectedMutants) {

		int undetectedMutantNumber = numberOfMutants - numberOfDetectedMutants;

		double percentage = ((double) undetectedMutantNumber) / ((double) numberOfMutants) * 100.0;

		if (undetectedMutantNumber == 0) {
			return 100.0;
		} else {
			// System.out.printf("Coverage %.2f %s\n", 100.0 - coverage, "%");
			return 100.0 - percentage;
		}

	}

}
