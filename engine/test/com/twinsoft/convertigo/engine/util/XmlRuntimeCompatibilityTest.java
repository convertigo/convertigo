/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ServiceLoader;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;
import org.hibernate.cfg.Configuration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.eviware.soapui.DefaultSoapUICore;
import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.support.wsdl.WsdlImporter;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

public class XmlRuntimeCompatibilityTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	private static final String TEXT = "A & <B> \u00e9";

	@Before public void setup() throws Exception {
		Engine.logEngine = Engine.logContext = Logger.getLogger("xml-runtime-test");
		EnginePropertiesManager.initProperties();
	}

	@Test public void engineDomParsingAndWritingKeepTextAndNamespaces() throws Exception {
		var document = XMLUtils.createDom();
		var root = document.createElementNS("urn:compat", "p:sample");
		root.setAttribute("xmlns:p", "urn:compat");
		document.appendChild(root);
		root.appendChild(document.createCDATASection(TEXT));
		var xml = XMLUtils.prettyPrintDOM(document);
		var restored = XMLUtils.parseDOMFromString(xml);
		assertEquals("p:sample", restored.getDocumentElement().getNodeName());
		assertEquals(TEXT, restored.getDocumentElement().getTextContent().trim());
		assertTrue(xml.contains("urn:compat"));
	}

	@Test public void staxFactoriesAndServiceDiscoveryRoundTripXml() throws Exception {
		var input = XMLInputFactory.newFactory();
		var output = XMLOutputFactory.newFactory();
		var events = XMLEventFactory.newFactory();
		System.out.println("XML-FACTORY input " + input.getClass().getName());
		System.out.println("XML-FACTORY output " + output.getClass().getName());
		System.out.println("XML-FACTORY events " + events.getClass().getName());
		for (var provider : ServiceLoader.load(XMLInputFactory.class)) {
			var discoveredReader = provider.createXMLStreamReader(new StringReader("<sample/>"));
			assertNotNull(discoveredReader);
			discoveredReader.close();
		}
		var xml = new StringWriter();
		var writer = output.createXMLStreamWriter(xml);
		try {
			writer.writeStartDocument("UTF-8", "1.0");
			writer.writeStartElement("p", "sample", "urn:compat");
			writer.writeNamespace("p", "urn:compat");
			writer.writeAttribute("id", "42");
			writer.writeCharacters(TEXT);
			writer.writeEndElement();
			writer.writeEndDocument();
		} finally {
			writer.close();
		}
		var reader = input.createXMLStreamReader(new StringReader(xml.toString()));
		var text = new StringBuilder();
		try {
			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT) {
					assertEquals("urn:compat", reader.getNamespaceURI());
					assertEquals("42", reader.getAttributeValue(null, "id"));
				}
				if (event == XMLStreamConstants.CHARACTERS) text.append(reader.getText());
			}
		} finally {
			reader.close();
		}
		assertEquals(TEXT, text.toString());
		assertEquals(TEXT, events.createCharacters(TEXT).getData());
		var eventReader = input.createXMLEventReader(new StringReader(xml.toString()));
		try {
			int starts = 0;
			while (eventReader.hasNext()) if (eventReader.nextEvent().isStartElement()) starts++;
			assertEquals(1, starts);
		} finally {
			eventReader.close();
		}
	}

	@XmlRootElement(name = "sample")
	public static class BoundValue {
		public String value;
	}

	@Test public void jakartaJaxbRuntimeAndEngineXsdConversionsRemainAvailable() throws Exception {
		var context = JAXBContext.newInstance(BoundValue.class);
		System.out.println("XML-JAXB runtime " + context.getClass().getName());
		var value = new BoundValue();
		value.value = TEXT;
		var xml = new StringWriter();
		context.createMarshaller().marshal(value, xml);
		var restored = (BoundValue) context.createUnmarshaller().unmarshal(new StringReader(xml.toString()));
		assertEquals(TEXT, restored.value);
		assertEquals(true, XsdTypeParser.parse(new QName("boolean"), "true"));
		assertEquals(42, XsdTypeParser.parse(new QName("int"), "42"));
		assertArrayEquals(new byte[] {1, 2, 3}, (byte[]) XsdTypeParser.parse(new QName("base64Binary"), "AQID"));
		assertArrayEquals(new byte[] {10, 15}, (byte[]) XsdTypeParser.parse(new QName("hexBinary"), "0A0F"));
	}

	@Test public void soapMessagesRoundTripThroughAxiomAndSaaj() throws Exception {
		for (String protocol : new String[] {SOAPConstants.SOAP_1_1_PROTOCOL, SOAPConstants.SOAP_1_2_PROTOCOL}) {
			var factory = MessageFactory.newInstance(protocol);
			var message = factory.createMessage();
			message.getSOAPBody().addBodyElement(new QName("urn:compat", "Echo", "p")).addTextNode(TEXT);
			message.saveChanges();
			var output = new ByteArrayOutputStream();
			message.writeTo(output);
			var restored = factory.createMessage(message.getMimeHeaders(), new ByteArrayInputStream(output.toByteArray()));
			assertEquals(TEXT, restored.getSOAPBody().getElementsByTagNameNS("urn:compat", "Echo").item(0).getTextContent());
			System.out.println("XML-SOAP " + protocol + " " + factory.getClass().getName());
		}
	}

	@Test public void importsALocalWsdlAndGeneratesASoapRequest() throws Exception {
		var root = folder.newFolder("soapui").toPath();
		String previousHome = System.getProperty("soapui.home");
		System.setProperty("soapui.home", root.toString());
		WsdlProject project = null;
		try {
			new DefaultSoapUICore(root.toString(), root.resolve("settings.xml").toString());
			var wsdl = root.resolve("echo.wsdl");
			try (var source = getClass().getResourceAsStream("/xml/echo.wsdl")) {
				assertNotNull(source);
				Files.copy(source, wsdl);
			}
			project = new WsdlProject();
			var interfaces = WsdlImporter.importWsdl(project, wsdl.toUri().toString());
			assertEquals(1, interfaces.length);
			assertEquals(1, interfaces[0].getOperationCount());
			var request = interfaces[0].getOperationAt(0).createRequest(true);
			assertTrue(request, request.contains("EchoRequest"));
			assertEquals("Envelope", XMLUtils.parseDOMFromString(request).getDocumentElement().getLocalName());
			System.out.println("XML-WSDL local import and SOAP request generation OK");
		} finally {
			if (project != null) project.release();
			SoapUI.shutdown();
			if (previousHome == null) System.clearProperty("soapui.home");
			else System.setProperty("soapui.home", previousHome);
		}
	}

	@Entity(name = "XmlCompatibilityValue")
	public static class StoredValue {
		@Id public Long id;
		public String payload;
	}

	@Test public void hibernateLoadsXmlConfigurationAndPersistsAValue() throws Exception {
		var file = folder.newFile("hibernate.cfg.xml").toPath();
		Files.writeString(file, """
				<hibernate-configuration xmlns="http://www.hibernate.org/xsd/orm/cfg">
				  <session-factory>
				    <property name="hibernate.connection.driver_class">org.hsqldb.jdbc.JDBCDriver</property>
				    <property name="hibernate.connection.url">jdbc:hsqldb:mem:xmlCompatibility</property>
				    <property name="hibernate.connection.username">sa</property>
				    <property name="hibernate.hbm2ddl.auto">create-drop</property>
				    <mapping class="%s"/>
				  </session-factory>
				</hibernate-configuration>
				""".formatted(StoredValue.class.getName()), StandardCharsets.UTF_8);
		try (var factory = new Configuration().configure(file.toFile()).buildSessionFactory();
				var session = factory.openSession()) {
			var transaction = session.beginTransaction();
			var value = new StoredValue();
			value.id = 1L;
			value.payload = TEXT;
			session.persist(value);
			transaction.commit();
			session.clear();
			assertEquals(TEXT, session.find(StoredValue.class, 1L).payload);
		}
		System.out.println("XML-HIBERNATE XML configuration and HSQLDB round trip OK");
	}
}
