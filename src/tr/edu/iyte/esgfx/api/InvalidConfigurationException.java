package tr.edu.iyte.esgfx.api;

import java.util.List;

public class InvalidConfigurationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final List<String> errors;

	public InvalidConfigurationException(List<String> errors) {
		super(String.join("; ", errors));
		this.errors = errors;
	}

	public List<String> getErrors() {
		return errors;
	}
}
