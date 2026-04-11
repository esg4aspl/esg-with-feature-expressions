package tr.edu.iyte.esgfx.cases;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;
import org.sat4j.tools.ModelIterator;

import tr.edu.iyte.esg.model.ESG;


import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;
import tr.edu.iyte.esgfx.testgeneration.eventtriplecoverage.TransformedESGFxGenerator;

public class TransformedProductESGFxToDOTFileWriter extends CaseStudyUtilities {

	public void writeTransformedProductESGFxToFile() throws Exception {

		featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFile,
				ESGFxFile);

		List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);

		SATSolverGenerationFromFeatureModel satSolverGenerationFromFeatureModel = new SATSolverGenerationFromFeatureModel();
		ISolver solver = new ModelIterator(SolverFactory.newDefault());

		System.out.println("Transformed product ESG-Fx  " + SPLName + " STARTED");

		satSolverGenerationFromFeatureModel.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel,
				featureExpressionList);
		ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();
		TransformedESGFxGenerator transformedESGFxGenerator = new TransformedESGFxGenerator();

		String dotDirectoryPath = DOTFolder + "L2/";
		File dotDirectory = new File(dotDirectoryPath);

		if (!dotDirectory.exists() || !dotDirectory.isDirectory()) {
			throw new Exception("DOT directory does not exist: " + dotDirectoryPath);
		}

		File[] dotFiles = dotDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".dot"));
		if (dotFiles == null || dotFiles.length == 0) {
			System.out.println("No DOT files found in directory.");
			return;
		}

		Arrays.sort(dotFiles, (f1, f2) -> f1.getName().compareTo(f2.getName()));

		String ESGFxPerProductLog = caseStudyFolder  + "/" + SPLName + "_TransformedESG-FxPerProductLog"
				 + ".csv";
		
		File logFile = new File(ESGFxPerProductLog);
		boolean writeHeader = !logFile.exists() || logFile.length() == 0;

		if (logFile.getParentFile() != null) {
			logFile.getParentFile().mkdirs();
		}

		featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFile,
				ESGFxFile);

		try (PrintWriter ESGFxPerProductLogWriter = new PrintWriter(new FileWriter(logFile, true))) {

			if (writeHeader) {
				ESGFxPerProductLogWriter.println("ProductID;" + "L=3;V_3;E_3;L=4;V_4;E_4;");
			}

			int productID = 0;
			for (int i = 0; i < dotFiles.length; i++) {

				
				File dotFile = dotFiles[i];
				String productName = dotFile.getName().replaceAll("(?i)\\.dot", "");
				productID++;

				String configFilePath = productConfigurationFolder + productName + ".config";
				updateFeatureExpressionMapFromConfigFile(configFilePath);
				ESG productESGFx = productESGFxGenerator.generateProductESGFx(productID, productName, ESGFx);

				//System.out.println("Processing product: " + productName + " (Product ID: " + productID + ")" + productESGFx.getRealVertexList().size() + " vertices, " + productESGFx.getRealEdgeList().size() + " edges");
				ESG transformedProductESGFx_1 = transformedESGFxGenerator.generateTransformedESGFx(3,
						productESGFx);

						productESGFx = null; // Help GC by dereferencing the original product ESG-Fx if it's no longer needed

						productESGFx = productESGFxGenerator.generateProductESGFx(productID, productName, ESGFx);
				ESG transformedProductESGFx_2 = transformedESGFxGenerator.generateTransformedESGFx(4,
						productESGFx);

				ESGFxPerProductLogWriter.println(
					productID + ";L=3;" + transformedProductESGFx_1.getRealVertexList().size()+ ";" 
									+ transformedProductESGFx_1.getRealEdgeList().size() + ";L=4;" 
									+ transformedProductESGFx_2.getRealVertexList().size() + ";" 
									+ transformedProductESGFx_2.getRealEdgeList().size() + ";");
				productESGFx = null;


			}

		} catch (Exception e) {
			System.err.println("Error processing DOT files: " + e.getMessage());
			e.printStackTrace();
		}
		System.out.println("Transformed product ESG-Fx DOT FILE WRITER " + SPLName + " FINISHED");
	}	
}

		

