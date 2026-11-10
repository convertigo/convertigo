/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.sun.net.httpserver.HttpServer;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

/**
 * A requestable runs with the class loader of its project as context class loader (DirClassLoader, the jars of the
 * project first). A project shipping its own Xerces or Xalan in its libs must still get working and hardened XML
 * parsers and transformers from XMLUtils, and must not change those of the other projects and of the engine served
 * by the same thread.
 */
public class XMLUtilsProjectLibrariesTest {
	private static final String JDK_PARSER = "com.sun.org.apache.xerces.internal.";
	private static final String JDK_TRANSFORMER = "com.sun.org.apache.xalan.internal.";
	private static final List<String> logged = new ArrayList<String>();
	private static Logger previousLogger;
	private static HttpServer witness;
	private static final AtomicInteger witnessHits = new AtomicInteger();

	private Path root;

	@BeforeClass
	public static void setUpClass() throws Exception {
		EnginePropertiesManager.initProperties();
		previousLogger = Engine.logEngine;
		Engine.logEngine = new Logger("xml-project-libraries-test") {
			@Override
			public void error(Object message) {
				logged.add(String.valueOf(message));
			}

			@Override
			public void warn(Object message) {
				logged.add(String.valueOf(message));
			}
		};
		// the implementations are pinned once, outside of any project
		assertNotNull(XMLUtils.getDefaultDocumentBuilder());
		witness = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		witness.createContext("/", exchange -> {
			witnessHits.incrementAndGet();
			byte[] body = "<!ENTITY leak \"LEAKED\">".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		witness.start();
	}

	@AfterClass
	public static void tearDownClass() {
		witness.stop(0);
		Engine.logEngine = previousLogger;
	}

	@Before
	public void setUp() throws Exception {
		root = Files.createTempDirectory("convertigo-xml-libs-test-");
		logged.clear();
	}

	@After
	public void tearDown() throws Exception {
		try (var paths = Files.walk(root)) {
			paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
		}
	}

	private static File jarOf(Class<?> cls) throws Exception {
		File jar = new File(cls.getProtectionDomain().getCodeSource().getLocation().toURI());
		assertTrue("the jar of the engine: " + jar, jar.isFile());
		return jar;
	}

	private static final String[] XERCES = { "org/apache/xerces/", "META-INF/services/javax.xml.parsers." };
	private static final String[] XALAN = { "org/apache/xalan/", "org/apache/xml/", "org/apache/xpath/",
			"META-INF/services/javax.xml.transform.TransformerFactory" };

	/**
	 * a project whose libs hold a copy of the entries of the engine jar that has the class library, with these
	 * prefixes (the jars of the engine may be flattened in one)
	 */
	private DirClassLoader project(String name, Class<?> library, String... prefixes) throws Exception {
		Path libs = Files.createDirectories(root.resolve(name).resolve("libs"));
		if (library != null) {
			try (ZipFile source = new ZipFile(jarOf(library));
					ZipOutputStream copy = new ZipOutputStream(Files.newOutputStream(libs.resolve(name + ".jar")))) {
				for (ZipEntry entry : Collections.list(source.entries())) {
					for (String prefix : prefixes) {
						if (!entry.isDirectory() && entry.getName().startsWith(prefix)) {
							copy.putNextEntry(new ZipEntry(entry.getName()));
							source.getInputStream(entry).transferTo(copy);
							copy.closeEntry();
							break;
						}
					}
				}
			}
		}
		return new DirClassLoader(libs.toFile(), XMLUtils.class.getClassLoader());
	}

	/** a project with the real Xerces jar of the engine in its libs (or its entries, if the jars are flattened) */
	private DirClassLoader projectWithXerces(String name) throws Exception {
		File xerces = jarOf(org.apache.xerces.parsers.XIncludeAwareParserConfiguration.class);
		DirClassLoader loader;
		if (xerces.getName().startsWith("xercesImpl")) {
			Path libs = Files.createDirectories(root.resolve(name).resolve("libs"));
			Files.copy(xerces.toPath(), libs.resolve(xerces.getName()));
			loader = new DirClassLoader(libs.toFile(), XMLUtils.class.getClassLoader());
		} else {
			loader = project(name, org.apache.xerces.parsers.XIncludeAwareParserConfiguration.class, XERCES);
		}
		assertNotSame("the project has a Xerces of its own",
				XMLUtils.class.getClassLoader().loadClass("org.apache.xerces.parsers.XIncludeAwareParserConfiguration"),
				loader.loadClass("org.apache.xerces.parsers.XIncludeAwareParserConfiguration"));
		return loader;
	}

	/** runs task in the current thread with this context class loader, as a requestable of its project */
	private static <T> T in(ClassLoader contextClassLoader, Callable<T> task) throws Exception {
		Thread thread = Thread.currentThread();
		ClassLoader previous = thread.getContextClassLoader();
		thread.setContextClassLoader(contextClassLoader);
		try {
			return task.call();
		} finally {
			thread.setContextClassLoader(previous);
		}
	}

	/** runs task in a new thread */
	private static void inNewThread(Callable<Void> task) throws Throwable {
		AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
		Thread thread = new Thread(() -> {
			try {
				task.call();
			} catch (Throwable t) {
				failure.set(t);
			}
		});
		thread.start();
		thread.join();
		if (failure.get() != null) {
			throw failure.get();
		}
	}

	/** what a requestable does with XMLUtils: the class of the documents built and parsed */
	private static String parseAndPrint() throws Exception {
		Document document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		document.appendChild(document.createElement("document"));
		assertTrue(XMLUtils.prettyPrintDOM(document).contains("<document"));

		Document parsed = XMLUtils.parseDOMFromString("<document><item>1</item></document>");
		assertEquals("1", parsed.getDocumentElement().getTextContent());
		assertSame(document.getClass().getClassLoader(), parsed.getClass().getClassLoader());

		XMLUtils.saxParse(new ByteArrayInputStream("<document/>".getBytes(StandardCharsets.UTF_8)), new DefaultHandler());
		return document.getClass().getName();
	}

	private static String pinned() {
		return XMLUtils.getDefaultDocumentBuilder().getClass().getName().startsWith(JDK_PARSER) ? "jdk" : "pinned";
	}

	@Test
	public void aProjectShippingXercesGetsTheParsersOfTheJdk() throws Throwable {
		try (DirClassLoader projectClassLoader = projectWithXerces("a")) {
			inNewThread(() -> {
				String documentClass = in(projectClassLoader, XMLUtilsProjectLibrariesTest::parseAndPrint);
				assertTrue(documentClass, documentClass.startsWith(JDK_PARSER));
				return null;
			});
		}
	}

	@Test
	public void aProjectWithoutXmlLibraryKeepsThePinnedImplementation() throws Throwable {
		try (DirClassLoader projectClassLoader = project("b", null)) {
			inNewThread(() -> {
				in(projectClassLoader, XMLUtilsProjectLibrariesTest::parseAndPrint);
				Document document = in(projectClassLoader, () -> XMLUtils.getDefaultDocumentBuilder().newDocument());
				assertSame("the implementation pinned by XMLUtils", XMLUtils.class.getClassLoader(), document.getClass().getClassLoader());
				return null;
			});
		}
	}

	@Test
	public void oneThreadServesProjectsAndTheEngineInAnyOrder() throws Throwable {
		try (DirClassLoader a = projectWithXerces("a"); DirClassLoader b = project("b", null)) {
			ClassLoader engine = XMLUtils.class.getClassLoader();
			inNewThread(() -> {
				for (ClassLoader[] order : new ClassLoader[][] { { a, b, engine }, { engine, b, a }, { a, engine, a, b } }) {
					for (ClassLoader loader : order) {
						in(loader, XMLUtilsProjectLibrariesTest::parseAndPrint);
						assertEquals(loader == a ? "jdk" : "pinned", in(loader, XMLUtilsProjectLibrariesTest::pinned));
					}
				}
				return null;
			});
		}
	}

	@Test
	public void aProjectShippingXalanTransformsWithTheJdk() throws Throwable {
		try (DirClassLoader projectClassLoader = project("x", org.apache.xalan.processor.TransformerFactoryImpl.class, XALAN)) {
			assertNotSame("the project has a Xalan of its own", org.apache.xalan.processor.TransformerFactoryImpl.class,
					projectClassLoader.loadClass("org.apache.xalan.processor.TransformerFactoryImpl"));
			inNewThread(() -> in(projectClassLoader, () -> {
				parseAndPrint();
				var transformer = XMLUtils.getNewTransformer(new StreamSource(new StringReader(
						"<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
						+ "<xsl:template match=\"/\"><out><xsl:value-of select=\"count(//item)\"/></out></xsl:template>"
						+ "</xsl:stylesheet>")));
				assertTrue(transformer.getClass().getName(), transformer.getClass().getName().startsWith(JDK_TRANSFORMER));
				StringWriter out = new StringWriter();
				transformer.transform(new DOMSource(XMLUtils.parseDOMFromString("<d><item/><item/></d>")), new StreamResult(out));
				assertTrue(out.toString(), out.toString().contains("<out>2</out>"));
				return null;
			}));
		}
	}

	/** external entity, external DTD and parameter entity: none may be resolved */
	private void assertNoResolution() throws Exception {
		Path marker = Files.writeString(root.resolve("marker.txt"), "SECRET-MARKER");
		String base = "http://127.0.0.1:" + witness.getAddress().getPort();
		String[] payloads = {
			"<!DOCTYPE d [<!ENTITY e SYSTEM \"" + marker.toUri() + "\">]><d>&e;</d>",
			"<!DOCTYPE d SYSTEM \"" + base + "/external.dtd\"><d>&leak;</d>",
			"<!DOCTYPE d [<!ENTITY % p SYSTEM \"" + base + "/parameter.dtd\"> %p;]><d>&leak;</d>"
		};
		for (String payload : payloads) {
			try {
				Document document = XMLUtils.getDefaultDocumentBuilder().parse(new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8)));
				String text = document.getDocumentElement().getTextContent();
				assertFalse(text, text.contains("SECRET-MARKER") || text.contains("LEAKED"));
			} catch (SAXException e) {
				// refused: nothing resolved
			}
			StringBuilder saxText = new StringBuilder();
			try {
				XMLUtils.saxParse(new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8)), new DefaultHandler() {
					@Override
					public void characters(char[] ch, int start, int length) {
						saxText.append(ch, start, length);
					}
				});
			} catch (SAXException e) {
				// refused: nothing resolved
			}
			assertFalse(saxText.toString(), saxText.toString().contains("SECRET-MARKER") || saxText.toString().contains("LEAKED"));
			try {
				XMLUtils.parseDOMFromString(payload);
				fail("the secure builder accepts no DOCTYPE: " + payload);
			} catch (SAXException e) {
				// refused, without NullPointerException
			}
		}
		assertEquals("no external DTD nor parameter entity fetched", 0, witnessHits.get());
	}

	@Test
	public void noExternalEntityIsResolvedWithTheXercesOfAProject() throws Throwable {
		try (DirClassLoader projectClassLoader = projectWithXerces("a")) {
			inNewThread(() -> in(projectClassLoader, () -> {
				assertNoResolution();
				return null;
			}));
		}
	}

	@Test
	public void noExternalEntityIsResolvedWithThePinnedImplementation() throws Throwable {
		inNewThread(() -> {
			assertNoResolution();
			return null;
		});
	}

	/** a factory refusing some settings, as an old implementation could */
	private static class RefusingFactory extends DocumentBuilderFactory {
		private final DocumentBuilderFactory delegate = DocumentBuilderFactory.newDefaultInstance();
		private final List<String> refusing;

		RefusingFactory(String... refusing) {
			this.refusing = List.of(refusing);
		}

		@Override
		public void setFeature(String name, boolean value) throws ParserConfigurationException {
			if (refusing.contains(name)) {
				throw new ParserConfigurationException("not supported: " + name);
			}
			delegate.setFeature(name, value);
		}

		@Override
		public boolean getFeature(String name) throws ParserConfigurationException {
			return delegate.getFeature(name);
		}

		@Override
		public javax.xml.parsers.DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
			return delegate.newDocumentBuilder();
		}

		@Override
		public void setAttribute(String name, Object value) {
			delegate.setAttribute(name, value);
		}

		@Override
		public Object getAttribute(String name) {
			return delegate.getAttribute(name);
		}
	}

	@Test
	public void thePinnedImplementationAcceptsEveryEssentialSetting() throws Exception {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		assertTrue(documentBuilderFactory.getClass().getName(), documentBuilderFactory.getClass().getName().startsWith("org.apache.xerces."));
		assertEquals(List.of(), XMLUtils.harden(documentBuilderFactory, false));
		assertEquals(List.of(), XMLUtils.harden(DocumentBuilderFactory.newInstance(), true));
		javax.xml.parsers.SAXParserFactory saxParserFactory = javax.xml.parsers.SAXParserFactory.newInstance();
		assertTrue(saxParserFactory.getClass().getName(), saxParserFactory.getClass().getName().startsWith("org.apache.xerces."));
		assertEquals(List.of(), XMLUtils.harden(saxParserFactory));
		assertTrue(logged.toString(), logged.isEmpty());
	}

	@Test
	public void aRefusedSettingIsNamedAndAnEssentialOneRejectsTheFactory() throws Exception {
		List<String> refused = XMLUtils.harden(new RefusingFactory(XMLUtils.SECURE_PROCESSING), false);
		assertEquals("secure processing bounds the expansion of entities", List.of(XMLUtils.SECURE_PROCESSING), refused);
		assertTrue(logged.toString(), logged.stream().anyMatch(line -> line.contains(XMLUtils.SECURE_PROCESSING) && line.contains("not used")));

		logged.clear();
		refused = XMLUtils.harden(new RefusingFactory(XMLUtils.SECURE_PROCESSING), true);
		assertTrue("the secure builder accepts no DOCTYPE: " + refused, refused.isEmpty());
		assertTrue(logged.toString(), logged.stream().anyMatch(line -> line.contains(XMLUtils.SECURE_PROCESSING)));

		logged.clear();
		refused = XMLUtils.harden(new RefusingFactory(XMLUtils.EXTERNAL_GENERAL_ENTITIES), false);
		assertEquals(List.of(XMLUtils.EXTERNAL_GENERAL_ENTITIES), refused);
		assertTrue(logged.toString(), logged.stream().anyMatch(line -> line.contains(XMLUtils.EXTERNAL_GENERAL_ENTITIES) && line.contains("not used")));

		refused = XMLUtils.harden(new RefusingFactory(XMLUtils.DISALLOW_DOCTYPE), true);
		assertEquals(List.of(XMLUtils.DISALLOW_DOCTYPE), refused);
	}
}
