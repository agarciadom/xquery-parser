package es.uca.webservices.xquery.parser;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Tests that all the XQTS queries are recognized by the parser.
 *
 * @author Antonio García-Domínguez
 */
@RunWith(Parameterized.class)
public class ParserRecognitionTest {

	@Parameter
	public File xqFile;

	// Set of filenames that should *not* be accepted by the parser
	private static final Set<String> BAD_INPUTS = new HashSet<String>();

	@BeforeClass
	public static void loadCatalog() throws Exception {
		final DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		final Document doc = db.parse(new File("src/test/resources/xqts/XQTSCatalog.xml"));
		
		final NodeList expectedErrors = doc.getElementsByTagName("expected-error");
		for (int i = 0; i < expectedErrors.getLength(); ++i) {
			final Element e = (Element)expectedErrors.item(i);
			final Element tc = (Element)e.getParentNode();
			if ("parse-error".equals(tc.getAttribute("scenario"))) {
				BAD_INPUTS.add(tc.getAttribute("name") + ".xq");
			}
		}

		BAD_INPUTS.add("trivial-1.xq"); // XQueryX example
	}

	private static final class ErrorCollector extends BaseErrorListener {
		private final List<String> errors = new ArrayList<String>();

		public List<String> getErrors() {
			return errors;
		}

		@Override
		public void syntaxError(Recognizer<?, ?> recognizer,
				Object offendingSymbol, int line, int charPositionInLine,
				String msg, RecognitionException e) {
			if (!msg.contains("report")) {
				errors.add(msg);
			}
		}
	}

	private static final class XQueryFileFilter implements IOFileFilter {
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(".xq")
					&& !name.contains("eqname") // These tests require XQuery 3.0
					;
		}

		@Override
		public boolean accept(File file) {
			return accept(file.getParentFile(), file.getName());
		}
	}

	@Parameters
	public static Iterable<Object[]> testData() {
		final List<Object[]> data = new ArrayList<Object[]>();

		final Collection<File> xqFiles = FileUtils.listFiles(
				new File("src/test/resources/xqts"),
				new XQueryFileFilter(),
				TrueFileFilter.INSTANCE);
		for (File xqFile : xqFiles) {
			data.add(new Object[]{ xqFile });
		}

		return data;
	}

	@Test
	public void parsesCorrectly() throws Exception {
		final ANTLRInputStream charStream = new ANTLRInputStream(
				new InputStreamReader(new FileInputStream(xqFile), "UTF-8"));
		final XQueryLexer lexer = new XQueryLexer(charStream);
		final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
		final XQueryParser parser = new XQueryParser(tokenStream);

		final ErrorCollector errorCollector = new ErrorCollector();
		parser.addErrorListener(errorCollector);
		parser.module();

		final boolean hasErrors = !errorCollector.getErrors().isEmpty();
		final boolean shouldBeRejected = BAD_INPUTS.contains(xqFile.getName());
		/*
		if (shouldBeRejected && !hasErrors) {
			fail(String.format("The parser should not have accepted '%s'", xqFile));
		}
		*/
		if (!shouldBeRejected && hasErrors) {
			fail(String.format("The parser rejected '%s':\n%s",
				xqFile, errorCollector.getErrors()));
		}
	}
}
