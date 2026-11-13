/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.studio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionStack;
import com.twinsoft.convertigo.beans.ngx.components.UIElement;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.steps.XMLCopyStep;
import com.twinsoft.convertigo.beans.transactions.HttpTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.admin.events.AdminEvent;
import com.twinsoft.convertigo.engine.admin.events.AdminEventBus;

public class StudioRevealTest {
	@BeforeClass
	public static void initialize() throws Exception {
		Engine.logBeans = Engine.logEngine = Engine.logDatabaseObjectManager = Engine.logStudio = Engine.logAdmin =
				Logger.getLogger("studio-reveal-test");
		EnginePropertiesManager.initProperties();
	}

	private static <T extends DatabaseObject> T named(T databaseObject, String name) throws Exception {
		databaseObject.setName(name);
		databaseObject.isSubLoaded = true;
		return databaseObject;
	}

	private static JSONObject describe(DatabaseObject databaseObject) throws Exception {
		return StudioReveal.describe(databaseObject);
	}

	@Test
	public void aSequenceAndWhatItHoldsShowTheFlowOfTheSequence() throws Exception {
		var project = named(new Project(), "Shop");
		var sequence = named(new GenericSequence(), "Checkout");
		project.add(sequence);
		var step = named(new XMLCopyStep(), "copy");
		sequence.add(step);
		var variable = named(new RequestableVariable(), "cart");
		sequence.add(variable);

		var event = describe(sequence);
		assertEquals("Shop", event.getString("project"));
		assertEquals(sequence.getFullQName(), event.getString("id"));
		assertEquals("sequence", event.getString("kind"));
		assertEquals(sequence.getFullQName(), event.getString("sequence"));
		assertEquals("", event.getString("page"));

		event = describe(step);
		assertEquals("the id is the one of the tree of the web Studio", step.getFullQName(), event.getString("id"));
		assertTrue(event.getString("id").contains(".sq:Checkout."));
		assertEquals("sequence.child", event.getString("kind"));
		assertEquals(sequence.getFullQName(), event.getString("sequence"));

		assertEquals("sequence.child", describe(variable).getString("kind"));
		assertEquals("project", describe(project).getString("kind"));
		assertEquals("", describe(project).getString("sequence"));
	}

	@Test
	public void theComponentsOfAnNgxApplicationShowItsPreviewOnTheirPage() throws Exception {
		var project = named(new Project(), "Shop");
		var mobile = named(new MobileApplication(), "MobileApplication");
		project.add(mobile);
		var application = named(new ApplicationComponent(), "Application");
		mobile.add(application);
		var page = named(new PageComponent(), "Cart");
		application.add(page);
		var element = named(new UIElement(), "title");
		page.add(element);
		var shared = named(new UISharedComponent(), "Badge");
		application.add(shared);
		var sharedElement = named(new UIElement(), "label");
		shared.add(sharedElement);
		var action = named(new UIActionStack(), "Pay");
		application.add(action);

		var event = describe(page);
		assertEquals("ngx.page", event.getString("kind"));
		assertEquals(page.getFullQName(), event.getString("page"));
		assertEquals(application.getFullQName(), event.getString("application"));
		assertEquals("", event.getString("sequence"));

		event = describe(element);
		assertEquals("ngx.component", event.getString("kind"));
		assertEquals("the page of a component, which the preview opens", page.getFullQName(), event.getString("page"));

		assertEquals("ngx.application", describe(application).getString("kind"));
		assertEquals("ngx.shared", describe(shared).getString("kind"));
		assertEquals("a component of a shared component has no page", "ngx.shared", describe(sharedElement).getString("kind"));
		assertEquals("", describe(sharedElement).getString("page"));
		assertEquals("ngx.component", describe(action).getString("kind"));
		assertEquals("object", describe(mobile).getString("kind"));
	}

	@Test
	public void connectorsAndTransactionsAreNamedByTheirKind() throws Exception {
		var project = named(new Project(), "Shop");
		var connector = named(new HttpConnector(), "Http");
		project.add(connector);
		var transaction = named(new HttpTransaction(), "Call");
		connector.add(transaction);
		assertEquals("connector", describe(connector).getString("kind"));
		assertEquals("transaction", describe(transaction).getString("kind"));
	}

	@Test
	public void aRevealIsPublishedToTheWebStudiosThatFollowIt() throws Exception {
		var project = named(new Project(), "Shop");
		var sequence = named(new GenericSequence(), "Checkout");
		project.add(sequence);
		var step = named(new XMLCopyStep(), "copy");
		sequence.add(step);

		List<AdminEvent> received = new ArrayList<>();
		assertFalse(AdminEventBus.hasSubscribers(StudioReveal.TOPIC));
		var result = new JSONObject(StudioReveal.publish(step));
		assertEquals(result.toString(), StudioReveal.PUBLISHED, result.getString("status"));
		assertFalse("no web Studio follows it yet", result.getBoolean("listening"));

		try (var other = AdminEventBus.subscribe(List.of("projects.changed"), received::add);
				var studio = AdminEventBus.subscribe(List.of("projects.changed", "studio.*"), received::add)) {
			assertTrue(AdminEventBus.hasSubscribers(StudioReveal.TOPIC));
			result = new JSONObject(StudioReveal.publish(step));
			assertEquals(result.toString(), StudioReveal.PUBLISHED, result.getString("status"));
			assertTrue(result.getBoolean("listening"));
			assertEquals(step.getFullQName(), result.getString("id"));
			assertEquals("sequence.child", result.getString("kind"));
		}
		assertFalse(AdminEventBus.hasSubscribers(StudioReveal.TOPIC));
		assertEquals("only the subscriber of the topic receives it", 1, received.size());
		var event = received.get(0);
		assertEquals(StudioReveal.TOPIC, event.topic());
		assertEquals("Shop", event.payload().getString("project"));
		assertEquals(step.getFullQName(), event.payload().getString("id"));
		assertEquals(sequence.getFullQName(), event.payload().getString("sequence"));
	}

	@Test
	public void theChangeAnAgentTellsGoesWithItsReveal() throws Exception {
		var project = named(new Project(), "Shop");
		var sequence = named(new GenericSequence(), "Checkout");
		project.add(sequence);
		var step = named(new XMLCopyStep(), "copy");
		sequence.add(step);
		Map<String, DatabaseObject> loaded = Map.of("Shop", project, "Shop.Checkout", sequence, "Shop.Checkout.copy",
				step, sequence.getFullQName(), sequence);

		var change = StudioReveal.changeOf("{\"change\":\"renamed\",\"previous\":\"Shop.Checkout.copy0\","
				+ "\"tool\":\"databaseobject-rename\",\"summary\":\"Renamed copy0 to copy\","
				+ "\"touched\":[\"Shop.Checkout\",\"Shop.Unknown\"],\"other\":1}", loaded::get);
		assertEquals("renamed", change.getString("change"));
		assertEquals("Shop.Checkout.copy0", change.getString("previous"));
		assertEquals("databaseobject-rename", change.getString("tool"));
		assertEquals("Renamed copy0 to copy", change.getString("summary"));
		assertEquals("the objects touched by their full QNames, the ones found", 1,
				change.getJSONArray("touched").length());
		assertEquals(sequence.getFullQName(), change.getJSONArray("touched").getString(0));
		assertFalse("only the keys of a change", change.has("other"));

		assertEquals("created", StudioReveal.changeOf(" created ", loaded::get).getString("change"));
		assertFalse("an unknown change is not told", StudioReveal.changeOf("exploded", loaded::get).has("change"));
		assertEquals("details not read are ignored", 0, StudioReveal.changeOf("{not json", loaded::get).length());
		assertEquals(0, StudioReveal.changeOf(null, loaded::get).length());
		assertEquals(200, StudioReveal.changeOf("{\"summary\":\"" + "x".repeat(300) + "\"}", loaded::get)
				.getString("summary").length());

		assertEquals("a step removed shows in its sequence", sequence,
				StudioReveal.nearestLoaded("Shop.Checkout.removed", loaded::get));
		assertEquals("a sequence removed shows in its project", project,
				StudioReveal.nearestLoaded("Shop.Removed", loaded::get));
		assertEquals(null, StudioReveal.nearestLoaded("Other.Removed", loaded::get));

		List<AdminEvent> received = new ArrayList<>();
		try (var studio = AdminEventBus.subscribe(List.of(StudioReveal.TOPIC), received::add)) {
			StudioReveal.publish(step, change);
			StudioReveal.publish(step);
		}
		assertEquals(2, received.size());
		var told = received.get(0).payload();
		assertEquals(step.getFullQName(), told.getString("id"));
		assertEquals("renamed", told.getString("change"));
		assertEquals("Shop.Checkout.copy0", told.getString("previous"));
		var plain = received.get(1).payload();
		assertFalse("a reveal without details is the one of an older MCP", plain.has("change"));
		assertEquals(6, plain.length());
	}

	@Test
	public void anEngineWithoutWebStudioRevealsNothing() throws Exception {
		// a server engine: the web Studio is disabled by default
		List<AdminEvent> received = new ArrayList<>();
		try (var studio = AdminEventBus.subscribe(List.of(StudioReveal.TOPIC), received::add)) {
			var result = new JSONObject(StudioReveal.reveal("Shop.Checkout.copy"));
			assertEquals(StudioReveal.UNAVAILABLE, result.getString("status"));
			result = new JSONObject(StudioReveal.revealApplication("Shop"));
			assertEquals(StudioReveal.UNAVAILABLE, result.getString("status"));
			result = new JSONObject(StudioReveal.revealChange("Shop.Checkout", "{\"change\":\"deleted\"}"));
			assertEquals(StudioReveal.UNAVAILABLE, result.getString("status"));
		}
		assertTrue(received.isEmpty());
	}
}
