package tr.edu.iyte.esgfx.productmodelgeneration;

import java.util.Iterator;
import java.util.HashMap; // New Import
import java.util.Map;     // New Import

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.EdgeSimple;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esg.model.Event;
import tr.edu.iyte.esg.model.EventSimple;

import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.ESGFx;

public class ProductESGFxGenerator {

	public ESG generateProductESGFx(int productID, String productName, ESG ESGFx) {

		ESG productESGFx = new ESGFx(productID, productName);

		// OPTIMIZATION: Create a temporary map to link Old Vertices to New Vertices.
		// This allows O(1) lookup during edge creation without String generation.
		Map<Vertex, Vertex> oldToNewVertexMap = new HashMap<>();

		generateVertexListOfProductESGFx(productESGFx, ESGFx, oldToNewVertexMap);
		generateEdgeListOfProductESGFx(productESGFx, ESGFx, oldToNewVertexMap);

		return productESGFx;
	}

	private void generateVertexListOfProductESGFx(ESG productESGFx, ESG ESGFx, Map<Vertex, Vertex> oldToNewVertexMap) {
		Iterator<Vertex> vertexListIterator = ESGFx.getVertexList().iterator();

//		System.out.println("Vertex List Size: " + ESGFx.getVertexList().size());

		while (vertexListIterator.hasNext()) {
			Vertex vertex = vertexListIterator.next();
//			System.out.println(i + " Vertex: " + vertex.toString());
//			if (!vertex.isPseudoStartVertex() && !vertex.isPseudoEndVertex()) {
			
			// Optimization: Check instance type safety
			if (vertex instanceof VertexRefinedByFeatureExpression) {
				VertexRefinedByFeatureExpression vertexRefinedByFeatureExpression = (VertexRefinedByFeatureExpression) vertex;
				
				if (vertexRefinedByFeatureExpression.getFeatureExpression().evaluate()) {
					Event newEvent = new EventSimple(productESGFx.getNextEventID(), vertex.getEvent().getName());
					productESGFx.addEvent(newEvent);
					FeatureExpression newFeatureExpression = vertexRefinedByFeatureExpression.getFeatureExpression();
					Vertex newVertex = new VertexRefinedByFeatureExpression(productESGFx.getNextVertexID(), newEvent,
							newFeatureExpression);
					
					productESGFx.addVertex(newVertex);
					
					// OPTIMIZATION: Cache the mapping (Old -> New)
					oldToNewVertexMap.put(vertex, newVertex);
				}
			}
//			} else {
//				Event newEvent = new EventSimple(productESGFx.getNextEventID(), vertex.getEvent().getName());
//				Vertex newVertex = new VertexSimple(productESGFx.getNextVertexID(), newEvent);
//				productESGFx.addVertex(newVertex);
//			}

		}
//		System.out.println("Product Vertex List Size: " + productESGFx.getVertexList().size());
//
//		for (Vertex vertex : productESGFx.getVertexList()) {
//			if(!vertex.isPseudoStartVertex() && !vertex.isPseudoEndVertex()) {
//				System.out.println("Product Vertex: " + ((VertexRefinedByFeatureExpression)vertex).toString2());
//			
//		}
	}

	private void generateEdgeListOfProductESGFx(ESG productESGFx, ESG ESGFx, Map<Vertex, Vertex> oldToNewVertexMap) {
		Iterator<Edge> edgeListIterator = ESGFx.getEdgeList().iterator();

		while (edgeListIterator.hasNext()) {
			Edge edge = edgeListIterator.next();
			Vertex source = edge.getSource();
			Vertex target = edge.getTarget();
			
			// OPTIMIZATION: Direct O(1) Lookup using the Map
			// Instead of recreating strings and searching, we just get the reference.
			Vertex productESGFxSource = oldToNewVertexMap.get(source);
			Vertex productESGFxTarget = oldToNewVertexMap.get(target);

			// LOGIC SIMPLIFICATION:
			// If both Source and Target exist in the map, it means they were both VALID 
			// (evaluate() == true) in the vertex generation step.
			// So we don't need to check evaluate() again here.
			
			if (productESGFxSource != null && productESGFxTarget != null) {
				Edge newEdge = new EdgeSimple(productESGFx.getNextEdgeID(), productESGFxSource, productESGFxTarget);
				productESGFx.addEdge(newEdge);
			}
			
			// Original complex logic replaced by Map check above. 
			// The map check inherently handles PseudoStart, PseudoEnd, and Feature checks 
			// because only valid vertices were added to the map in the previous method.
			
			/* Original Logic for reference (preserved as requested logic was redundant with map approach):
			if (source.isPseudoStartVertex()) {
				// logic...
			} else if (target.isPseudoEndVertex()) {
				// logic...
			} else {
				// logic...
			}
			*/
		}
	}
}