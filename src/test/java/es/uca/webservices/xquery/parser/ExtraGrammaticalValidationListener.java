package es.uca.webservices.xquery.parser;

import java.util.List;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import es.uca.webservices.xquery.parser.XQueryParser.AndContext;
import es.uca.webservices.xquery.parser.XQueryParser.CastContext;
import es.uca.webservices.xquery.parser.XQueryParser.CastableContext;
import es.uca.webservices.xquery.parser.XQueryParser.CommonContentContext;
import es.uca.webservices.xquery.parser.XQueryParser.ComparisonContext;
import es.uca.webservices.xquery.parser.XQueryParser.DirAttributeListContext;
import es.uca.webservices.xquery.parser.XQueryParser.DirElemConstructorOpenCloseContext;
import es.uca.webservices.xquery.parser.XQueryParser.DirElemConstructorSingleTagContext;
import es.uca.webservices.xquery.parser.XQueryParser.InstanceOfContext;
import es.uca.webservices.xquery.parser.XQueryParser.IntersectContext;
import es.uca.webservices.xquery.parser.XQueryParser.MultContext;
import es.uca.webservices.xquery.parser.XQueryParser.OrContext;
import es.uca.webservices.xquery.parser.XQueryParser.QNameContext;
import es.uca.webservices.xquery.parser.XQueryParser.RootedPathContext;
import es.uca.webservices.xquery.parser.XQueryParser.StringLiteralContext;
import es.uca.webservices.xquery.parser.XQueryParser.TreatContext;
import es.uca.webservices.xquery.parser.XQueryParser.UnionContext;

public class ExtraGrammaticalValidationListener extends XQueryParserBaseListener
{
	private static final String DEC_CHARREF_PREFIX = "&#";
	private static final String HEX_CHARREF_PREFIX = "&#x";

	private final CommonTokenStream tokenStream;

	private boolean xgcLeadingSlashOK = true;
	private boolean wsExplicitOK = true;
	private boolean balancedTags = true;
	private boolean noAdjacentNonDelimiting = true;
	private boolean validCharReferences = true;

	ExtraGrammaticalValidationListener(CommonTokenStream tokenStream) {
		this.tokenStream = tokenStream;
	}

	public boolean isValid() {
		return xgcLeadingSlashOK && wsExplicitOK && balancedTags && noAdjacentNonDelimiting && validCharReferences;
	}

	@Override
	public void enterAnd(AndContext ctx) {
		checkNoAdjacentNonDelimiting(ctx.op);
	}

	@Override
	public void enterCast(CastContext ctx) {
		checkNoAdjacentNonDelimiting(ctx.op);
	}

	@Override
	public void enterCastable(CastableContext ctx) {
		checkNoAdjacentNonDelimiting(ctx.op);
	}

	@Override
	public void enterCommonContent(CommonContentContext ctx) {
		if (validCharReferences && ctx.CharRef() != null) {
			validCharReferences = charRefIsValid(ctx.CharRef().getText());
		}
	}

	@Override
	public void enterComparison(ComparisonContext ctx) {
		checkNoAdjacentNonDelimiting(getNextVisibleToken(ctx.l.stop.getTokenIndex()));
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
	public void enterInstanceOf(InstanceOfContext ctx) {
		checkNoAdjacentNonDelimiting(ctx.op);
	}

	@Override
	public void enterIntersect(IntersectContext ctx) {
		checkNoAdjacentNonDelimiting(ctx.op);
	}

	@Override
	public void enterMult(MultContext ctx) {
		checkNoAdjacentNonDelimiting(ctx.op);
	}

	@Override
	public void enterOr(OrContext ctx) {
		checkNoAdjacentNonDelimiting(ctx.op);
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

	@Override
	public void enterStringLiteral(StringLiteralContext ctx) {
		if (validCharReferences && ctx.CharRef() != null) {
			for (TerminalNode c : ctx.CharRef()) {
				if (!charRefIsValid(c.getText())) {
					validCharReferences = false;
					break;
				}
			}
		}
	}

	@Override
	public void enterTreat(TreatContext ctx) {
		checkNoAdjacentNonDelimiting(ctx.op);
	}

	@Override
	public void enterUnion(UnionContext ctx) {
		checkNoAdjacentNonDelimiting(ctx.op);
	}

	boolean charRefIsValid(String text) {
		if (text == null || !text.endsWith(";")) {
			return false;
		}
	
		try {
			int code = 0;
			if (text.startsWith(HEX_CHARREF_PREFIX)) {
				final String sHex = text.substring(HEX_CHARREF_PREFIX.length(), text.length() - 1);
				code = Integer.valueOf(sHex, 16);
			}
			else if (text.startsWith(DEC_CHARREF_PREFIX)) {
				final String sDec = text.substring(DEC_CHARREF_PREFIX.length(), text.length() - 1);
				code = Integer.valueOf(sDec);
			}

			return code == 0x9 || code == 0xa || code == 0xd
					|| (code >= 0x20 && code <= 0xd7ff)
					|| (code >= 0xe000 && code <= 0xfffd)
					|| (code >= 0x10000 && code <= 0x10ffff);
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	/**
	 * Checks that if the token <code>t</code> is a non-delimiting token, it is
	 * not directly preceded by another non-delimiting token.
	 * 
	 * @see #isNonDelimiting(Token)
	 */
	private void checkNoAdjacentNonDelimiting(final Token t) {
		if (noAdjacentNonDelimiting) {
			final int tIdx = t.getTokenIndex();
			if (tIdx > 0 && isNonDelimiting(t)) {
				final Token prev = tokenStream.get(tIdx - 1);
				if (isNonDelimiting(prev)) {
					// XQ 1.0 A.2.2: non-delimiting terminals should have whitespace or comments between them
					noAdjacentNonDelimiting = false;
				}
			}
		}
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

	private boolean isNonDelimiting(Token t) {
		final int tType = t.getType();

		if (tType >= XQueryLexer.KW_ANCESTOR && tType <= XQueryLexer.KW_XQUERY) {
			// Keywords
			return true;
		}
		switch (tType) {
		case XQueryLexer.IntegerLiteral:
		case XQueryLexer.DecimalLiteral:
		case XQueryLexer.DoubleLiteral:
		case XQueryLexer.NCName:
		case XQueryLexer.FullQName:
			return true;
		}

		// anything else is delimiting
		return false;
	}

	/**
	 * Returns <code>true</code> if <code>left</code> comes right before
	 * <code>right</code>, with no hidden tokens between them.
	 */
	private boolean justBefore(Token left, Token right) {
		return left.getTokenIndex() + 1 == right.getTokenIndex();
	}
}