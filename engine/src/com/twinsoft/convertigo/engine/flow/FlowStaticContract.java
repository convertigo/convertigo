/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.engine.flow;

import java.util.LinkedHashSet;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.ArrayLiteral;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.KeywordLiteral;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NumberLiteral;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.StringLiteral;
import org.mozilla.javascript.ast.TemplateCharacters;
import org.mozilla.javascript.ast.TemplateLiteral;
import org.mozilla.javascript.ast.UnaryExpression;
import org.mozilla.javascript.ast.VariableDeclaration;

/**
 * Reads the literal header {@code const _flow = {...}} of a source with the Rhino parser, which never evaluates it.
 * The header is certain only when it is the first statement, holds literal values only, and no other identifier of the
 * source names {@code _flow}; mentions in comments and strings are not references.
 */
public final class FlowStaticContract {
	private static final int MAX_SOURCE = 2 * 1024 * 1024;
	private static final int MAX_DEPTH = 256;
	private static final java.util.Set<String> WARNED = new LinkedHashSet<>();

	private FlowStaticContract() { }

	/** Whether the word _flow appears anywhere, comments and strings included: an uncertain header is then shown closed. */
	public static boolean declaresFlow(String source) {
		if (source == null) return false;
		for (int i = source.indexOf("_flow"); i >= 0; i = source.indexOf("_flow", i + 5)) {
			if ((i == 0 || !identifierPart(source.charAt(i - 1))) && (i + 5 >= source.length() || !identifierPart(source.charAt(i + 5)))) return true;
		}
		return false;
	}

	private static boolean identifierPart(char ch) { return Character.isLetterOrDigit(ch) || ch == '_' || ch == '$'; }

	/**
	 * What a view may conclude from a source: its certain header, else null; and whether the source may declare one.
	 * A source Rhino reads with no identifier named _flow declares none, whatever its comments and strings say.
	 */
	public record Header(JSONObject contract, boolean declaresFlow) { }

	/** The certain header as JSON, else null. */
	public static JSONObject read(String source) {
		return inspect(source).contract();
	}

	public static Header inspect(String source) {
		if (source == null || !declaresFlow(source)) return new Header(null, false);
		try {
			if (source.length() > MAX_SOURCE) return unreadable(source);
			var root = parse(maskKeywordCalls(source));
			if (root == null) return unreadable(source);
			var references = new int[1];
			root.visit(node -> {
				if (node instanceof Name name && "_flow".equals(name.getIdentifier())) references[0]++;
				return true;
			});
			// The runtime reads its header line by line and knows no block comment: a declaration line it would read keeps the view closed.
			if (references[0] == 0) return RUNTIME_HEADER_LINE.matcher(source).find() ? unreadable(source) : new Header(null, false);
			if (references[0] != 1 || !root.hasChildren()) return unreadable(source);
			if (!(root.getFirstChild() instanceof VariableDeclaration declaration) || declaration.getType() != Token.CONST
					|| declaration.getVariables().size() != 1) return unreadable(source);
			var initializer = declaration.getVariables().get(0);
			if (!(initializer.getTarget() instanceof Name target) || !"_flow".equals(target.getIdentifier())
					|| !(initializer.getInitializer() instanceof ObjectLiteral literal)) return unreadable(source);
			// The masks only rename keyword calls outside strings and comments: the values are read from the author's text.
			int start = literal.getAbsolutePosition(), end = start + literal.getLength();
			if (end > source.length()) return unreadable(source);
			var header = parse("(" + source.substring(start, end) + ")");
			if (header == null || !(header.getFirstChild() instanceof org.mozilla.javascript.ast.ExpressionStatement statement)
					|| !(statement.getExpression() instanceof org.mozilla.javascript.ast.ParenthesizedExpression parenthesized)
					|| !(parenthesized.getExpression() instanceof ObjectLiteral object)) return unreadable(source);
			return new Header((JSONObject) value(object, 0), true);
		} catch (RuntimeException | StackOverflowError failure) {
			return unreadable(source);
		}
	}

	private static AstRoot parse(String text) {
		var environment = new CompilerEnvirons();
		environment.setLanguageVersion(Context.VERSION_ES6);
		environment.setRecoverFromErrors(false);
		environment.setRecordingComments(false);
		environment.setStrictMode(false);
		try {
			return new Parser(environment).parse(text, "flow-header", 1);
		} catch (org.mozilla.javascript.EvaluatorException invalid) {
			return null;
		}
	}

	/**
	 * Renames, without moving any character, the keyword calls of the former source dialect ({@code if({}, try({},
	 * while({, default(}) outside strings and comments, so that Rhino reads the rest. A wrong guess only makes the
	 * parse fail or misread a statement; it never adds or hides an identifier named _flow.
	 */
	private static final java.util.regex.Pattern RUNTIME_HEADER_LINE = java.util.regex.Pattern.compile("(?m)^[ \\t]*(?:const|let|var)[ \\t]+_flow[ \\t]*=");
	private static final java.util.regex.Pattern IMPORT_LINE = java.util.regex.Pattern.compile(
			"(?m)^[ \\t]*import[ \\t]+[^;\\r\\n'\"`]*?[ \\t]from[ \\t]*(['\"])[^'\"\\r\\n]*\\1[ \\t]*;?[ \\t]*$");

	static String maskKeywordCalls(String source) {
		var out = new StringBuilder(source);
		// An import line is blanked, as Rhino has no modules; a line holding anything else, or naming _flow, is kept.
		var imports = IMPORT_LINE.matcher(source);
		while (imports.find()) {
			if (declaresFlow(imports.group())) continue;
			for (int i = imports.start(); i < imports.end(); i++) out.setCharAt(i, ' ');
		}
		int length = source.length();
		for (int i = 0; i < length;) {
			char ch = source.charAt(i);
			if (ch == '/' && i + 1 < length && source.charAt(i + 1) == '/') {
				while (i < length && source.charAt(i) != '\n' && source.charAt(i) != '\r') i++;
			} else if (ch == '/' && i + 1 < length && source.charAt(i + 1) == '*') {
				int close = source.indexOf("*/", i + 2);
				i = close < 0 ? length : close + 2;
			} else if (ch == '\'' || ch == '"' || ch == '`') {
				i++;
				while (i < length && source.charAt(i) != ch) i += source.charAt(i) == '\\' ? 2 : 1;
				i++;
			} else if (Character.isJavaIdentifierStart(ch)) {
				int start = i;
				while (i < length && Character.isJavaIdentifierPart(source.charAt(i))) i++;
				var word = source.substring(start, i);
				if ((start == 0 || source.charAt(start - 1) != '.') && List.of("if", "try", "while", "default").contains(word)) {
					int next = i;
					while (next < length && Character.isWhitespace(source.charAt(next))) next++;
					boolean call = next < length && source.charAt(next) == '(';
					if (call && !"default".equals(word)) {
						int open = next + 1;
						while (open < length && Character.isWhitespace(source.charAt(open))) open++;
						call = open < length && source.charAt(open) == '{';
					}
					if (call) out.setCharAt(start + 1, '_');
				}
			} else i++;
		}
		return out.toString();
	}

	private static Object value(AstNode node, int depth) {
		if (depth > MAX_DEPTH) throw new IllegalArgumentException("Header is too deeply nested");
		if (node instanceof StringLiteral text) return text.getValue();
		if (node instanceof NumberLiteral number) return number(number.getNumber());
		if (node instanceof UnaryExpression unary && unary.getType() == Token.NEG && unary.getOperand() instanceof NumberLiteral number) return number(-number.getNumber());
		if (node instanceof KeywordLiteral keyword) {
			return switch (keyword.getType()) {
				case Token.TRUE -> Boolean.TRUE;
				case Token.FALSE -> Boolean.FALSE;
				case Token.NULL -> JSONObject.NULL;
				default -> throw new IllegalArgumentException("Dynamic value");
			};
		}
		if (node instanceof TemplateLiteral template && template.getSubstitutions().isEmpty()) {
			var text = new StringBuilder();
			for (TemplateCharacters part : template.getTemplateStrings()) text.append(part.getValue());
			return text.toString();
		}
		if (node instanceof ArrayLiteral array) {
			var out = new JSONArray();
			for (var element : array.getElements()) out.put(value(element, depth + 1));
			return out;
		}
		if (node instanceof ObjectLiteral object) {
			var out = new JSONObject();
			for (var element : object.getElements()) {
				// a spread or a method is not a literal value
				if (!(element instanceof ObjectProperty property) || property.isMethod()) throw new IllegalArgumentException("Dynamic value");
				var key = property.getKey();
				String name;
				if (key instanceof Name identifier) name = identifier.getIdentifier();
				else if (key instanceof StringLiteral text) name = text.getValue();
				else if (key instanceof NumberLiteral number) name = key.toSource();
				else throw new IllegalArgumentException("Dynamic key");
				try {
					out.put(name, value(property.getValue(), depth + 1));
				} catch (org.codehaus.jettison.json.JSONException invalid) {
					throw new IllegalArgumentException(invalid);
				}
			}
			return out;
		}
		throw new IllegalArgumentException("Dynamic value");
	}

	private static Object number(double value) {
		if (!Double.isFinite(value)) throw new IllegalArgumentException("Invalid number");
		return value == Math.rint(value) && Math.abs(value) < 9.007199254740992E15 ? (Object) (long) value : (Object) value;
	}

	private static Header unreadable(String source) {
		if (com.twinsoft.convertigo.engine.Engine.logBeans != null) {
			boolean first;
			synchronized (WARNED) {
				first = WARNED.add(org.apache.commons.codec.digest.DigestUtils.sha256Hex(source));
				if (WARNED.size() > 128) WARNED.remove(WARNED.iterator().next());
			}
			if (first) com.twinsoft.convertigo.engine.Engine.logBeans.warn("Static requestable header is unavailable");
			else com.twinsoft.convertigo.engine.Engine.logBeans.debug("Static requestable header is unavailable");
		}
		return new Header(null, true);
	}
}
