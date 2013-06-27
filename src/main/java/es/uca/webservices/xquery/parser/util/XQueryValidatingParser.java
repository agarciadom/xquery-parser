package es.uca.webservices.xquery.parser.util;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import es.uca.webservices.xquery.parser.XQueryLexer;
import es.uca.webservices.xquery.parser.XQueryParser;
import es.uca.webservices.xquery.parser.XQueryParser.ModuleContext;
import es.uca.webservices.xquery.parser.validation.ExtraGrammaticalValidationListener;

/**
 * Utility class which wraps the ANTLR4 parsing process and
 * adds an additional validation step for the extra-grammatical
 * constraints.
 *
 * @author Antonio García-Domínguez
 */
public class XQueryValidatingParser {

	/**
	 * Parses and validates the XQuery module provided through
	 * <code>charStream</code>, collecting any additional results.
	 * 
	 * @throws XQueryParsingException
	 *             There were lexical, syntactical or extra-syntactical errors
	 *             in the XQuery module. These can be examined using
	 *             {@link XQueryParsingException#getErrors()}.
	 */
	public ModuleContext parse(final ANTLRInputStream charStream) throws XQueryParsingException {
		final XQueryLexer lexer = new XQueryLexer(charStream);
		final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
		final XQueryParser parser = new XQueryParser(tokenStream);

		final List<String> errors = new ArrayList<String>();
		final ErrorCollector errorCollector = new ErrorCollector(errors);
		lexer.addErrorListener(errorCollector);
		parser.addErrorListener(errorCollector);
		final ModuleContext tree = parser.module();

		if (tree != null && errors.isEmpty()) {
			final ExtraGrammaticalValidationListener extraValidator
				= new ExtraGrammaticalValidationListener(tokenStream, errors);
			final ParseTreeWalker walker = new ParseTreeWalker();
			walker.walk(extraValidator, tree);
		}

		if (!errorCollector.getErrors().isEmpty()) {
			throw new XQueryParsingException(errors);
		}
		return tree;
	}

}
