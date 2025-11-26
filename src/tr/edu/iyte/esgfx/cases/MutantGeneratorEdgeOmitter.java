package tr.edu.iyte.esgfx.cases;

import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.LinkedHashSet;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;
import org.sat4j.tools.ModelIterator;

import tr.edu.iyte.esg.model.ESG;

import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;

import tr.edu.iyte.esgfx.mutationtesting.faultdetection.FaultDetector;
import tr.edu.iyte.esgfx.mutationtesting.mutationoperators.EdgeOmitter;
import tr.edu.iyte.esgfx.mutationtesting.mutationoperators.MutationOperator;
import tr.edu.iyte.esgfx.mutationtesting.resultutils.FaultDetectionResultRecorder;
import tr.edu.iyte.esgfx.productconfigurationgeneration.SATSolverGenerationFromFeatureModel;
import tr.edu.iyte.esgfx.productmodelgeneration.ProductESGFxGenerator;

public class MutantGeneratorEdgeOmitter extends MutantGenerator {

    public void generateMutants() throws Exception {

        featureExpressionMapFromFeatureModel = generateFeatureExpressionMapFromFeatureModel(featureModelFilePath,
                ESGFxFilePath);
        List<FeatureExpression> featureExpressionList = getFeatureExpressionList(featureExpressionMapFromFeatureModel);

        SATSolverGenerationFromFeatureModel sat = new SATSolverGenerationFromFeatureModel();
        ISolver solver = new ModelIterator(SolverFactory.newDefault());
        sat.addSATClauses(solver, featureModel, featureExpressionMapFromFeatureModel, featureExpressionList);

        int N_SHARDS = Integer.parseInt(System.getenv().getOrDefault("N_SHARDS", "1"));
        int CURRENT_SHARD = Integer.parseInt(System.getenv().getOrDefault("SHARD", "0"));

        int handledProducts = 0; 
        int productID = 0;

        int numberOfMutantsInSPL = 0;

        // Updated counters for L0-L4
        int numberOfDetectedMutantsInSPL_L0 = 0;
        int numberOfDetectedMutantsInSPL_L1 = 0;
        int numberOfDetectedMutantsInSPL_L2 = 0;
        int numberOfDetectedMutantsInSPL_L3 = 0;
        int numberOfDetectedMutantsInSPL_L4 = 0;

        while (solver.isSatisfiable()) {
            productID++;

            // Product name and configuration
            String productName = ProductIDUtil.format(productID);
            StringBuilder productConfiguration = new StringBuilder(productName + ": <");
            int numberOfFeatures = 0;

            int[] model = solver.model();
            for (int i = 0; i < model.length; i++) {
                FeatureExpression fe = featureExpressionList.get(i);
                String fname = fe.getFeature().getName();
                if (model[i] > 0) {
                    fe.setTruthValue(true);
                    productConfiguration.append(fname).append(", ");
                    numberOfFeatures++;
                } else {
                    fe.setTruthValue(false);
                }
            }
            if (numberOfFeatures > 0) {
                productConfiguration.setLength(productConfiguration.length() - 2);
            }
            productConfiguration.append(">:").append(numberOfFeatures).append(" features");

            // Block current model to get the next one
            VecInt blockingClause = new VecInt();
            for (int i = 0; i < model.length; i++)
                blockingClause.push(-model[i]);
            solver.addClause(blockingClause);

            boolean isProductConfigurationValid = isProductConfigurationValid(featureModel,
                    featureExpressionMapFromFeatureModel);

            if (!isProductConfigurationValid) {
                productID--;
                continue;
            }

            // ---SHARD GATE ---
            if (((productID - 1) % N_SHARDS) != CURRENT_SHARD) {
                continue;
            }
            
            handledProducts++; // Increment processed product count

            // Build product ESG-Fx once
            String ESGFxName = productName + productID;
            ProductESGFxGenerator productESGFxGenerator = new ProductESGFxGenerator();
            ESG productESGFx = productESGFxGenerator.generateProductESGFx(productID, ESGFxName, ESGFx);

            // Build test suites for L=0,1,2,3,4
            FaultDetector detectorL0 = generateFaultDetector(productESGFx, 0);
            FaultDetector detectorL1 = generateFaultDetector(productESGFx, 1);
            FaultDetector detectorL2 = generateFaultDetector(productESGFx, 2);
            FaultDetector detectorL3 = generateFaultDetector(productESGFx, 3);
            FaultDetector detectorL4 = generateFaultDetector(productESGFx, 4);

            // Generate mutants ON ORIGINAL product ESG-Fx
            MutationOperator mutationOperator = new EdgeOmitter();
            mutationOperator.generateMutantESGFxSets(productESGFx);

            Set<ESG> validMutants = mutationOperator.getValidMutantESGFxSet();
            Set<ESG> invalidMutants = mutationOperator.getInvalidMutantESGFxSet();

            Set<ESG> allMutants = new LinkedHashSet<>();
            allMutants.addAll(validMutants);
            allMutants.addAll(invalidMutants);

            int numberOfAllMutantsCurrentProduct = allMutants.size();
            numberOfMutantsInSPL += numberOfAllMutantsCurrentProduct;

            // Local counters for the current product
            int numberOfDetectedValidPerProductL0 = 0;
            int numberOfDetectedInValidPerProductL0 = 0;
            
            int numberOfDetectedValidPerProductL1 = 0;
            int numberOfDetectedInValidPerProductL1 = 0;

            int numberOfDetectedValidPerProductL2 = 0;
            int numberOfDetectedInValidPerProductL2 = 0;

            int numberOfDetectedValidPerProductL3 = 0;
            int numberOfDetectedInValidPerProductL3 = 0;

            int numberOfDetectedValidPerProductL4 = 0;
            int numberOfDetectedInValidPerProductL4 = 0;

            long execTimeCurrentProductL0 = 0;
            long execTimeCurrentProductL1 = 0;
            long execTimeCurrentProductL2 = 0;
            long execTimeCurrentProductL3 = 0;
            long execTimeCurrentProductL4 = 0;

            for (Entry<String, ESG> entry : ((EdgeOmitter) mutationOperator).getEdgeMutantMap().entrySet()) {
//                System.out.println(entry.getKey() + " is omitted");
            	ESG mutantESGFx = entry.getValue();
                boolean isMutantValid = validMutants.contains(mutantESGFx);

				for(int w=0; w<WARMUP_COUNT; w++) {
			        detectorL0.isFaultDetected(mutantESGFx);
			    }
				
				long sumNanosL0 = 0;
			    boolean d0 = false;
			    for (int i = 0; i < MEASURE_COUNT; i++) {
			        long start = System.nanoTime();
			        d0 = detectorL0.isFaultDetected(mutantESGFx);
			        long end = System.nanoTime();
			        sumNanosL0 += (end - start);
			    }
			    execTimeCurrentProductL0 += (sumNanosL0 / MEASURE_COUNT);
			    
			    for(int w=0; w<WARMUP_COUNT; w++) {
			    	detectorL1.isFaultDetected(mutantESGFx);
			    }
			    
				long sumNanosL1 = 0;
			    boolean d1 = false;
			    for (int i = 0; i < MEASURE_COUNT; i++) {
			        long start = System.nanoTime();
			        d1 = detectorL1.isFaultDetected(mutantESGFx);
			        long end = System.nanoTime();
			        sumNanosL1 += (end - start);
			    }
			    execTimeCurrentProductL1 += (sumNanosL1 / MEASURE_COUNT);
			    
			    for(int w=0; w<WARMUP_COUNT; w++) {
			    	detectorL2.isFaultDetected(mutantESGFx);
			    }
			    
				long sumNanosL2 = 0;
			    boolean d2 = false;
			    for (int i = 0; i < MEASURE_COUNT; i++) {
			        long start = System.nanoTime();
			        d2 = detectorL2.isFaultDetected(mutantESGFx);
			        long end = System.nanoTime();
			        sumNanosL2 += (end - start);
			    }
			    execTimeCurrentProductL2 += (sumNanosL2 / MEASURE_COUNT);
			    
			    for(int w=0; w<WARMUP_COUNT; w++) {
			    	detectorL3.isFaultDetected(mutantESGFx);
			    }
			    
				long sumNanosL3 = 0;
			    boolean d3 = false;
			    for (int i = 0; i < MEASURE_COUNT; i++) {
			        long start = System.nanoTime();
			        d3 = detectorL3.isFaultDetected(mutantESGFx);
			        long end = System.nanoTime();
			        sumNanosL3 += (end - start);
			    }
			    execTimeCurrentProductL3 += (sumNanosL3 / MEASURE_COUNT);
			    
			    for(int w=0; w<WARMUP_COUNT; w++) {
			    	detectorL4.isFaultDetected(mutantESGFx);
			    }
			    
				long sumNanosL4 = 0;
			    boolean d4 = false;
			    for (int i = 0; i < MEASURE_COUNT; i++) {
			        long start = System.nanoTime();
			        d4 = detectorL4.isFaultDetected(mutantESGFx);
			        long end = System.nanoTime();
			        sumNanosL4 += (end - start);
			    }
			    execTimeCurrentProductL4 += (sumNanosL4 / MEASURE_COUNT);

                if (d0) {
                    if (isMutantValid) numberOfDetectedValidPerProductL0++;
                    else numberOfDetectedInValidPerProductL0++;
                }
                if (d1) {
                    if (isMutantValid) numberOfDetectedValidPerProductL1++;
                    else numberOfDetectedInValidPerProductL1++;
                }
                if (d2) {
                    if (isMutantValid) numberOfDetectedValidPerProductL2++;
                    else numberOfDetectedInValidPerProductL2++;
                }
                if (d3) {
                    if (isMutantValid) numberOfDetectedValidPerProductL3++;
                    else numberOfDetectedInValidPerProductL3++;
                }
                if (d4) {
                    if (isMutantValid) numberOfDetectedValidPerProductL4++;
                    else numberOfDetectedInValidPerProductL4++;
                }
            } // endfor

            // Accumulate times
            totalExecTimeNanosL0 += execTimeCurrentProductL0;
            totalExecTimeNanosL1 += execTimeCurrentProductL1;
            totalExecTimeNanosL2 += execTimeCurrentProductL2;
            totalExecTimeNanosL3 += execTimeCurrentProductL3;
            totalExecTimeNanosL4 += execTimeCurrentProductL4;

            // Accumulate detected counts
            int numberOfDetectedPerProductL0 = numberOfDetectedValidPerProductL0 + numberOfDetectedInValidPerProductL0;
            int numberOfDetectedPerProductL1 = numberOfDetectedValidPerProductL1 + numberOfDetectedInValidPerProductL1;
            int numberOfDetectedPerProductL2 = numberOfDetectedValidPerProductL2 + numberOfDetectedInValidPerProductL2;
            int numberOfDetectedPerProductL3 = numberOfDetectedValidPerProductL3 + numberOfDetectedInValidPerProductL3;
            int numberOfDetectedPerProductL4 = numberOfDetectedValidPerProductL4 + numberOfDetectedInValidPerProductL4;

            numberOfDetectedMutantsInSPL_L0 += numberOfDetectedPerProductL0;
            numberOfDetectedMutantsInSPL_L1 += numberOfDetectedPerProductL1;
            numberOfDetectedMutantsInSPL_L2 += numberOfDetectedPerProductL2;
            numberOfDetectedMutantsInSPL_L3 += numberOfDetectedPerProductL3;
            numberOfDetectedMutantsInSPL_L4 += numberOfDetectedPerProductL4;

        } // endwhile

        // Calculate final stats
        double percentageInSPLL0 = percentageOfFaultDetection(numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L0);
        double percentageInSPLL1 = percentageOfFaultDetection(numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L1);
        double percentageInSPLL2 = percentageOfFaultDetection(numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L2);
        double percentageInSPLL3 = percentageOfFaultDetection(numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L3);
        double percentageInSPLL4 = percentageOfFaultDetection(numberOfMutantsInSPL, numberOfDetectedMutantsInSPL_L4);

        double totalSecondsL0 = totalExecTimeNanosL0 / 1_000_000_000.0;
        double totalSecondsL1 = totalExecTimeNanosL1 / 1_000_000_000.0;
        double totalSecondsL2 = totalExecTimeNanosL2 / 1_000_000_000.0;
        double totalSecondsL3 = totalExecTimeNanosL3 / 1_000_000_000.0;
        double totalSecondsL4 = totalExecTimeNanosL4 / 1_000_000_000.0;

        double killedPerSecondL0 = (totalSecondsL0 > 0) ? numberOfDetectedMutantsInSPL_L0 / totalSecondsL0 : 0;
        double killedPerSecondL1 = (totalSecondsL1 > 0) ? numberOfDetectedMutantsInSPL_L1 / totalSecondsL1 : 0;
        double killedPerSecondL2 = (totalSecondsL2 > 0) ? numberOfDetectedMutantsInSPL_L2 / totalSecondsL2 : 0;
        double killedPerSecondL3 = (totalSecondsL3 > 0) ? numberOfDetectedMutantsInSPL_L3 / totalSecondsL3 : 0;
        double killedPerSecondL4 = (totalSecondsL4 > 0) ? numberOfDetectedMutantsInSPL_L4 / totalSecondsL4 : 0;

        if (N_SHARDS > 1) {
//        	System.out.println("Shard " + CURRENT_SHARD + " Completed.");
//        	System.out.println("Total Products Processed by this Shard: " + handledProducts); // <--- Add this
//        	System.out.println("Total Mutants Generated: " + numberOfMutantsInSPL);
//        	
            String shardResultFilePath = shards_mutantgenerator_edgeomitter
                    + String.format("faultdetection.shard%02d.csv", CURRENT_SHARD);
            
            FaultDetectionResultRecorder.writeFaultDetectionResultsForSPL(shardResultFilePath, SPLName, "Edge Omitter",
                    numberOfMutantsInSPL, 
                    numberOfDetectedMutantsInSPL_L0, percentageInSPLL0, killedPerSecondL0,
                    numberOfDetectedMutantsInSPL_L1, percentageInSPLL1, killedPerSecondL1,
                    numberOfDetectedMutantsInSPL_L2, percentageInSPLL2, killedPerSecondL2,
                    numberOfDetectedMutantsInSPL_L3, percentageInSPLL3, killedPerSecondL3,
                    numberOfDetectedMutantsInSPL_L4, percentageInSPLL4, killedPerSecondL4);

        } else {
            FaultDetectionResultRecorder.writeFaultDetectionResultsForSPL(SPLSummary_FaultDetection, SPLName,
                    "Edge Omitter", numberOfMutantsInSPL, 
                    numberOfDetectedMutantsInSPL_L0, percentageInSPLL0, killedPerSecondL0,
                    numberOfDetectedMutantsInSPL_L1, percentageInSPLL1, killedPerSecondL1,
                    numberOfDetectedMutantsInSPL_L2, percentageInSPLL2, killedPerSecondL2,
                    numberOfDetectedMutantsInSPL_L3, percentageInSPLL3, killedPerSecondL3,
                    numberOfDetectedMutantsInSPL_L4, percentageInSPLL4, killedPerSecondL4);
        }

    }
}