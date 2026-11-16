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

import java.lang.reflect.Proxy;
import java.util.AbstractList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.xml.soap.MessageFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.servlets.WebServiceServlet;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class VisibilityLogDefaultsTest {
	private static final String MARKER = "synthetic-a(b$\\value";

	@Test public void credentialFamiliesAreMaskedWithoutTreatingOrdinaryKeysAsSecrets() {
		// Raw payloads/headers, password variants, authentication tokens and explicitly credential-like keys.
		for (String name : List.of("__body", "__header_X_Custom", "PASSWORD", "passphrase", "dbPasswd", "pwd", "client_secret",
				"userToken", "apikey", "api_key", "X-API-Key", "credentials", "Authorization", "privateKey",
				"awsAccessKey", "signing_key", "encryptionKey", "clientKey")) {
			assertTrue(name, Visibility.Logs.isMasked(name, 0));
			assertFalse(name, Visibility.Logs.printValue(name, 0, MARKER).contains(MARKER));
			assertEquals(MARKER, Visibility.Studio.printValue(name, 0, MARKER));
		}
		// A generic database key or normal query input has no credential semantics.
		for (String name : List.of("key", "rowKey", "primaryKey", "monkey", "search", "page", "limit")) {
			assertFalse(name, Visibility.Logs.isMasked(name, 0));
			assertEquals(MARKER, Visibility.Logs.printValue(name, 0, MARKER));
		}
	}

	@Test public void queryNamesAndHttpAliasesAreLiteralAndTheOriginalMapIsIntact() throws Exception {
		var variable = new RequestableHttpVariable();variable.setName("ordinary");
		variable.setHttpName("q$[x]");variable.setVisibility(Visibility.Logs.getMask());
		String query = "password=" + MARKER + "&q$[x]=" + MARKER + "&search=visible";
		String logged = (String) Visibility.Logs.replaceVariables(List.of(variable), query);
		assertFalse(logged.contains(MARKER));assertTrue(logged.contains("search=visible"));
		Map<String, Object> values = new LinkedHashMap<>();values.put("api_key", MARKER);values.put("search", "visible");
		Object loggedMap = Visibility.Logs.replaceVariables(null, values);
		assertFalse(loggedMap.toString().contains(MARKER));assertEquals(MARKER, values.get("api_key"));
	}

	@Test public void structuredInputMaskingChangesOnlyItsCopyEvenWithoutVariableDefinitions() throws Exception {
		Document input = XMLUtils.createDom();var root=input.createElement("input");input.appendChild(root);
		var variables=input.createElement("transaction-variables");root.appendChild(variables);
		var body=input.createElement("variable");body.setAttribute("name","__body");variables.appendChild(body);
		var nested=input.createElement("field");nested.setAttribute("name","password");nested.setTextContent(MARKER);body.appendChild(nested);
		var headers=input.createElement("transaction-headers");root.appendChild(headers);
		var header=input.createElement("header");header.setAttribute("name","X-Custom");header.setAttribute("value",MARKER);headers.appendChild(header);
		String original=XMLUtils.prettyPrintDOM(input);
		Document logged=(Document)Visibility.Logs.replaceVariables(null,input);
		String text=XMLUtils.prettyPrintDOM(logged);
		assertFalse(text.contains(MARKER));assertTrue(text.contains(Visibility.STRING_MASK));
		assertEquals(original,XMLUtils.prettyPrintDOM(input));
	}

	@Test public void soapLoggingMasksACopyRatherThanTheExecutionMessage() throws Exception {
		var source=MessageFactory.newInstance().createMessage();
		var method=source.getSOAPBody().addChildElement("operation");
		method.addChildElement("password").setTextContent(MARKER);source.saveChanges();
		var request=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{HttpServletRequest.class},
				(proxy,called,args)->called.getName().equals("getAttribute") && WebServiceServlet.REQUEST_MESSAGE_ATTRIBUTE.equals(args[0]) ? source : called.getName().equals("getCharacterEncoding") ? "UTF-8" : null);
		String logged=(String)Visibility.Logs.replaceVariables(null,request);
		assertFalse(logged.contains(MARKER));assertTrue(logged.contains(Visibility.STRING_MASK));
		assertEquals(MARKER,source.getSOAPBody().getTextContent());
	}

	@Test public void failedLogPreparationPreservesTheOutputTypeWithoutReturningOriginalData() throws Exception {
		List<Variable> broken=new AbstractList<>() {
			@Override public int size() { return 1; }
			@Override public Variable get(int index) { throw new IllegalStateException("synthetic failure"); }
		};
		assertEquals(Visibility.STRING_MASK,Visibility.Logs.replaceVariables(broken,"password="+MARKER));
		assertFalse(Visibility.Logs.replaceVariables(broken,Map.of("password",MARKER)).toString().contains(MARKER));
		Document source=XMLUtils.createDom();var root=source.createElement("input");root.setTextContent(MARKER);source.appendChild(root);
		Object logged=Visibility.Logs.replaceVariables(broken,source);
		assertTrue(logged instanceof Document);assertFalse(XMLUtils.prettyPrintDOM((Document)logged).contains(MARKER));
		assertEquals(MARKER,source.getDocumentElement().getTextContent());
	}
}
