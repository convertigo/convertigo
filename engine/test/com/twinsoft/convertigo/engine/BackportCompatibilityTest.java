/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.servlets.SecurityHeadersFilter;
import com.twinsoft.convertigo.engine.util.ServletUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/** Positive compatibility checks for existing 8.3 project and static resource behavior. */
public class BackportCompatibilityTest {
	@BeforeClass
	public static void initialize() throws Exception {
		EnginePropertiesManager.initProperties();
		Engine.logEngine = Engine.logContext = Engine.logBeans = Logger.getLogger("backport-compatibility");
	}

	@Test
	public void ordinaryXmlWithNamespacesStillParses() throws Exception {
		var document = XMLUtils.getDefaultDocumentBuilder().parse(
				new InputSource(new StringReader("<f:fixture xmlns:f=\"urn:fixture\">content</f:fixture>")));
		assertEquals("f:fixture", document.getDocumentElement().getNodeName());
		assertEquals("content", document.getDocumentElement().getTextContent());
	}

	@Test
	public void smtpStylesheetKeepsLiteralHtmlAttributes() throws Exception {
		String stylesheet = """
				<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
				  <xsl:template match="/">
				    <html><body><a href="https://example.invalid/mail" style="color:blue">Mail</a>
				    <img src="cid:fixture"/></body></html>
				  </xsl:template>
				</xsl:stylesheet>
				""";
		var xsl = XMLUtils.getDefaultDocumentBuilder().parse(new InputSource(new StringReader(stylesheet)));
		var input = XMLUtils.getDefaultDocumentBuilder().parse(new InputSource(new StringReader("<mail/>")));
		var output = new StringWriter();
		XMLUtils.getNewTransformer(new DOMSource(xsl)).transform(new DOMSource(input), new StreamResult(output));
		assertTrue(output.toString().contains("href=\"https://example.invalid/mail\""));
		assertTrue(output.toString().contains("style=\"color:blue\""));
		assertTrue(output.toString().contains("src=\"cid:fixture\""));
	}

	@Test
	public void jacksonProjectValuesRoundTripWithTheObjectFilter() throws Exception {
		var value = JsonNodeFactory.instance.objectNode().put("fixture", "value").put("count", 2);
		var document = XMLUtils.createDom();
		var serialized = (Element) XMLUtils.writeObjectToXml(document, value);
		assertEquals(value, XMLUtils.readObjectFromXml(serialized));
	}

	@Test
	public void importingTestVariablesPreservesTheirSymbolSource() throws Exception {
		var sequence = new GenericSequence();
		var variable = new RequestableVariable();
		variable.setName("fixture");
		variable.setValueOrNull("resolved-fixture");
		String symbol = "$" + "{fixture}";
		variable.setCompilablePropertySourceValue("value", symbol);
		sequence.addVariable(variable);
		var testCase = new TestCase();
		testCase.importRequestableVariables(sequence);
		assertEquals(symbol, testCase.getVariable("fixture").getCompilablePropertySourceValue("value"));
		assertEquals("resolved-fixture", testCase.getVariable("fixture").getValueOrNull());
	}

	@Test
	public void historicalConnectorDefaultsKeepReplicationPoliciesOnFullSyncOnly() throws Exception {
		try (var input = getClass().getResourceAsStream("/com/twinsoft/convertigo/beans/database_objects_default.xml")) {
			assertNotNull(input);
			var document = XMLUtils.getDefaultDocumentBuilder().parse(input);
			var connectors = document.getElementsByTagName("connector");
			int fullSyncDefaults = 0;
			int couchDefaults = 0;
			for (int i = 0; i < connectors.getLength(); i++) {
				var connector = (Element) connectors.item(i);
				String classname = connector.getAttribute("classname");
				if (!classname.endsWith(".FullSyncConnector") && !classname.endsWith(".CouchDbConnector")) {
					continue;
				}
				Element policy = null;
				var properties = connector.getElementsByTagName("property");
				for (int j = 0; j < properties.getLength(); j++) {
					var property = (Element) properties.item(j);
					if ("replicationAccess".equals(property.getAttribute("name"))) { policy = property; }
				}
				if (classname.endsWith(".FullSyncConnector")) {
					fullSyncDefaults++;
					assertNotNull("FullSync defaults must have the allow policy", policy);
					assertEquals("allow", ((Element) policy.getElementsByTagName("java.lang.String").item(0)).getAttribute("value"));
				} else {
					couchDefaults++;
					assertNull("The ordinary CouchDB connector has no replication policy", policy);
				}
			}
			assertEquals(2, fullSyncDefaults);
			assertEquals(1, couchDefaults);
		}
	}

	@Test
	public void extensionlessPdfKeepsItsMimeTypeWithNosniff() throws Exception {
		var file = Files.createTempFile("convertigo-mime-fixture", "");
		byte[] content = "%PDF-1.4\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
		Files.write(file, content);
		try {
			Map<String, String> headers = new HashMap<>();
			var bytes = new ByteArrayOutputStream();
			var output = new ServletOutputStream() {
				@Override public boolean isReady() { return true; }
				@Override public void setWriteListener(WriteListener listener) {}
				@Override public void write(int b) { bytes.write(b); }
			};
			var request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
					new Class<?>[] {HttpServletRequest.class}, (proxy, method, args) -> switch (method.getName()) {
						case "getDateHeader" -> -1L;
						default -> null;
					});
			var response = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
					new Class<?>[] {HttpServletResponse.class}, (proxy, method, args) -> switch (method.getName()) {
						case "setHeader" -> { headers.put((String) args[0], (String) args[1]); yield null; }
						case "getOutputStream" -> output;
						default -> null;
					});
			var context = (ServletContext) Proxy.newProxyInstance(getClass().getClassLoader(),
					new Class<?>[] {ServletContext.class}, (proxy, method, args) -> null);
			var config = (FilterConfig) Proxy.newProxyInstance(getClass().getClassLoader(),
					new Class<?>[] {FilterConfig.class}, (proxy, method, args) ->
						"getServletContext".equals(method.getName()) ? context : null);
			new SecurityHeadersFilter().doFilter(request, response, (req, res) ->
					ServletUtils.handleFileFilter(file.toFile(), request, response, config,
							(nextReq, nextRes) -> fail("The existing static file should be served directly")));
			assertEquals("nosniff", headers.get("X-Content-Type-Options"));
			assertEquals("application/pdf", headers.get("Content-Type"));
			assertArrayEquals(content, bytes.toByteArray());
		} finally {
			Files.delete(file);
		}
	}
}
