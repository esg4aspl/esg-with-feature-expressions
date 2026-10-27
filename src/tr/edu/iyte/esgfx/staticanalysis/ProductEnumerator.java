package tr.edu.iyte.esgfx.staticanalysis;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

/**
 * Enumerates the products of Phi_d, or of Phi_d together with further clauses:
 * solve, take the model, add a blocking clause made of the negated literals of
 * the concrete variables only, repeat. Each distinct assignment to the concrete
 * variables is visited once, with the full solver model it came with.
 *
 * <p>{@code ModelIterator} is not used: it blocks over all variables and counts
 * configurations, and it does not branch on a variable that occurs in no clause.
 * The models passed to the visitor assign every variable of the solver.
 */
public final class ProductEnumerator {

	/** Receives the full solver model of each product. */
	public interface Visitor {
		void visit(int[] model) throws Exception;
	}

	public static final long NO_LIMIT = Long.MAX_VALUE;

	private ProductEnumerator() {
	}

	/** A fresh solver holding the clauses of Phi_d and nothing else. */
	public static ISolver solverFor(PhiD phiD) {
		ISolver solver = SolverFactory.newDefault();
		solver.newVar(phiD.getVariableCount());
		try {
			for (int[] clause : phiD.getClauses()) {
				solver.addClause(new VecInt(clause));
			}
		} catch (ContradictionException ex) {
			return null;
		}
		return solver;
	}

	/** Distinct assignments of Phi_d to the concrete variables. */
	public static long products(PhiD phiD, Visitor visitor, long limit) throws Exception {
		ISolver solver = solverFor(phiD);
		return solver == null ? 0 : enumerate(solver, new int[0], phiD.getConcreteVariables(), visitor, limit);
	}

	/** Models of Phi_d over all its variables. */
	public static long configurations(PhiD phiD, Visitor visitor, long limit) throws Exception {
		ISolver solver = solverFor(phiD);
		int[] all = new int[phiD.getVariableCount()];
		for (int i = 0; i < all.length; i++) {
			all[i] = i + 1;
		}
		return solver == null ? 0 : enumerate(solver, new int[0], all, visitor, limit);
	}

	/**
	 * The blocking loop. The solver is consumed: the blocking clauses stay in it.
	 *
	 * @param assumptions        assumed in every call
	 * @param blockingVariables  the variables whose assignment makes a model distinct
	 * @param visitor            may be null
	 * @param limit              stop after this many models
	 * @return the number of distinct assignments to {@code blockingVariables}
	 */
	public static long enumerate(ISolver solver, int[] assumptions, int[] blockingVariables, Visitor visitor,
			long limit) throws Exception {
		VecInt assumed = new VecInt(assumptions);
		long count = 0;
		while (count < limit && isSatisfiable(solver, assumed)) {
			int[] model = complete(solver.model(), solver.nVars());
			count++;
			if (visitor != null) {
				visitor.visit(model);
			}
			if (blockingVariables.length == 0) {
				break;
			}
			VecInt blocking = new VecInt(blockingVariables.length);
			for (int variable : blockingVariables) {
				blocking.push(-model[variable - 1]);
			}
			try {
				solver.addClause(blocking);
			} catch (ContradictionException ex) {
				break;
			}
		}
		return count;
	}

	/**
	 * The solver model over variables 1..n, in order. Sat4j leaves out a variable
	 * that occurs in no clause; it is unconstrained, so it is completed with
	 * false. The blocking clause then mentions it, and later models assign it.
	 */
	private static int[] complete(int[] model, int variables) {
		int[] full = new int[variables];
		for (int variable = 1; variable <= variables; variable++) {
			full[variable - 1] = -variable;
		}
		for (int literal : model) {
			full[Math.abs(literal) - 1] = literal;
		}
		return full;
	}

	private static boolean isSatisfiable(ISolver solver, VecInt assumptions) {
		try {
			return solver.isSatisfiable(assumptions);
		} catch (TimeoutException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
