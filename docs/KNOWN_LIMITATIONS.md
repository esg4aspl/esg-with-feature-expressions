# Known limitations

## Non-deterministic test-sequence partitioning at the balancing stage

For a given (product configuration, coverage length) pair, repeated
generation in the same JVM may produce **equivalent but not byte-identical**
test suites:

- The reported coverage percentage is the same.
- The total number of test sequences is the same.
- The total number of events across all sequences is the same.
- The exact partition of events into sequences can differ, and the order
  in which sequences are returned can differ.

The source is the strongly-connected balancing stage used inside the
engine. Balancing reduces to a Kuhn-Munkres minimum-weight bipartite
matching over `org.jgrapht.graph.DefaultWeightedEdge` instances, which
do not override `hashCode`/`equals`. When several optimal matchings
exist (ties on weight), the matching algorithm breaks ties in an order
that depends on `System.identityHashCode` of those edge instances. That
hash is stable per object but unpredictable across objects, so its
distribution drifts as more objects are allocated within a JVM.

Consequences:

- The same input can yield different (but equally valid) test suites on
  repeated calls within the same JVM.
- Two separate JVM invocations starting from the same state at the same
  call index produce the same output.
- This is a property of the underlying engine, not of the
  `tr.edu.iyte.esgfx.api` layer. The API runs the same pipeline as the
  RQ1 case classes and inherits the same behaviour.

Verification that depends on a recorded ground truth should compare
coverage percentage, sequence count, and total event count rather than
demanding byte-identical sequences.

## LoadedSplModel is not thread-safe on its own

The model's `featureExpressionMap` is mutated each time a product is
generated: feature truth values are written into the same expression
objects that the SPL ESG-Fx references. The API serialises access by
synchronising on the model instance, so a single `LoadedSplModel` can
safely be shared between callers, but concurrent `generate` or
`validate` calls against the same model run one at a time.

## EventSequence equality is name-based

`tr.edu.iyte.esg.eventsequence.EventSequence` defines `equals` and
`hashCode` over its string form (the concatenated event names of its
vertices). A `Set<EventSequence>` may therefore merge two sequences
that share the same event-name sequence even when they traverse
different vertex instances. This does not occur for the three bundled
SPLs, but is a possible edge case for user-supplied models with
duplicate event names.
