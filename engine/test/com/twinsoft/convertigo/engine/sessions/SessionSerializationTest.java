/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details: <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.sessions;

import java.io.StringReader;
import java.security.KeyPairGenerator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.util.DomSerializationSupport;
import com.twinsoft.convertigo.engine.util.DomSerializationSupport.DomType;
import com.twinsoft.convertigo.engine.util.DomSerializationSupport.SerializedDom;

/** Standalone regression suite; optional --redis uses an explicitly configured test Redis. */
public final class SessionSerializationTest {
	private static final SessionValueCodec CODEC = new SessionValueCodec();
	private static int checks;

	public static void main(String[] args) throws Exception {
		Engine.logRedis = org.apache.log4j.Logger.getLogger("session-test");
		Engine.logEngine = Engine.logRedis;
		EnginePropertiesManager.initProperties();
		var properties = new Properties();
		properties.setProperty("price", "2.50");
		properties.setProperty("unicode", "é & < > \n");
		check(roundTrip(properties) instanceof Properties, "Properties type");
		check(properties.equals(roundTrip(properties)), "Properties values");
		var defaults = new Properties();
		defaults.setProperty("fallback", "inherited");
		var inherited = new Properties(defaults);
		inherited.setProperty("price", "3");
		check("inherited".equals(((Properties) roundTrip(inherited)).getProperty("fallback")), "effective Properties defaults");
		check(CODEC.deserialize("value", "{\"price\":\"2.50\"}") instanceof LinkedHashMap, "legacy untyped JSON remains a map");
		check(roundTrip(Map.of("price", "2.50")) instanceof Map, "plain Map compatibility");
		check(roundTrip(List.of("a", "b")).equals(List.of("a", "b")), "plain List compatibility");
		check(roundTrip("xml string").equals("xml string"), "string compatibility");

		var document = parse("<?before keep?><r:root xmlns:r='urn:root' xmlns:a='urn:attr' a:id='42'>before <r:child><![CDATA[a < b]]></r:child> after<!--keep--></r:root>");
		var root = document.getDocumentElement();
		assertNode(document, Document.class);
		assertNode(root, Element.class);
		assertNode(root.getFirstChild(), org.w3c.dom.Text.class);
		assertNode(root.getElementsByTagNameNS("urn:root", "child").item(0).getFirstChild(), org.w3c.dom.CDATASection.class);
		assertNode(root.getLastChild(), org.w3c.dom.Comment.class);
		assertNode(document.getFirstChild(), org.w3c.dom.ProcessingInstruction.class);
		assertNode(root.getAttributeNodeNS("urn:attr", "id"), org.w3c.dom.Attr.class);
		var namespaceContext = parse("<outer xmlns:t='urn:outer' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'><inner xmlns:t='urn:inner'><value xsi:type='t:T'/></inner></outer>");
		var nestedElement = (Element) namespaceContext.getElementsByTagName("value").item(0);
		var detachedElement = (Element) roundTrip(nestedElement);
		check("urn:inner".equals(detachedElement.lookupNamespaceURI("t")), "inherited QName namespace, nearest binding wins");
		check(!nestedElement.hasAttribute("xmlns:t"), "serialization does not mutate source DOM");
		assertNode(parse("<empty/>").getImplementation().createDocument(null, null, null), Document.class);
		var fragment = document.createDocumentFragment();
		fragment.appendChild(document.createTextNode("prefix "));
		fragment.appendChild(root.cloneNode(true));
		assertNode(fragment, org.w3c.dom.DocumentFragment.class);

		var nodes = snapshot(document, root, root.getFirstChild(), root.getAttributeNodeNS("urn:attr", "id"));
		var restoredNodes = (NodeList) roundTrip(nodes);
		check(restoredNodes.getLength() == 4, "NodeList length");
		check(restoredNodes.item(0) instanceof Document && restoredNodes.item(1) instanceof Element, "precise NodeList item types");
		check(restoredNodes.item(2).isEqualNode(nodes.item(2)) && restoredNodes.item(3).isEqualNode(nodes.item(3)), "heterogeneous NodeList contents");
		check(restoredNodes.item(-1) == null && restoredNodes.item(4) == null, "NodeList bounds");
		check(((NodeList) roundTrip(snapshot())).getLength() == 0, "empty NodeList");
		check(((NodeList) roundTrip(root.getElementsByTagNameNS("urn:root", "child"))).getLength() == 1, "queried NodeList");
		check(SessionAttributeFilter.sanitizeValue(null, "document", document) == document, "Document no longer filtered");
		check(SessionAttributeFilter.sanitizeValue(null, "session:internal", document) == null, "technical attributes remain filtered");
		var contextCodec = new ContextValueCodec();
		check(contextCodec.deserialize(contextCodec.serialize(properties)) instanceof Properties, "context Properties");
		check(((Document) contextCodec.deserialize(contextCodec.serialize(document))).isEqualNode(document), "context DOM compatibility");
		check(DomSerializationSupport.deserialize(new SerializedDom(DomType.DOCUMENT, "<legacy/>")) instanceof Document, "legacy stored DOM format");
		check(DomSerializationSupport.deserializeDocument("<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///must-not-read'>]><x>&e;</x>") == null, "XXE rejected");
		check(DomSerializationSupport.deserializeDocument("<!DOCTYPE x [<!ENTITY e 'expanded'>]><x>&e;</x>") == null, "internal DTD rejected");
		check(DomSerializationSupport.deserializeElement("<broken>") == null, "invalid XML rejected");
		var entityDocument = parse("<root/>");
		entityDocument.getDocumentElement().appendChild(entityDocument.createEntityReference("custom"));
		check(!CODEC.canSerialize("value", entityDocument), "unresolved entity nodes rejected before storage");
		var hostile = "{\"clazz\":\"org.w3c.dom.Document\",\"format\":\"dom\",\"value\":{\"type\":\"DOCUMENT\",\"xml\":\"<!DOCTYPE x SYSTEM 'http://127.0.0.1:1/no'><x/>\"}}";
		try {
			CODEC.deserialize("value", hostile);
			throw new AssertionError("Hostile DOM must fail deserialization");
		} catch (IllegalArgumentException expected) {
			checks++;
		}
		checkSignedDocument();
		SessionStore store = args.length > 0 && "--redis".equals(args[0])
				? new RedisSessionStore(RedisSessionConfiguration.fromProperties()) : new MemoryStore();
		try {
			checkSession(store, properties, document, root, nodes);
		} finally {
			store.shutdown();
			if (store instanceof RedisSessionStore) RedisClients.getClient().shutdown();
		}
		System.out.println("PASS: " + checks + " session serialization checks (" + store.getClass().getSimpleName() + ")");
	}

	private static void checkSession(SessionStore store, Object... values) throws Exception {
		var configuration = RedisSessionConfiguration.fromProperties();
		var id = "serialization-test-" + UUID.randomUUID();
		try {
			var first = RedisHttpSession.newSession(store, null, configuration, id);
			for (int i = 0; i < values.length; i++) first.setAttribute("value" + i, values[i]);
			first.flush();
			var meta = store.readMeta(id);
			check(meta != null, "session persisted");
			var second = RedisHttpSession.fromMeta(store, null, configuration, id, meta);
			check(second.getAttribute("value0") instanceof Properties, "Properties through flush/new session");
			check(((Document) second.getAttribute("value1")).isEqualNode((Node) values[1]), "Document through flush/new session");
			check(second.getAttribute("value2") instanceof Element, "Element through flush/new session");
			check(second.getAttribute("value3") instanceof NodeList, "NodeList through flush/new session");
			check(store.readAttribute(id, "value1").contains("org.w3c.dom.Document"), "stable W3C interface in Redis JSON");
		} finally {
			store.delete(id);
		}
	}

	private static void checkSignedDocument() throws Exception {
		var document = parse("<r:root xmlns:r='urn:test'>a <r:item/> b</r:root>");
		var generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		var keys = generator.generateKeyPair();
		var factory = XMLSignatureFactory.getInstance("DOM");
		var reference = factory.newReference("", factory.newDigestMethod(DigestMethod.SHA256, null),
				List.of(factory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)), null, null);
		var info = factory.newSignedInfo(factory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,
				(C14NMethodParameterSpec) null), factory.newSignatureMethod(SignatureMethod.RSA_SHA256, null), List.of(reference));
		factory.newXMLSignature(info, null).sign(new DOMSignContext(keys.getPrivate(), document.getDocumentElement()));
		var restored = (Document) roundTrip(document);
		var validation = new DOMValidateContext(keys.getPublic(), restored.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0));
		check(factory.unmarshalXMLSignature(validation).validate(validation), "XML signature survives DOM round-trip");
	}

	private static Object roundTrip(Object value) throws Exception {
		return CODEC.deserialize("value", CODEC.serialize("value", value));
	}

	private static void assertNode(Node value, Class<?> expected) throws Exception {
		var restored = roundTrip(value);
		check(expected.isInstance(restored), "DOM type " + expected.getSimpleName());
		check(value.isEqualNode((Node) restored), "DOM content " + expected.getSimpleName());
	}

	private static Document parse(String xml) throws Exception {
		var factory = DocumentBuilderFactory.newDefaultInstance();
		factory.setNamespaceAware(true);
		return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
	}

	private static NodeList snapshot(Node... nodes) {
		return new NodeList() {
			public Node item(int index) { return index >= 0 && index < nodes.length ? nodes[index] : null; }
			public int getLength() { return nodes.length; }
		};
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
		checks++;
	}

	private static final class MemoryStore implements SessionStore {
		private final Map<String, Map<String, String>> sessions = new HashMap<>();
		public SessionStoreMeta readMeta(String id) {
			var data = sessions.get(id);
			return data == null ? null : new SessionStoreMeta(Long.parseLong(data.get(SessionStoreKeys.META_CREATION)),
					Long.parseLong(data.get(SessionStoreKeys.META_LAST_ACCESS)), Integer.parseInt(data.get(SessionStoreKeys.META_MAX_INACTIVE)));
		}
		public String readAttribute(String id, String name) { return sessions.getOrDefault(id, Map.of()).get(name); }
		public Set<String> readAttributeNames(String id) { return sessions.getOrDefault(id, Map.of()).keySet(); }
		public void writeDelta(String id, Map<String, String> hset, Set<String> hdel, long ttlMillis) {
			var data = sessions.computeIfAbsent(id, key -> new HashMap<>());
			data.putAll(hset);
			hdel.forEach(data::remove);
		}
		public void delete(String id) { sessions.remove(id); }
		public void shutdown() { }
	}
}
