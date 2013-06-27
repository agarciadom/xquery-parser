package es.uca.webservices.xquery.parser;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

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

import es.uca.webservices.xquery.parser.util.XQueryParsingException;
import es.uca.webservices.xquery.parser.util.XQueryValidatingParser;

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

	// Set of file names that should be skipped (for XQuery 1.1)
	private static final Set<String> SKIP_INPUTS = new HashSet<String>();

	@BeforeClass
	public static void loadCatalog() throws Exception {
		final DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		final Document doc = db.parse(new File("src/test/resources/xqts/XQTSCatalog.xml"));

		final NodeList expectedErrors = doc.getElementsByTagName("expected-error");
		for (int i = 0; i < expectedErrors.getLength(); ++i) {
			final Element e = (Element)expectedErrors.item(i);
			final Element tc = (Element)e.getParentNode();

			final String xqVersion = e.getAttribute("spec-version");
			final String scenario = tc.getAttribute("scenario");

			// Make sure we also reject it if it is in the parse error scenario...
			boolean bParseError = "parse-error".equals(scenario);
			// ... or expected parse error in XQuery 1.0 or all versions ...
			bParseError = bParseError || ("1.0".equals(xqVersion) || xqVersion == null) && "XPST0003".equals(e.getTextContent());
			// ... or invalid character reference ...
			bParseError = bParseError || "XQST0090".equals(e.getTextContent());
			if (bParseError) {
				addToInputFileSet(tc, BAD_INPUTS);
			}

			// Skip if not supported by XQuery 1.0
			final boolean bNotSupported = "1.0".equals(xqVersion) && "XQST0031".equals(e.getTextContent());
			if (bNotSupported) {
				addToInputFileSet(tc, SKIP_INPUTS);
			}
		}

		// bad test cases (see http://lists.w3.org/Archives/Public/public-qt-comments/2011Aug/0037.html)
		SKIP_INPUTS.add("XML10-4ed-Excluded-char-1.xq");
		SKIP_INPUTS.add("XML10-4ed-Excluded-char-2.xq");

		// UTF-16 (we only support UTF-8)
		SKIP_INPUTS.add("prolog-version-2.xq");
	}

	private static void addToInputFileSet(final Element tc,
			Set<String> badInputs) {
		badInputs.add(tc.getAttribute("name") + ".xq");

		// Some catalog entries have the .xq filename in the <query> child of the <test-case>
		final NodeList queries = tc.getElementsByTagName("query");
		if (queries.getLength() > 0) {
			for (int j = 0; j < queries.getLength(); j++) {
				final Element q = (Element)queries.item(j);
				badInputs.add(q.getAttribute("name") + ".xq");
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
		assumeFalse(SKIP_INPUTS.contains(xqFile.getName()));

		final ANTLRInputStream charStream = new ANTLRInputStream(
				new InputStreamReader(new FileInputStream(xqFile), "UTF-8"));

		final boolean isValid = !BAD_INPUTS.contains(xqFile.getName());
		try {
			new XQueryValidatingParser().parse(charStream);
			if (!isValid) {
				fail("The parser accepted the invalid XQuery module " + xqFile);
			}
		} catch (XQueryParsingException ex) {
			if (isValid) {
				fail("The parser rejected the valid XQuery module " + xqFile + ":\n" + ex.getErrors());
			}
		}
	}
}
