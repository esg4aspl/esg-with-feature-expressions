package tr.edu.iyte.esgfx.model.sequenceesgfx;

import java.util.Comparator;
import tr.edu.iyte.esg.model.ESG;
import tr.edu.iyte.esg.model.Edge;
import tr.edu.iyte.esg.model.EdgeSimple;
import tr.edu.iyte.esg.model.Event;
import tr.edu.iyte.esg.model.EventSimple;
import tr.edu.iyte.esg.model.Vertex;
import tr.edu.iyte.esg.model.sequenceesg.Sequence;
import tr.edu.iyte.esg.model.comparators.SequenceComparator;
import tr.edu.iyte.esg.model.comparators.VertexComparator;
import tr.edu.iyte.esgfx.model.ESGFx;
import tr.edu.iyte.esgfx.model.CompatibilityChecker;

public class SequenceESGFxTransformer {

	public SequenceESGFxTransformer() {

	}

	/**
	 * - ID and name for ESG -- ID of the (k+1)-ESG is obtained by incrementing the
	 * ID of the input k-ESG. -- Name of the (k+1)-ESG is obtained by appending "t"
	 * to the name of the input k-ESG. - Events in the event list -- Event list of
	 * the (k+1)-ESG contains the event instances in the event list of the input
	 * 1-ESG. - Vertices in sequences in sequence vertices -- (k+1)-ESG uses
	 * existing vertex instances from the input k-ESG and 1-ESG to construct new
	 * sequences. - Avoiding the creation of redundant sequence vertices -- A new
	 * sequence vertex instance is not created if there is a sequence vertex
	 * instance containing an equivalent sequence. -- Instances are looked-up from
	 * the sequence vertex list with respect to the sequences. This decreases the
	 * performance. - A sequence vertex, its event and its vertex sequence -- An
	 * event of a sequence vertex is not an actual event; therefore, it is not
	 * included in the event list. -- Name of a sequence vertex event is the string
	 * form of the sequence constructed using contexted string forms of the events
	 * in the vertices of the sequence.
	 */
	public ESG transform(ESG kESGFx, ESG oneESGFx) {
		ESG kp1ESGFx = new ESGFx(kESGFx.getID() + 1, kESGFx.getName() + "t"); // !!! esg id and name
		for (Event e : oneESGFx.getEventList()) {
			kp1ESGFx.addEvent(e);
		}
//		System.out.println("kp1ESGFx ");
		CompatibilityChecker compatibilityChecker = new CompatibilityChecker();
		Comparator<Vertex> vertexComparator = new VertexComparator();
		Comparator<Sequence<Vertex>> sequenceComparator = new SequenceComparator<Vertex>(vertexComparator);
		for (Edge qr : kESGFx.getEdgeList()) {
			VertexRefinedBySequence r = (VertexRefinedBySequence) qr.getTarget();
//			r.getSequence().forEach(e -> System.out.println("r " + e.getEvent().getName()));
			if (!r.isPseudoEndVertex()) {
				VertexRefinedBySequence q = (VertexRefinedBySequence) qr.getSource();
//				q.getSequence().forEach(e -> System.out.println("q " + e.getEvent().getName()));
				Vertex rLast = r.getSequence().getElement(r.getSequence().getSize() - 1);
//				System.out.println("rLast " + rLast.getEvent().getName());
				for (Edge ab : oneESGFx.getEdgeList()) {
					VertexRefinedBySequence a = (VertexRefinedBySequence) ab.getSource();
//					a.getSequence().forEach(e -> System.out.println("a " + e.getEvent().getName()));
					Vertex a1 = a.getSequence().getElement(0);
//					System.out.println("a1 " + a1.getEvent().getName());
					if (vertexComparator.compare(rLast, a1) == 0) {
						VertexRefinedBySequence b = (VertexRefinedBySequence) ab.getTarget();
//						b.getSequence().forEach(e -> System.out.println("b " + e.getEvent().getName()));
						Vertex b1 = b.getSequence().getElement(0);
//						System.out.println("b1 " + b1.getEvent().getName());
						// below part is simplified from the old transformation method.
						if (!(q.isPseudoStartVertex() && b1.isPseudoEndVertex())) {
							Sequence<Vertex> s = new Sequence<Vertex>();
							Sequence<Vertex> t = new Sequence<Vertex>();
							s.addElements(q.getSequence()); // !!! existing vertex instances
							if (!q.isPseudoStartVertex()) {
								s.addElement(rLast); // !!! existing vertex instances
							}
//							s.forEach(e -> System.out.println("s featureExpression " + e.getEvent().getName() + " "
//									+ ((VertexRefinedByFeatureExpression) e).getFeatureExpression().toString()));
							if (!b1.isPseudoEndVertex()) {
								t.addElements(r.getSequence()); // !!! existing vertex instances
							}
							t.addElement(b1); // !!! existing vertex instances
//							t.forEach(e -> System.out.println("t " + e.getEvent().getName()));
							VertexRefinedBySequence v = (VertexRefinedBySequence) SequenceESGUtilities
									.getVertexByVertexSequence(kp1ESGFx, s, sequenceComparator); // !!! look up to avoid
																									// using redundant
																									// instances
																									// (performance
																									// decrease)

							if (v == null) {
								Event e = new EventSimple(kp1ESGFx.getNextEventID(),
										VertexSequenceUtilities.getStringFormWithContextedEvents(s));
//								System.out.println("Event e " + e.getName());
								v = new VertexRefinedBySequence(kp1ESGFx.getNextVertexID(), e, s);
								v.setFeatureExpression();

								if (!v.isPseudoStartVertex() && !v.isPseudoEndVertex()) {
									compatibilityChecker.fillFeatureTruthValueMap(v);
									if (compatibilityChecker.isCompatible(v)) {
										kp1ESGFx.addVertex(v);
										compatibilityChecker.clearFeatureTruthValueMap();
									}

								} else {
									kp1ESGFx.addVertex(v);
									compatibilityChecker.clearFeatureTruthValueMap();
								}

							}
							VertexRefinedBySequence w = (VertexRefinedBySequence) SequenceESGUtilities
									.getVertexByVertexSequence(kp1ESGFx, t, sequenceComparator); // !!! look up to avoid
																									// using redundant
																									// instances
																									// (performance
																									// decrease)
							if (w == null) {
								Event e = new EventSimple(kp1ESGFx.getNextEventID(),
										VertexSequenceUtilities.getStringFormWithContextedEvents(t));
								w = new VertexRefinedBySequence(kp1ESGFx.getNextVertexID(), e, t);
								w.setFeatureExpression();

								if (!w.isPseudoStartVertex() && !w.isPseudoEndVertex()) {
									compatibilityChecker.fillFeatureTruthValueMap(w);
									if (compatibilityChecker.isCompatible(w)) {
										kp1ESGFx.addVertex(w);
										compatibilityChecker.clearFeatureTruthValueMap();
									}

								} else {
									kp1ESGFx.addVertex(w);
									compatibilityChecker.clearFeatureTruthValueMap();
								}

							}
//							System.out.println("v " + v.getEvent().getName());
//							System.out.println("w " + w.getEvent().getName());

							if (kp1ESGFx.getVertexList().contains(v) && kp1ESGFx.getVertexList().contains(w)) {
								if (!v.isPseudoStartVertex() && !w.isPseudoEndVertex()) {
									compatibilityChecker.fillFeatureTruthValueMap(v);
									compatibilityChecker.fillFeatureTruthValueMap(w);
									if (compatibilityChecker.isCompatible(w)) {
										Edge newEdge = new EdgeSimple(kp1ESGFx.getNextEdgeID(), v, w);
//										System.out.println(
//												"newEdge " + v.getEvent().getName() + " " + w.getEvent().getName());
										kp1ESGFx.addEdge(newEdge);
										compatibilityChecker.clearFeatureTruthValueMap();
									}
								} else {
									Edge newEdge = new EdgeSimple(kp1ESGFx.getNextEdgeID(), v, w);
//									System.out.println(
//											"newEdge " + v.getEvent().getName() + " " + w.getEvent().getName());
									kp1ESGFx.addEdge(newEdge);
									compatibilityChecker.clearFeatureTruthValueMap();
								}
							}

						} else {
							// q is start and b1 is end.
							// if the only following vertex of rLast is the end vertex in the 1-ESG,
							// then k-sequence r cannot be included in a longer sequence.
							// such sequences are discarded.
							// to include each of them, two edges must be added (start,r) and (r,end).
						}
					}
				}
			}
		}
		return kp1ESGFx;
	}

	public ESG transformIncludingShorterSequences(ESG kESGFx, ESG oneESGFx) {
		ESG kp1ESGFx = new ESGFx(kESGFx.getID() + 1, kESGFx.getName() + "t"); // !!! esg id and name
		CompatibilityChecker compatibilityChecker = new CompatibilityChecker();
		for (Event e : oneESGFx.getEventList()) {
			kp1ESGFx.addEvent(e);
		}
		Comparator<Vertex> vc = new VertexComparator();
		Comparator<Sequence<Vertex>> comp = new SequenceComparator<Vertex>(vc);
		for (Edge qr : kESGFx.getEdgeList()) {
			VertexRefinedBySequence r = (VertexRefinedBySequence) qr.getTarget();
			r.getSequence().forEach(e -> System.out.println("r " + e.getEvent().getName()));
			if (!r.isPseudoEndVertex()) {
				VertexRefinedBySequence q = (VertexRefinedBySequence) qr.getSource();
				q.getSequence().forEach(e -> System.out.println("q " + e.getEvent().getName()));
				Vertex rLast = r.getSequence().getElement(r.getSequence().getSize() - 1);
//				System.out.println("rLast " + rLast.getEvent().getName());
				for (Edge ab : oneESGFx.getEdgeList()) {
					VertexRefinedBySequence a = (VertexRefinedBySequence) ab.getSource();
					Vertex a1 = a.getSequence().getElement(0);
//					System.out.println("a1 " + a1.getEvent().getName());
					if (vc.compare(rLast, a1) == 0) {
						VertexRefinedBySequence b = (VertexRefinedBySequence) ab.getTarget();
						Vertex b1 = b.getSequence().getElement(0);
//						System.out.println("b1 " + b1.getEvent().getName());
//						below part is simplified from the old transformation method.
						if (!(q.isPseudoStartVertex() && b1.isPseudoEndVertex())) {
							Sequence<Vertex> s = new Sequence<Vertex>();
							Sequence<Vertex> t = new Sequence<Vertex>();
							s.addElements(q.getSequence()); // !!! existing vertex instances
							if (!q.isPseudoStartVertex()) {
								s.addElement(rLast); // !!! existing vertex instances
							}
//							s.forEach(e -> System.out.println("s " + e.getEvent().getName()));
							if (!b1.isPseudoEndVertex()) {
								t.addElements(r.getSequence()); // !!! existing vertex instances
							}
							t.addElement(b1); // !!! existing vertex instances
//							t.forEach(e -> System.out.println("t " + e.getEvent().getName()));
							VertexRefinedBySequence v = (VertexRefinedBySequence) SequenceESGUtilities
									.getVertexByVertexSequence(kp1ESGFx, s, comp); // !!! look up to avoid using
																					// redundant instances (performance
																					// decrease)
							if (v == null) {
								Event e = new EventSimple(kp1ESGFx.getNextEventID(),
										VertexSequenceUtilities.getStringFormWithContextedEvents(s));
								v = new VertexRefinedBySequence(kp1ESGFx.getNextVertexID(), e, s);

								if (!v.isPseudoStartVertex() && !v.isPseudoEndVertex()) {
									compatibilityChecker.fillFeatureTruthValueMap(v);
									if (compatibilityChecker.isCompatible(v)) {
										kp1ESGFx.addVertex(v);
										compatibilityChecker.clearFeatureTruthValueMap();
									}

								} else {
									kp1ESGFx.addVertex(v);
									compatibilityChecker.clearFeatureTruthValueMap();
								}
							}
							VertexRefinedBySequence w = (VertexRefinedBySequence) SequenceESGUtilities
									.getVertexByVertexSequence(kp1ESGFx, t, comp); // !!! look up to avoid using
																					// redundant instances (performance
																					// decrease)
							if (w == null) {
								Event e = new EventSimple(kp1ESGFx.getNextEventID(),
										VertexSequenceUtilities.getStringFormWithContextedEvents(t));
								w = new VertexRefinedBySequence(kp1ESGFx.getNextVertexID(), e, t);

								if (!w.isPseudoStartVertex() && !w.isPseudoEndVertex()) {
									compatibilityChecker.fillFeatureTruthValueMap(w);
									if (compatibilityChecker.isCompatible(w)) {
										kp1ESGFx.addVertex(w);
										compatibilityChecker.clearFeatureTruthValueMap();
									}

								} else {
									kp1ESGFx.addVertex(w);
									compatibilityChecker.clearFeatureTruthValueMap();
								}
							}
//							System.out.println("v " + v.getEvent().getName());
//							System.out.println("w " + w.getEvent().getName());
							if (kp1ESGFx.getVertexList().contains(v) && kp1ESGFx.getVertexList().contains(w)) {
								if (!v.isPseudoStartVertex() && !w.isPseudoEndVertex()) {
									compatibilityChecker.fillFeatureTruthValueMap(v);
									compatibilityChecker.fillFeatureTruthValueMap(w);
									if (compatibilityChecker.isCompatible(w)) {
										Edge newEdge = new EdgeSimple(kp1ESGFx.getNextEdgeID(), v, w);
//										System.out.println(
//												"newEdge " + v.getEvent().getName() + " " + w.getEvent().getName());
										kp1ESGFx.addEdge(newEdge);
										compatibilityChecker.clearFeatureTruthValueMap();
									}
								} else {
									Edge newEdge = new EdgeSimple(kp1ESGFx.getNextEdgeID(), v, w);
//									System.out.println(
//											"newEdge " + v.getEvent().getName() + " " + w.getEvent().getName());
									kp1ESGFx.addEdge(newEdge);
									compatibilityChecker.clearFeatureTruthValueMap();
								}
							}
						} else {
							// q is start and b1 is end.
							// if the only following vertex of rLast is the end vertex in the 1-ESG,
							// then k-sequence r cannot be included in a longer sequence.
							// such sequences are discarded.
							// to include each of them, two edges must be added (start,r) and (r,end).
							// if(a1.getOutDegree() == 1) {
							Sequence<Vertex> s = new Sequence<Vertex>();
							Sequence<Vertex> t = new Sequence<Vertex>();
							Sequence<Vertex> u = new Sequence<Vertex>();
							s.addElements(q.getSequence());
							t.addElements(r.getSequence());
							u.addElements(b.getSequence());
							VertexRefinedBySequence v = (VertexRefinedBySequence) SequenceESGUtilities
									.getVertexByVertexSequence(kp1ESGFx, s, comp); // !!! look up to avoid using
																					// redundant instances (performance
																					// decrease)
							if (v == null) {
								Event e = new EventSimple(kp1ESGFx.getNextEventID(),
										VertexSequenceUtilities.getStringFormWithContextedEvents(s));
								v = new VertexRefinedBySequence(kp1ESGFx.getNextVertexID(), e, s);

								if (!v.isPseudoStartVertex() && !v.isPseudoEndVertex()) {
									compatibilityChecker.fillFeatureTruthValueMap(v);
									if (compatibilityChecker.isCompatible(v)) {
										kp1ESGFx.addVertex(v);
										compatibilityChecker.clearFeatureTruthValueMap();
									}

								} else {
									kp1ESGFx.addVertex(v);
									compatibilityChecker.clearFeatureTruthValueMap();
								}
							}
							VertexRefinedBySequence w = (VertexRefinedBySequence) SequenceESGUtilities
									.getVertexByVertexSequence(kp1ESGFx, t, comp); // !!! look up to avoid using
																					// redundant instances (performance
																					// decrease)
							if (w == null) {
								Event e = new EventSimple(kp1ESGFx.getNextEventID(),
										VertexSequenceUtilities.getStringFormWithContextedEvents(t));
								w = new VertexRefinedBySequence(kp1ESGFx.getNextVertexID(), e, t);

								if (!w.isPseudoStartVertex() && !w.isPseudoEndVertex()) {
									compatibilityChecker.fillFeatureTruthValueMap(w);
									if (compatibilityChecker.isCompatible(w)) {
										kp1ESGFx.addVertex(w);
										compatibilityChecker.clearFeatureTruthValueMap();
									}

								} else {
									kp1ESGFx.addVertex(w);
									compatibilityChecker.clearFeatureTruthValueMap();
								}
							}
							VertexRefinedBySequence x = (VertexRefinedBySequence) SequenceESGUtilities
									.getVertexByVertexSequence(kp1ESGFx, u, comp); // !!! look up to avoid using
																					// redundant instances (performance
																					// decrease)
							if (x == null) {
								Event e = new EventSimple(kp1ESGFx.getNextEventID(),
										VertexSequenceUtilities.getStringFormWithContextedEvents(u));
								x = new VertexRefinedBySequence(kp1ESGFx.getNextVertexID(), e, u);

								if (!x.isPseudoStartVertex() && !x.isPseudoEndVertex()) {
									compatibilityChecker.fillFeatureTruthValueMap(x);
									if (compatibilityChecker.isCompatible(x)) {
										kp1ESGFx.addVertex(x);
										compatibilityChecker.clearFeatureTruthValueMap();
									}

								} else {
									kp1ESGFx.addVertex(x);
									compatibilityChecker.clearFeatureTruthValueMap();
								}
							}


							if (kp1ESGFx.getVertexList().contains(v) && kp1ESGFx.getVertexList().contains(w)) {
								if (!v.isPseudoStartVertex() && !w.isPseudoEndVertex()) {
									compatibilityChecker.fillFeatureTruthValueMap(v);
									compatibilityChecker.fillFeatureTruthValueMap(w);
									if (compatibilityChecker.isCompatible(w)) {
										Edge newEdge = new EdgeSimple(kp1ESGFx.getNextEdgeID(), v, w);
//										System.out.println(
//												"newEdge " + v.getEvent().getName() + " " + w.getEvent().getName());
										kp1ESGFx.addEdge(newEdge);
										compatibilityChecker.clearFeatureTruthValueMap();
									}
								} else {
									Edge newEdge = new EdgeSimple(kp1ESGFx.getNextEdgeID(), v, w);
//									System.out.println(
//											"newEdge " + v.getEvent().getName() + " " + w.getEvent().getName());
									kp1ESGFx.addEdge(newEdge);
									compatibilityChecker.clearFeatureTruthValueMap();
								}
							}

							if (kp1ESGFx.getVertexList().contains(w) && kp1ESGFx.getVertexList().contains(x)) {
								if (!w.isPseudoStartVertex() && !x.isPseudoEndVertex()) {
									compatibilityChecker.fillFeatureTruthValueMap(w);
									compatibilityChecker.fillFeatureTruthValueMap(x);
									if (compatibilityChecker.isCompatible(x)) {
										Edge newEdge = new EdgeSimple(kp1ESGFx.getNextEdgeID(), w, x);
//										System.out.println(
//												"newEdge " + v.getEvent().getName() + " " + w.getEvent().getName());
										kp1ESGFx.addEdge(newEdge);
										compatibilityChecker.clearFeatureTruthValueMap();
									}
								} else {
									Edge newEdge = new EdgeSimple(kp1ESGFx.getNextEdgeID(), w, x);
//									System.out.println(
//											"newEdge " + v.getEvent().getName() + " " + w.getEvent().getName());
									kp1ESGFx.addEdge(newEdge);
									compatibilityChecker.clearFeatureTruthValueMap();
								}
							}

							// }
						}
					}
				}
			}
		}
		return kp1ESGFx;
	}

}
