package tr.edu.iyte.esgfx.staticanalysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import tr.edu.iyte.esgfx.api.LoadedSplModel;
import tr.edu.iyte.esgfx.api.SingleProductTestGenerationAPI;

/** The eight product lines of the study, their files and their expected counts. */
public final class CaseStudy {

	public static final long NOT_ENUMERABLE = -1;

	public final String name;
	public final String folder;
	public final String prefix;
	/** Models of the DIMACS Phi_d over all features. */
	public final long configurations;
	/** Distinct assignments of the DIMACS Phi_d to the concrete features. */
	public final long products;
	/** The count of the Chapter 6 enumeration (engine clauses, all engine variables). */
	public final long chapter6;

	private CaseStudy(String name, String folder, String prefix, long configurations, long products, long chapter6) {
		this.name = name;
		this.folder = folder;
		this.prefix = prefix;
		this.configurations = configurations;
		this.products = products;
		this.chapter6 = chapter6;
	}

	public static final List<CaseStudy> ALL = Collections.unmodifiableList(Arrays.asList(
			new CaseStudy("SVM", "files/Cases/SodaVendingMachine/", "SVM", 12, 12, 12),
			new CaseStudy("eMail", "files/Cases/eMail/", "eM", 60, 24, 23),
			new CaseStudy("Elevator", "files/Cases/Elevator/", "El", 42, 42, 42),
			new CaseStudy("BankAccount", "files/Cases/BankAccountv2/", "BAv2", 504, 504, 498),
			new CaseStudy("StudentAttendance", "files/Cases/StudentAttendanceSystem/", "SAS", 2664, 2664, 2664),
			new CaseStudy("syngovia", "files/Cases/syngovia/", "Svia", 47970, 219, 47970),
			new CaseStudy("Tesla", "files/Cases/Tesla/", "Te", 428544, 428544, 426672),
			new CaseStudy("HockertyShirts", "files/Cases/HockertyShirts/", "HS", NOT_ENUMERABLE, NOT_ENUMERABLE,
					NOT_ENUMERABLE)));

	public static CaseStudy byName(String name) {
		for (CaseStudy study : ALL) {
			if (study.name.equalsIgnoreCase(name) || study.prefix.equalsIgnoreCase(name)) {
				return study;
			}
		}
		throw new IllegalArgumentException("Unknown product line " + name);
	}

	/** The studies named, or all eight when none is named. */
	public static List<CaseStudy> select(List<String> names) {
		if (names.isEmpty()) {
			return ALL;
		}
		List<CaseStudy> selected = new ArrayList<CaseStudy>();
		for (String name : names) {
			selected.add(byName(name));
		}
		return selected;
	}

	public boolean isEnumerable() {
		return products != NOT_ENUMERABLE;
	}

	public LoadedSplModel load() throws Exception {
		return SingleProductTestGenerationAPI.load(modelXml().getPath(), folder + prefix + "_ESGFx.mxe");
	}

	public File modelXml() {
		return new File(folder + "configs/model.xml");
	}

	public File dimacs() {
		return new File(folder + "configs/" + prefix + ".dimacs");
	}

	public File dimacsMapping() {
		return new File(folder + "configs/" + prefix + "_dimacsmapping.txt");
	}

	/** UniGen samples of Chapter 6 (DIMACS variable ids), or a file that does not exist. */
	public File samples() {
		return new File(folder + "configs/" + prefix + "_400.samples");
	}

	/** The configurations the Chapter 6 enumeration wrote, or a file that does not exist. */
	public File enumerationFile() {
		return new File(folder + prefix + "_ProductConfigurations.txt");
	}
}
