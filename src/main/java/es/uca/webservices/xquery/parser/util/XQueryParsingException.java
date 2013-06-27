package es.uca.webservices.xquery.parser.util;

import java.util.Arrays;
import java.util.List;

/**
 * Exception for a lexing or parsing error on an XQuery expression.
 *
 * @author Antonio García-Domínguez
 */
public class XQueryParsingException extends Exception {
	private static final long serialVersionUID = 1L;

	private final List<String> errors;

	public XQueryParsingException(List<String> errors) {
		super("Lexing and/or parsing failed with the following errors:\n" + errors);
		this.errors = errors;
	}

	public XQueryParsingException(String message, Throwable cause) {
		super(message, cause);
		this.errors = Arrays.asList(message);
	}

	public XQueryParsingException(String message) {
		super(message);
		this.errors = Arrays.asList(message);
	}

	public XQueryParsingException(Throwable cause) {
		super(cause);
		this.errors = Arrays.asList(cause.getMessage());
	}

	public List<String> getErrors() {
		return errors;
	}
}
