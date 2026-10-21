/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Random;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.transactions.HttpTransaction;
import com.twinsoft.convertigo.engine.AttachmentManager.AttachmentDetails;
import com.twinsoft.convertigo.engine.translators.WebServiceTranslator;

public class AttachmentManagerTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();

	@Before public void setup() throws Exception {
		Engine.logEngine = Engine.logBeans = Logger.getLogger("attachment-test");
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
	}

	@Test public void fileMetadataDoesNotLoadThePayloadAndUsesALongSize() throws Exception {
		var path = folder.newFile().toPath();
		long length = (long) Integer.MAX_VALUE + 123;
		try (var file = new RandomAccessFile(path.toFile(), "rw")) {
			file.setLength(length);
			file.seek(length - 1);
			file.write(42);
		}
		var xml = attachment();
		xml.setAttribute("local-url", path.toString());
		xml.setAttribute("content-length", "7"); // Stale XML metadata must not truncate the response.
		var details = AttachmentManager.getAttachment(xml);
		assertNotNull(details);
		assertEquals(length, details.getLength());
		assertEquals("file.bin", details.getName());
		assertEquals("application/octet-stream", details.getContentType());
		try (var input = details.getInputStream()) {
			input.skipNBytes(length - 1);
			assertEquals(42, input.read());
			assertEquals(-1, input.read());
		}
	}

	@Test public void legacyFileDataIsCompleteAndStreamsCanBeReopened() throws Exception {
		var bytes = new byte[128 * 1024 + 37];
		new Random(1237).nextBytes(bytes);
		var path = folder.newFile().toPath();
		Files.write(path, bytes);
		var xml = attachment();
		xml.setAttribute("local-url", path.toString());
		var details = AttachmentManager.getAttachment(xml);
		assertArrayEquals(bytes, details.getData());
		assertSame(details.getData(), details.getData());
		for (int i = 0; i < 2; i++) {
			try (var input = details.getInputStream()) {
				assertArrayEquals(bytes, input.readAllBytes());
			}
		}
	}

	@Test public void missingFilesDirectoriesAndUnsupportedEncodingsAreRejected() throws Exception {
		var xml = attachment();
		xml.setAttribute("local-url", folder.getRoot().toPath().resolve("missing.bin").toString());
		assertNull(AttachmentManager.getAttachment(xml));
		xml.setAttribute("local-url", folder.getRoot().toString());
		assertNull(AttachmentManager.getAttachment(xml));
		xml.removeAttribute("local-url");
		xml.setAttribute("encoding", "unknown");
		assertNull(AttachmentManager.getAttachment(xml));
		xml.setAttribute("encoding", "base64");
		xml.removeAttribute("type");
		assertNull(AttachmentManager.getAttachment(xml));
	}

	@Test public void deletedFileReportsAnIoFailureRatherThanReturningPartialData() throws Exception {
		var path = folder.newFile().toPath();
		var xml = attachment();
		xml.setAttribute("local-url", path.toString());
		var details = AttachmentManager.getAttachment(xml);
		Files.delete(path);
		assertThrows(IOException.class, details::getInputStream);
		assertThrows(IOException.class, details::getLength);
		assertThrows(UncheckedIOException.class, details::getData);
	}

	@Test public void base64IncludingEmptyContentKeepsItsExistingDecoding() throws Exception {
		for (var bytes : new byte[][] { new byte[0], new byte[] { 0, 1, -1, 13, 10, 37 } }) {
			var xml = attachment();
			xml.setAttribute("encoding", "base64");
			xml.setTextContent(Base64.encodeBase64String(bytes));
			var details = AttachmentManager.getAttachment(xml);
			assertArrayEquals(bytes, details.getData());
			assertEquals(bytes.length, details.getLength());
			try (var input = details.getInputStream()) {
				assertArrayEquals(bytes, input.readAllBytes());
			}
		}
	}

	@Test public void oldAttachmentImplementationsHaveCompatibleStreamDefaults() throws Exception {
		var bytes = new byte[] { 1, 2, 3 };
		AttachmentDetails details = new AttachmentDetails() {
			public byte[] getData() { return bytes; }
			public String getName() { return "old"; }
			public String getContentType() { return "application/octet-stream"; }
		};
		assertEquals(3, details.getLength());
		try (var input = details.getInputStream()) {
			assertArrayEquals(bytes, input.readAllBytes());
		}
	}

	@Test public void soapStillEmbedsLocalFileContentsAsBase64() throws Exception {
		var bytes = new byte[] { 0, 1, -1, 13, 10, 37 };
		var path = folder.newFile().toPath();
		Files.write(path, bytes);
		var xml = attachment();
		xml.setAttribute("local-url", path.toString());
		var context = new Context("attachment-soap");
		context.project = new Project();
		context.requestedObject = new HttpTransaction();
		var message = MessageFactory.newInstance().createMessage();
		var envelope = message.getSOAPPart().getEnvelope();
		// Exercise the real SOAP attachment translation without the engine schema manager.
		var method = WebServiceTranslator.class.getDeclaredMethod("addSoapElement", Context.class,
				javax.xml.soap.SOAPEnvelope.class, javax.xml.soap.SOAPElement.class, org.w3c.dom.Node.class);
		method.setAccessible(true);
		var element = (javax.xml.soap.SOAPElement) method.invoke(new WebServiceTranslator(null), context,
				envelope, envelope.getBody(), xml);
		assertArrayEquals(bytes, Base64.decodeBase64(element.getTextContent()));
	}

	public static Element attachment() throws Exception {
		var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		var root = document.createElement("document");
		document.appendChild(root);
		var element = document.createElement("attachment");
		root.appendChild(element);
		element.setAttribute("type", "attachment");
		element.setAttribute("name", "file.bin");
		element.setAttribute("content-type", "application/octet-stream");
		return element;
	}
}
