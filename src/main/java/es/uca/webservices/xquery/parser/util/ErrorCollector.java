package es.uca.webservices.xquery.parser.util;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
 * ANTLR4 error listener that collects all the non-report
 * error messages produced during lexing and/or parsing.
 *
 * @author Antonio García-Domínguez
 */
class ErrorCollector extends BaseErrorListener {
	private final List<String> errors;

	public ErrorCollector() {
		this(new ArrayList<String>());
	}

	public ErrorCollector(List<String> errors) {
		this.errors = errors;
	}

	public List<String> getErrors() {
		return errors;
	}

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer,
			Object offendingSymbol, int line, int charPositionInLine,
			String msg, RecognitionException e) {
		if (!msg.contains("report")) {
			errors.add("line "+ line + ":" + charPositionInLine + ": " + msg);
		}
	}
}