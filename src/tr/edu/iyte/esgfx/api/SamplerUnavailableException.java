package tr.edu.iyte.esgfx.api;

/** A sampler that relies on an external tool could not run it. Reported to the caller, not a fault. */
public class SamplerUnavailableException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SamplerUnavailableException(String message) {
		super(message);
	}
}
