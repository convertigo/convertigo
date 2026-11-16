/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.enums;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPMessage;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.engine.servlets.WebServiceServlet;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.SOAPUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public enum Visibility {
	Logs(0x01),		// for log files
	Studio(0x02),	// for Studio UI
	Platform(0x04),	// for Platform UI
	XmlFile(0x08);	// for project's XML files

	int mask;
	Visibility(int mask) {
		this.mask = mask;
	}

	public static final String STRING_MASK = "******";

	public int getMask() {
		return mask;
	}

	public boolean isMasked(int visibility) {
		return (visibility & mask) != 0 ? true : false;
	}

	public String printValue(int visibility, Object value) {
		if (value == null) return null;
		return (visibility & mask) == 0 ? toString(value) : maskValue(value);
	}

	private String toString(Object object) {
		String s = null;
		if (object != null) {
			if (object instanceof NodeList) {
				NodeList list = (NodeList)object;
				s = list.toString();
			}
			else if (object instanceof Collection<?>) {
				List<String> list = GenericUtils.toString((Collection<?>)object);
				s = list.toString();
			}
			else if (object instanceof NativeJavaArray) {
				Object ob = ((NativeJavaArray)object).unwrap();
				List<String> list = GenericUtils.toString(Arrays.asList((Object[])ob));
				s = list.toString();
			}
			else if (object instanceof NativeArray) {
				s = (String)((NativeArray)object).getDefaultValue(String.class);
			}
			else if (object instanceof NativeJavaObject) {
				s = (String)((NativeJavaObject)object).getDefaultValue(String.class);
			}
			else if (object.getClass().isArray()) {
				s = Arrays.toString((Object[])object);
			}
			else
				s = object.toString();
		}
		return s;
	}

	public Object replaceValues(List<String> hiddenValues, Object object) {
		if (object == null) return null;
		if (hiddenValues == null) return object;
		try {
			if (hiddenValues.isEmpty()) return object;
			// Case of postQuery from XML requestTemplate
			if (object instanceof String) {
				String literals = hiddenValues.stream().filter(value -> value != null && !value.isEmpty())
						.distinct().sorted(Comparator.comparingInt(String::length).reversed())
						.map(Pattern::quote).collect(Collectors.joining("|"));
				if (literals.isEmpty()) return object;
				// Match once against the original text, so masking a literal '*' never expands previous masks.
				return Pattern.compile(literals).matcher((String) object)
						.replaceAll(Matcher.quoteReplacement(STRING_MASK));
			}
		}
		catch (Exception e) {
			return STRING_MASK;
		}
		return object;
	}

	/** Logs also hide raw request parameters and credential names without changing their stored visibility. */
	public boolean isMasked(String name, int visibility) {
		return isMasked(visibility) || (this == Logs && isSensitiveLogName(name));
	}

	public boolean isMasked(Variable variable) {
		if (variable == null) return false;
		if (isMasked(variable.getVisibility())) return true;
		return this == Logs && getVariableKeyNames(variable).stream().anyMatch(Visibility::isSensitiveLogName);
	}

	private static boolean isSensitiveLogName(String name) {
		if (name == null) return false;
		String normalized = name.toLowerCase(Locale.ROOT);
		if (normalized.equals("__body") || normalized.startsWith("__header_")) return true;
		normalized = normalized.replace("_", "").replace("-", "");
		return normalized.contains("password") || normalized.contains("passphrase") || normalized.contains("passwd") || normalized.contains("pwd")
				|| normalized.contains("secret") || normalized.contains("token") || normalized.contains("apikey")
				|| normalized.contains("credential") || normalized.contains("authorization")
				|| normalized.contains("privatekey") || normalized.contains("accesskey")
				|| normalized.contains("signingkey") || normalized.contains("encryptionkey")
				|| normalized.contains("clientkey");
	}

	public String printValue(String name, int visibility, Object value) {
		return this == Logs && isSensitiveLogName(name) ? logMask(name, value) : printValue(visibility, value);
	}

	public String printValue(Variable variable, Object value) {
		return this == Logs && isMasked(variable) ? logMask(variable.getName(), value)
				: printValue(variable.getVisibility(), value);
	}

	private String logMask(String name, Object value) {
		if (this == Logs && name != null && (name.equalsIgnoreCase("__body")
				|| name.toLowerCase(Locale.ROOT).startsWith("__header_")) && value instanceof CharSequence text) {
			return STRING_MASK + " (" + text.length() + " characters)";
		}
		return STRING_MASK;
	}

	public Object replaceVariables(List<? extends Variable> variableList, Object object) {
		if (object == null) return null;
		if (variableList == null) {
			if (this != Logs) return object;
			variableList = List.of();
		}
		try {
			Set<String> maskedNames = new HashSet<>();
			for (Variable variable : variableList) {
				if (isMasked(variable)) maskedNames.addAll(getVariableKeyNames(variable));
			}
			if (object instanceof String text) {
				String[] parameters = text.split("&", -1);
				for (int i = 0; i < parameters.length; i++) {
					int separator = parameters[i].indexOf('=');
					if (separator < 0) continue;
					String key = parameters[i].substring(0, separator);
					String name = URLDecoder.decode(key, StandardCharsets.UTF_8);
					if (maskedNames.contains(name) || isMasked(name, 0)) {
						parameters[i] = key + "=\"" + logMask(name, parameters[i].substring(separator + 1)) + "\"";
					}
				}
				return String.join("&", parameters);
			}
			if (object instanceof HttpServletRequest request) {
				SOAPMessage source = (SOAPMessage) request.getAttribute(WebServiceServlet.REQUEST_MESSAGE_ATTRIBUTE);
				// SOAP masking must not edit the request used by the transaction.
				var bytes = new ByteArrayOutputStream();
				source.writeTo(bytes);
				SOAPMessage copy = MessageFactory.newInstance(SOAPConstants.DYNAMIC_SOAP_PROTOCOL)
						.createMessage(source.getMimeHeaders(), new ByteArrayInputStream(bytes.toByteArray()));
				SOAPBody body = copy.getSOAPBody();
				for (Iterator<?> methods = body.getChildElements(); methods.hasNext();) {
					Object method = methods.next();
					if (!(method instanceof SOAPElement operation)) continue;
					for (Iterator<?> parameters = operation.getChildElements(); parameters.hasNext();) {
						Object parameter = parameters.next();
						if (!(parameter instanceof SOAPElement element)) continue;
						String name = element.getLocalName();
						if (maskedNames.contains(name) || isMasked(name, 0)) {
							element.setTextContent(logMask(name, element.getTextContent()));
						}
					}
				}
				return SOAPUtils.toString(copy, request.getCharacterEncoding());
			}
			if (object instanceof Map<?, ?>) {
				Map<String, Object> source = GenericUtils.cast(object);
				Map<String, Object> copy = new LinkedHashMap<>(source);
				for (String name : copy.keySet()) {
					if ((maskedNames.contains(name) || isMasked(name, 0)) && copy.get(name) != null) {
						copy.put(name, logMask(name, copy.get(name)));
					}
				}
				return copy;
			}
			if (object instanceof Document source) {
				Document copy = XMLUtils.createDom("java");
				copy.appendChild(copy.importNode(source.getDocumentElement(), true));
				if (this != Logs && maskedNames.isEmpty()) return copy;
				List<Element> named = new ArrayList<>();
				NodeList elements = copy.getElementsByTagName("*");
				for (int i = 0; i < elements.getLength(); i++) {
					Element element = (Element) elements.item(i);
					if (element.hasAttribute("name")) named.add(element);
				}
				boolean input = copy.getDocumentElement().getNodeName().equals("input")
						&& copy.getElementsByTagName("transaction-variables").getLength() > 0;
				for (Element element : named) {
					if (element.getParentNode() == null) continue;
					String name = element.getAttribute("name");
					if (element.getParentNode().getNodeName().equals("transaction-headers")) name = "__header_" + name;
					if (!maskedNames.contains(name) && !isMasked(name, 0)) continue;
					Attr value = element.getAttributeNode("value");
					if (value != null) value.setValue(logMask(name, value.getValue()));
					if (this == Logs || input) {
						if (value == null || element.hasChildNodes()) {
							String mask = logMask(name, element.getTextContent());
							element.setTextContent(mask);
						}
					} else {
						for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
							if (child instanceof Element item && item.getNodeName().equals("value")) item.setTextContent(STRING_MASK);
						}
					}
				}
				return copy;
			}
		} catch (Exception e) {
			// Never return the original values when preparing their log representation fails.
			if (object instanceof Document) {
				try {
					Document masked = XMLUtils.createDom("java");
					Element root = masked.createElement("masked");
					root.setTextContent(STRING_MASK);
					masked.appendChild(root);
					return masked;
				} catch (Exception ignored) { return null; }
			}
			return object instanceof Map<?, ?> ? Map.of("masked", STRING_MASK) : STRING_MASK;
		}
		return object;
	}

	public static String maskValue(Object value) {
		if (value == null) return null;
		if (value.equals("")) return "";
		String regexp = value instanceof String ? ".":"[^\\[\\]\\,]";
		try {
			return value.toString().replaceAll(regexp, "*");
		} catch (Exception e) {}
		return STRING_MASK;
	}

	private static List<String> getVariableKeyNames(Variable variable) {
		List<String> keys = new ArrayList<String>();
		try {
			keys.add(variable.getName());
			Class<?> c = variable.getClass();
			if (c.getName().indexOf(".RequestableHttp") != -1) {
				//java.lang.reflect.Method method = c.getMethod("getHttpName");
				//String key = (String)method.invoke(c);
				String key = "";
				if (variable instanceof RequestableHttpVariable) {
					key = ((RequestableHttpVariable) variable).getHttpName();
				}
				if (!keys.contains(key)) keys.add(key);
			}
		} catch (Exception e) {}
		return keys;
	}

	public static String maskQueryValues(String queryString) {
		if (queryString == null) return null;
		var params = queryString.split("&");
		for (int i = 0; i < params.length; i++) {
			var param = params[i].split("=", 2);
			if (param.length > 1) {
				params[i] = param[0] + "=" + STRING_MASK;
			}
		}
		return String.join("&", params);
	}
}
