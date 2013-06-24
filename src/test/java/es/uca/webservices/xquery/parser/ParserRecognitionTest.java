package es.uca.webservices.xquery.parser;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests that all the XQTS queries are recognized by the parser.
 *
 * @author Antonio García-Domínguez
 */
@RunWith(Parameterized.class)
public class ParserRecognitionTest {

	@Parameter
	public File xqFile;

	private static final class XQueryFileFilter implements IOFileFilter {
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(".xq");
		}

		@Override
		public boolean accept(File file) {
			return file.getName().endsWith(".xq");
		}
	}

	@Parameters
	public static Iterable<Object[]> testData() {
		final List<Object[]> data = new ArrayList<Object[]>();

		final Collection<File> xqFiles = FileUtils.listFiles(
				new File("src/test/resources/xqts/Basics"),
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

		parser.addErrorListener(new BaseErrorListener() {
			@Override
			public void syntaxError(Recognizer<?, ?> recognizer,
					Object offendingSymbol, int line, int charPositionInLine,
					String msg, RecognitionException e) {
				if (!msg.contains("report")) {
					fail(String.format("Syntax error while parsing '%s': %s", xqFile, msg));
				}
			}
		});

		parser.module();
	}
}
