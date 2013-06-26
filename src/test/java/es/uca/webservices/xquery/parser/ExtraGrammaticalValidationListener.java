package es.uca.webservices.xquery.parser;

import java.util.List;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import es.uca.webservices.xquery.parser.XQueryParser.DirAttributeListContext;
import es.uca.webservices.xquery.parser.XQueryParser.DirElemConstructorOpenCloseContext;
import es.uca.webservices.xquery.parser.XQueryParser.DirElemConstructorSingleTagContext;
import es.uca.webservices.xquery.parser.XQueryParser.QNameContext;
import es.uca.webservices.xquery.parser.XQueryParser.RootedPathContext;

public class ExtraGrammaticalValidationListener extends XQueryParserBaseListener
{
	private final CommonTokenStream tokenStream;
	private boolean xgcLeadingSlashOK = true;
	private boolean wsExplicitOK = true;
	private boolean balancedTags = true;

	ExtraGrammaticalValidationListener(CommonTokenStream tokenStream) {
		this.tokenStream = tokenStream;
	}

	public boolean isValid() {
		return xgcLeadingSlashOK && wsExplicitOK && balancedTags;
	}

	@Override
	public void enterDirElemConstructorOpenClose(DirElemConstructorOpenCloseContext ctx) {
		if (wsExplicitOK) {
			if (!justBefore(ctx.start, ctx.openName.start)
					|| !justBefore(ctx.startClose, ctx.slashClose)
					|| !justBefore(ctx.slashClose, ctx.closeName.start)) {
				// There should be no whitespace between the < and the qName of the opening tag,
				// or the < and / of the closing tag, or the / and name of the closing tag
				wsExplicitOK = false;
			}

			// Direct element constructors cannot have XQuery comments in the tags
			checkNoXQComments(ctx.openName.start.getTokenIndex(), ctx.endOpen.getTokenIndex());
			checkNoXQComments(ctx.startClose.getTokenIndex(), ctx.stop.getTokenIndex());
		}

		balancedTags = balancedTags && ctx.openName.getText().equals(ctx.closeName.getText());
	}

	@Override
	public void enterDirElemConstructorSingleTag(DirElemConstructorSingleTagContext ctx) {
		if (wsExplicitOK) {
			if (!justBefore(ctx.start, ctx.openName.start) || !justBefore(ctx.slashClose, ctx.stop)) {
				// There should be no whitespace between the < and the qName of the tag,
				// or the final / and the >
				wsExplicitOK = false;
			}
			checkNoXQComments(ctx.start.getTokenIndex(), ctx.stop.getTokenIndex());
		}
	}

	@Override
	public void enterDirAttributeList(DirAttributeListContext ctx) {
		for (QNameContext nameCtx : ctx.qName()) {
			final int startIdx = nameCtx.start.getTokenIndex();
			final Token prevToken = tokenStream.getTokens().get(startIdx - 1);
			if (prevToken.getType() != XQueryLexer.WS) {
				// There must be space before the name of an attribute
				wsExplicitOK = false;
			}
		}
	}

	@Override
	public void enterRootedPath(RootedPathContext ctx) {
		if (xgcLeadingSlashOK && ctx.relativePathExpr() == null) {
			// xgc:leading-lone-slash
			final Token nextToken = getNextVisibleToken(ctx.start.getTokenIndex());
			if (nextToken != null) {
				switch (nextToken.getType()) {
				// These tokens cannot be part of a RelativeLocationExpr and
				// can appear after a lone leading '/'
				case XQueryLexer.EQUAL:
				case XQueryLexer.NOT_EQUAL:
				case XQueryLexer.LPAREN:
				case XQueryLexer.RPAREN:
				case XQueryLexer.LBRACKET:
				case XQueryLexer.RBRACKET:
				case XQueryLexer.LBRACE:
				case XQueryLexer.RBRACE:
				case XQueryLexer.PLUS:
				case XQueryLexer.MINUS:
				case XQueryLexer.COMMA:
				case XQueryLexer.COLON:
				case XQueryLexer.COLON_EQ:
				case XQueryLexer.SEMICOLON:
				case XQueryLexer.VBAR:
				case XQueryLexer.RANGLE:
				case XQueryLexer.QUESTION:
					// OK, do nothing
					break;
				default:
					xgcLeadingSlashOK = false;
					break;
				}
			}
		}
	}

	/**
	 * Returns the next token that is not on the {@link XQueryLexer#HIDDEN} channel.
	 */
	private Token getNextVisibleToken(final int start) {
		Token nextToken = null;
		final List<Token> tokens = tokenStream.getTokens();
		for (int i = start + 1; nextToken == null && i < tokens.size(); ++i) {
			nextToken = tokens.get(i);
			if (nextToken.getChannel() != XQueryLexer.DEFAULT_TOKEN_CHANNEL) {
				nextToken = null;
			}
		}
		return nextToken;
	}

	/**
	 * Enforces the part of ws:explicit that disallows having XQuery comments in
	 * some places.
	 */
	private void checkNoXQComments(final int start, final int end) {
		for (int i = start; wsExplicitOK && i <= end; i++) {
			final Token t = tokenStream.getTokens().get(i);
			if (t.getType() == XQueryLexer.XQComment) {
				wsExplicitOK = false;
			}
		}
	}

	/**
	 * Returns <code>true</code> if <code>left</code> comes right before
	 * <code>right</code>, with no hidden tokens between them.
	 */
	private boolean justBefore(Token left, Token right) {
		return left.getTokenIndex() + 1 == right.getTokenIndex();
	}
}