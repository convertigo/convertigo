/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import com.twinsoft.convertigo.engine.enums.FullSyncAnonymousReplication;
import com.twinsoft.convertigo.engine.enums.FullSyncReplicationAccess;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;
import com.twinsoft.convertigo.engine.servlets.FullSyncServlet.ReplicationPolicy;
import com.twinsoft.convertigo.engine.servlets.FullSyncServlet.RequestParser;

public class FullSyncReplicationPolicyTest {
	private static final ReplicationPolicy PULL_ONLY = new ReplicationPolicy(
			FullSyncReplicationAccess.pullOnly, FullSyncAnonymousReplication.deny);

	@Test
	public void sharedDatabaseRestrictionsCombineIndependentlyOfConnectorOrder() {
		var accesses = List.of(FullSyncReplicationAccess.allow, FullSyncReplicationAccess.pullOnly, FullSyncReplicationAccess.deny);
		for (var a : accesses) {
			for (var b : accesses) {
				for (var anonymousA : FullSyncAnonymousReplication.values()) {
					for (var anonymousB : FullSyncAnonymousReplication.values()) {
						var first = new ReplicationPolicy(a, anonymousA);
						var second = new ReplicationPolicy(b, anonymousB);
						var combined = first.restrict(second);
						assertEquals(second.restrict(first), combined);
						assertEquals(accesses.get(Math.max(accesses.indexOf(a), accesses.indexOf(b))), combined.access());
						assertEquals(anonymousA == FullSyncAnonymousReplication.allow && anonymousB == FullSyncAnonymousReplication.allow
								? FullSyncAnonymousReplication.allow : FullSyncAnonymousReplication.deny, combined.anonymousReplication());
					}
				}
			}
		}
		// A later connector allowing push must not override pull-only or anonymous restrictions.
		assertEquals(PULL_ONLY, new ReplicationPolicy(FullSyncReplicationAccess.pullOnly, FullSyncAnonymousReplication.allow)
				.restrict(new ReplicationPolicy(FullSyncReplicationAccess.allow, FullSyncAnonymousReplication.deny))
				.restrict(new ReplicationPolicy(FullSyncReplicationAccess.allow, FullSyncAnonymousReplication.allow)));
	}

	@Test
	public void pullKeepsPostReadsAndCheckpointWrites() throws Exception {
		for (var path : List.of("/db/", "/db/document", "/db/document/attachment", "/db/_design/c8o")) {
			assertTrue(path, PULL_ONLY.allows(request(path), HttpMethodType.GET));
			assertTrue(path, PULL_ONLY.allows(request(path), HttpMethodType.HEAD));
		}
		for (var special : List.of("_changes", "_all_docs", "_bulk_get")) {
			assertTrue(special, PULL_ONLY.allows(request("/db/" + special), HttpMethodType.POST));
		}
		for (var method : List.of(HttpMethodType.GET, HttpMethodType.HEAD, HttpMethodType.PUT, HttpMethodType.DELETE)) {
			assertTrue(method.name(), PULL_ONLY.allows(request("/db/_local/checkpoint"), method));
		}
	}

	@Test
	public void pullOnlyRefusesDocumentWritesWhileAllowKeepsPush() throws Exception {
		var allow = new ReplicationPolicy(FullSyncReplicationAccess.allow, FullSyncAnonymousReplication.deny);
		for (var special : List.of("_bulk_docs", "_ensure_full_commit")) {
			var request = request("/db/" + special);
			assertFalse(special, PULL_ONLY.allows(request, HttpMethodType.POST));
			assertTrue(special, allow.allows(request, HttpMethodType.POST));
		}
		// Direct document, attachment, design-document and database writes remain restricted.
		for (var path : List.of("/db/", "/db/document", "/db/document/attachment", "/db/_design/c8o")) {
			for (var method : List.of(HttpMethodType.POST, HttpMethodType.PUT, HttpMethodType.DELETE)) {
				assertFalse(path + " " + method, PULL_ONLY.allows(request(path), method));
			}
		}
		assertFalse(PULL_ONLY.allows(request("/db/_local/checkpoint/attachment"), HttpMethodType.PUT));
		assertFalse(PULL_ONLY.allows(request("/db/_security"), HttpMethodType.GET));
	}

	@Test
	public void denyRefusesReadsAndCheckpoints() throws Exception {
		var deny = new ReplicationPolicy(FullSyncReplicationAccess.deny, FullSyncAnonymousReplication.allow);
		for (var path : List.of("/db/", "/db/document", "/db/_changes", "/db/_bulk_docs", "/db/_local/checkpoint")) {
			for (var method : HttpMethodType.values()) {
				assertFalse(path + " " + method, deny.allows(request(path), method));
			}
		}
	}

	private static RequestParser request(String path) throws Exception {
		var request = (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
				new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> switch (method.getName()) {
					case "getRequestURI" -> "/convertigo/fullsync" + path;
					case "getContextPath" -> "/convertigo";
					case "getServletPath" -> "/fullsync";
					default -> throw new UnsupportedOperationException(method.getName());
				});
		return new RequestParser(request, "");
	}
}
