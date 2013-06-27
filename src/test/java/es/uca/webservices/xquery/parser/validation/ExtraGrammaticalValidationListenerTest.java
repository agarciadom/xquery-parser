package es.uca.webservices.xquery.parser.validation;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Unit tests for the {@link ExtraGrammaticalValidationListener} class.
 *
 * @author Antonio García-Domínguez
 */
@RunWith(Parameterized.class)
public class ExtraGrammaticalValidationListenerTest {
	private final ExtraGrammaticalValidationListener listener
		= new ExtraGrammaticalValidationListener(null, new ArrayList<String>());

	@Parameter(0)
	public String charRef;
	
	@Parameter(1)
	public boolean shouldBeValid;
	
	@Parameters
	public static Iterable<Object[]> data() {
		return Arrays.asList(
			t("", false),       // Empty string is invalid
			t(null, false),     // Null is invalid
			t("&#32;", true),   // Space is valid
			t("&#x20;", true),
			t("&#9;", true),    // CR is valid
			t("&#x9;", true),
			t("&#13;", true),   // Line feed is valid
			t("&#xD;", true),
			t("&#xd;", true),
			t("&#10;", true),   // Horizontal tab is valid
			t("&#xA;", true),
			t("&#xa;", true),
			t("&#xa", false),   // Missing final ;
			t("x&#xa;", false), // Unwanted prefix
			t("y&#a;", false),
			t("&#xa;x", false), // Unwanted suffix
			t("&#xa;y", false),
			t("&#0;", false),   // Outside Char
			t("&#x00;", false),
			t("&#x0;", false),
			t("&#x8;", false),
			t("&#xC;", false),
			t("&#x16;", false),
			t("&#xd800;", false),
			t("&#xdfff;", false),
			t("&#xfffe;", false),
			t("&#xffff;", false),
			t("&#x110000;", false),
			t("&#x1100000;", false),  // Big numbers
			t("&#x110000000;", false),
			t("&#iampretty;", false), // Not numbers
			t("&#xohsopretty;", false),
			t("&#-34;", false), // Negative numbers
			t("&#x-A;", false),
			t("&#2A0;", false) // Decimal with letters
		);
	}

	@Test
	public void validate() {
		assertEquals(
			String.format("valid('%s')?", charRef),
			shouldBeValid,
			listener.charRefIsValid(charRef));
	}

	public static Object[] t(Object... l) {
		return l; 
	}
}
