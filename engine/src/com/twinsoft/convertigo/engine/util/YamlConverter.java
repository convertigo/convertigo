package com.twinsoft.convertigo.engine.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class YamlConverter {
	private static final String inc = "  ";
	private static final String sep = ": ";
	
	private final Pattern toSplit = Pattern.compile("\\n");
	
	private final Matcher toQuote = Pattern.compile("(?:^(?:-|\\?|:|,|\\[|\\]|\\{|\\}|#|&|\\*|\\!|\\||>|'|\"|%|@|`|\\\\s))|(?:: )", Pattern.MULTILINE).matcher("");
	
	private final Matcher parse = Pattern.compile("( *)(?:- )?(↑)?(→)?(↓)?(.*?): (.*)").matcher("");
	private static final int P_INDENT = 1;
	private static final int P_ATTR   = 2;
	private static final int P_TXT    = 3;
	private static final int P_CHILD  = 4;
	private static final int P_KEY    = 5;
	private static final int P_VALUE  = 6;

	private StringBuilder sb;
	private BufferedReader br;
	private Document doc;
	private String line = "";
	private String lastLine = null;
	
	private YamlConverter() {
		
	}
	
	private void writeYamlText(String indent, String txt) {
		if (txt.isEmpty()) {
			txt = "''";
		} else if (toQuote.reset(txt).find()) {
			txt = '\'' + txt.replace("'", "''") + '\'';
		}
		String[] lines = toSplit.split(txt);
		if (lines.length == 1) {
			sb.append(txt);
		} else {
			sb.append("|");
			for (String line: lines) {
				sb.append('\n').append(indent).append(line);
			}
		}
	}
	
	private void writeYamlElement(String indent, Element element, boolean inArray) {
		String nextIndent = indent + inc;
		String txtIndent = nextIndent;
		
		boolean isBean = element.hasAttribute("yaml_key") && element.getTagName().equals("bean");
		
		if (!isBean) {
			txtIndent += inc;
		}
		
		String key = isBean ? element.getAttribute("yaml_key") : element.getTagName();
		sb.append(indent).append(inArray ? "- " : "").append(key).append(sep);
		
		int len = sb.length();
		
		NamedNodeMap attributes = element.getAttributes();
		int attributesLength = attributes.getLength();
		for (int i = 0; i < attributesLength; i++) {
			Attr attr = (Attr) attributes.item(i);
			String name = attr.getName();
			if (!name.startsWith("yaml_")) {
				sb.append('\n').append(nextIndent);
				if (!isBean) {
					sb.append("- ");
					inArray = true;
				}
				sb.append('↑').append(name).append(sep);
				writeYamlText(txtIndent, attr.getValue());
			}
		}
		
		Node child = element.getFirstChild();		
		while (child != null) {
			if (child instanceof Element) {
				Element eChild = (Element) child;
				sb.append('\n');
				writeYamlElement(nextIndent, eChild, !isBean);
			} else if (child instanceof CDATASection) {
				CDATASection cdata = (CDATASection) child;
				String txt = cdata.getData();
				sb.append('\n').append(nextIndent).append('→').append(sep);
				writeYamlText(txtIndent, txt);
			} else if (child.getNodeType() == Node.TEXT_NODE && child.getNextSibling() == null) {
				String txt = child.getNodeValue();
				if (len != sb.length() && inArray) {
					sb.append('\n').append(nextIndent).append("- ").append("→→").append(sep);
				}
				writeYamlText(txtIndent, txt);
			}
			child = child.getNextSibling();
		}
	}
	
	private void readYamlArray(Node parent, String indent, boolean isBean) throws Exception {
		while(checkLine()) {
			parse.reset(line);
			if (!parse.matches()) {
				throw new Exception("no match: " + line);
			}
			if (parse.group(P_INDENT).length() == indent.length()) {
				String tagName = parse.group(P_KEY);
				Element elt = doc.createElement(tagName);
				parent.appendChild(elt);
				String value = parse.group(P_VALUE); 
				if (!value.isEmpty()) {
					value = readYalmText(value, indent);
					elt.setTextContent(value);
				} else {
					readYamlArray(elt, indent + inc + inc, isBean);
				}
			} else {
				restoreLine();
				return;
			}
		}
	}
	
	private void readYamlElement(Element elt, String indent, boolean isBean) throws Exception {
		while(checkLine()) {
			parse.reset(line);
			if (!parse.matches()) {
				throw new Exception("no match: " + line);
			}
			if (parse.group(P_INDENT).length() != indent.length()) {
				restoreLine();
				return;
			}
			if (parse.group(P_ATTR) != null) {
				String name = parse.group(P_KEY);
				String value = parse.group(P_VALUE);
				value = readYalmText(value, indent);
				elt.setAttribute(name, value);
			} else if (parse.group(P_TXT) != null) {
				String key = parse.group(P_KEY);
				String value = parse.group(P_VALUE);
				value = readYalmText(value, indent);
				if (key.equals("→")) {
					elt.appendChild(doc.createTextNode(value));
				} else {
					elt.appendChild(doc.createCDATASection(value));
				}
			} else if (parse.group(P_CHILD) != null) {
				if (isBean) {
					Element nElt = doc.createElement("bean");
					elt.appendChild(nElt);
					nElt.setAttribute("yaml_key", parse.group(P_KEY));
					readYamlElement(nElt, indent + inc, isBean);
				} else {
					readYamlArray(elt, indent, isBean);
				}
			} else {
				String key = parse.group(P_KEY);
				String value = parse.group(P_VALUE);
				value = readYalmText(value, indent);
				Element nElt = doc.createElement(key);
				elt.appendChild(nElt);
				nElt.setTextContent(value);
				readYamlElement(nElt, indent + inc, false);
			}
		}
	}
	
	private String readYalmText(String value, String indent) throws IOException {
		if (value.equals("|")) {
			indent += inc;
			StringBuilder sb = new StringBuilder();
			while (checkLine()) {
				if (line.startsWith(indent)) {
					if (sb.length() != 0) {
						sb.append('\n');
					}
					sb.append(line.substring(indent.length()));
				} else {
					restoreLine();
					break;
				}
			}
			value = sb.toString();
		}
		if (value.startsWith("'")) {
			value = value.substring(1, value.length() - 1).replace("''", "'");
		}
		return value;
	}
	
	private void restoreLine() {
		lastLine = line;
	}
	
	private boolean checkLine() throws IOException {
		if (lastLine != null) {
			line = lastLine;
			lastLine = null;
			return true;
		}
		return line != null && (line = br.readLine()) != null;
	}
	
	public static Document readYaml(File yaml) throws Exception {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(yaml), "UTF-8"))) {
			YamlConverter y = new YamlConverter();
			y.doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			y.doc.appendChild(y.doc.createElement("convertigo"));
			y.br = br;
			try {
				y.readYamlElement(y.doc.getDocumentElement(), "", true);
			} catch (Exception e) {
				y.br.close();
				return y.doc;
			}
			y.br.close();
			return y.doc;
		} catch (Exception e) {
			throw e;
		}
	}
	
	public static void writeYaml(Document document, File yaml) throws IOException {
		YamlConverter y = new YamlConverter();
		y.sb = new StringBuilder();
		Node node = document.getDocumentElement().getFirstChild();
		while (node != null) {
			if (node instanceof Element) {
				if (node.getPreviousSibling() != null) {
					y.sb.append('\n');
				}
				y.writeYamlElement("", (Element) node, false);
			}
			node = node.getNextSibling();
		}
		FileUtils.write(yaml, y.sb.toString(), "UTF-8");
	}
}