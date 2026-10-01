/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.util.Set;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchDbManager;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchDbManager.FullSyncAuthentication;

/** Checks the provider API with the 8.3 engine classpath and ordinary document fixtures. */
public class FullSyncDocumentAclTest {
	private final CouchDbManager manager = new CouchDbManager();

	private FullSyncAuthentication user(String name, String... groups) {
		return manager.new FullSyncAuthentication() {
			@Override public String getAuthenticatedUser() { return name; }
			@Override public Set<String> getGroups() { return Set.of(groups); }
		};
	}

	private JSONObject document() throws Exception {
		return new JSONObject().put("_id", "fixture").put("_rev", "1-fixture");
	}

	@Test
	public void ownerAndAnonymousPermissionsUseTheDocumentAcl() throws Exception {
		JSONObject document = document().put(CouchKey.c8oAcl.key(), "alice");
		assertTrue(manager.checkDocumentACL(document, user("alice")));
		assertFalse(manager.checkDocumentACL(document, user("bob")));
		assertFalse(manager.checkDocumentACL(document, null));
	}

	@Test
	public void bothGroupFormatsRemainCompatible() throws Exception {
		JSONObject document = document().put("c8oGrp", "team");
		assertTrue(manager.checkDocumentACL(document, user("alice", "team")));
		assertFalse(manager.checkDocumentACL(document, user("bob", "other")));
		document.put("c8oGrp", new JSONObject().put("team", true).put("other", false));
		assertTrue(manager.checkDocumentACL(document, user("alice", "team")));
		assertFalse(manager.checkDocumentACL(document, user("bob", "other")));
	}

	@Test
	public void publicDocumentsAndCheckpointsKeepTheirExistingAccess() throws Exception {
		assertTrue(manager.checkDocumentACL(document(), null));
		assertTrue(manager.checkDocumentACL(document().put("_id", "_local/checkpoint"), null));
	}

	@Test
	public void directChecksDoNotGrantAProtectedDeletedRevisionAutomatically() throws Exception {
		JSONObject document = document().put("_deleted", true).put(CouchKey.c8oAcl.key(), "alice");
		assertTrue(manager.checkDocumentACL(document, user("alice")));
		assertFalse(manager.checkDocumentACL(document, user("bob")));
		assertFalse(manager.checkDocumentACL(document, null));
	}
}
