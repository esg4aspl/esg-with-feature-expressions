package tr.edu.iyte.esgfx.staticanalysis;

import java.io.File;
import java.io.IOException;

import org.sat4j.specs.ContradictionException;

import tr.edu.iyte.esgfx.api.FeatureModelCnf;
import tr.edu.iyte.esgfx.api.LoadedSplModel;

/**
 * Where Phi_d comes from. {@link #ENGINE}: the clauses of
 * {@code SATSolverGenerationFromFeatureModel}, recorded by
 * {@code FeatureModelCnf}, which the enumeration of Chapter 6 used.
 * {@link #DIMACS}: FeatureIDE's DIMACS export of the feature model. Both mark
 * the features that model.xml declares abstract as non-concrete.
 */
public enum PhiDSource {

	ENGINE("engine"), DIMACS("dimacs");

	private final String label;

	PhiDSource(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public PhiD load(CaseStudy study, LoadedSplModel model) throws IOException, ContradictionException {
		File modelXml = study.modelXml();
		if (this == ENGINE) {
			return PhiD.of(FeatureModelCnf.of(model), DimacsPhiD.abstractFeatures(modelXml));
		}
		return DimacsPhiD.read(study.dimacs(), study.dimacsMapping(), modelXml);
	}

	public static PhiDSource parse(String text) {
		for (PhiDSource source : values()) {
			if (source.label.equalsIgnoreCase(text)) {
				return source;
			}
		}
		throw new IllegalArgumentException("Unknown Phi_d source '" + text + "'; expected engine or dimacs");
	}
}
