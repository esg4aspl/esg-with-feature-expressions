package tr.edu.iyte.esgfx.productmodelgeneration;

import java.util.Iterator;

import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.EdgeSimple;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esg.model.VertexSimple;
import tr.edu.iyte.esg.model.Event;
import tr.edu.iyte.esg.model.EventSimple;

import tr.edu.iyte.esgfx.model.VertexRefinedByFeatureExpression;
import tr.edu.iyte.esgfx.model.featureexpression.FeatureExpression;
import tr.edu.iyte.esgfx.model.ESGFx;

public class ProductESGFxGenerator {

	public ESG generateProductESGFx(int productID, String productName, ESG ESGFx) {

		ESG productESGFx = new ESGFx(productID, productName);

		generateVertexListOfProductESGFx(productESGFx, ESGFx);
		generateEdgeListOfProductESGFx(productESGFx, ESGFx);

		return productESGFx;
	}

	private void generateEdgeListOfProductESGFx(ESG productESGFx, ESG ESGFx) {
		Iterator<Edge> edgeListIterator = ESGFx.getEdgeList().iterator();

		while (edgeListIterator.hasNext()) {
			Edge edge = edgeListIterator.next();
			Vertex source = edge.getSource();
			Vertex target = edge.getTarget();

			if (source.isPseudoStartVertex()) {
				VertexRefinedByFeatureExpression targetVertexRefinedByFeatureExpression = (VertexRefinedByFeatureExpression) target;

				if (targetVertexRefinedByFeatureExpression.getFeatureExpression().evaluate()) {
					Vertex productESGFxSource = productESGFx.getVertexByEventName(source.toString());
					Vertex productESGFxTarget = productESGFx.getVertexByEventName(target.toString());
					Edge newEdge = new EdgeSimple(productESGFx.getNextEdgeID(), productESGFxSource, productESGFxTarget);

					productESGFx.addEdge(newEdge);
				}
			} else if (target.isPseudoEndVertex()) {
				VertexRefinedByFeatureExpression sourceVertexRefinedByFeatureExpression = (VertexRefinedByFeatureExpression) source;

				if (sourceVertexRefinedByFeatureExpression.getFeatureExpression().evaluate()) {
					Vertex productESGFxSource = productESGFx.getVertexByEventName(source.toString());
					Vertex productESGFxTarget = productESGFx.getVertexByEventName(target.toString());
					Edge newEdge = new EdgeSimple(productESGFx.getNextEdgeID(), productESGFxSource, productESGFxTarget);

					productESGFx.addEdge(newEdge);
				}
			} else {

				VertexRefinedByFeatureExpression sourceVertexRefinedByFeatureExpression = (VertexRefinedByFeatureExpression) source;
				VertexRefinedByFeatureExpression targetVertexRefinedByFeatureExpression = (VertexRefinedByFeatureExpression) target;

				if (sourceVertexRefinedByFeatureExpression.getFeatureExpression().evaluate()
						&& targetVertexRefinedByFeatureExpression.getFeatureExpression().evaluate()) {
					Vertex productESGFxSource = productESGFx.getVertexByEventName(source.toString());
					Vertex productESGFxTarget = productESGFx.getVertexByEventName(target.toString());
					Edge newEdge = new EdgeSimple(productESGFx.getNextEdgeID(), productESGFxSource, productESGFxTarget);

					productESGFx.addEdge(newEdge);
				}
			}
		}
	}

	private void generateVertexListOfProductESGFx(ESG productESGFx, ESG ESGFx) {
		Iterator<Vertex> vertexListIterator = ESGFx.getVertexList().iterator();

//		System.out.println("Vertex List Size: " + ESGFx.getVertexList().size());

		while (vertexListIterator.hasNext()) {
			Vertex vertex = vertexListIterator.next();
//			System.out.println(i + " Vertex: " + vertex.toString());
			if (!vertex.isPseudoStartVertex() && !vertex.isPseudoEndVertex()) {
				VertexRefinedByFeatureExpression vertexRefinedByFeatureExpression = (VertexRefinedByFeatureExpression) vertex;
				if (vertexRefinedByFeatureExpression.getFeatureExpression().evaluate()) {
					Event newEvent = new EventSimple(productESGFx.getNextEventID(), vertex.getEvent().getName());
					productESGFx.addEvent(newEvent);
					FeatureExpression newFeatureExpression = vertexRefinedByFeatureExpression.getFeatureExpression();
					Vertex newVertex = new VertexRefinedByFeatureExpression(productESGFx.getNextVertexID(), newEvent,
							newFeatureExpression);
					productESGFx.addVertex(newVertex);
				}
			} else {
				Event newEvent = new EventSimple(productESGFx.getNextEventID(), vertex.getEvent().getName());
				Vertex newVertex = new VertexSimple(productESGFx.getNextVertexID(), newEvent);
				productESGFx.addVertex(newVertex);
			}

		}
//		System.out.println("Product Vertex List Size: " + productESGFx.getVertexList().size());
//
//		for (Vertex vertex : productESGFx.getVertexList()) {
//			if(!vertex.isPseudoStartVertex() && !vertex.isPseudoEndVertex()) {
//				System.out.println("Product Vertex: " + ((VertexRefinedByFeatureExpression)vertex).toString2());
//			
//		}

	}

}
