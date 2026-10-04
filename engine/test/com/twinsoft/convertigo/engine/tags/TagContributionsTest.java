package com.twinsoft.convertigo.engine.tags;

import static org.junit.Assert.*;
import java.io.IOException;
import org.junit.Test;
import com.twinsoft.convertigo.beans.core.Project;

public class TagContributionsTest {
	private static final String DESCRIPTOR = """
			{"label":"References","fields":{"resources":{"label":"Resources","type":"array","uniqueItems":true,
			"items":{"type":"string","enum":["B1","B2","Mail"]}}}}
			""";
	@Test public void contextualProvidersAreOptionalDetachedAndOutsideTheirMonitor() throws Exception {
		var contributions = new TagContributions();
		contributions.register("extension", context -> {
			assertFalse(Thread.holdsLock(contributions));
			return context.project() == null ? null : TagDocument.parseObject(DESCRIPTOR);
		});
		assertTrue(contributions.descriptors(new TagContributions.Context(TagManager.Scope.workspaceProjects, null)).isEmpty());
		var context = new TagContributions.Context(TagManager.Scope.projectObjects, new Project());
		var descriptor = contributions.descriptors(context); descriptor.removeAll();
		assertTrue(contributions.descriptors(context).has("extension"));
		assertThrows(IOException.class, () -> contributions.register("extension", TagDocument.parseObject(DESCRIPTOR)));
	}
	@Test public void referenceArraysAreOrderedUniqueAndStrictlyChosen() throws Exception {
		var contributions = new TagContributions(); contributions.register("extension", TagDocument.parseObject(DESCRIPTOR));
		var previous = TagDocument.parseObject("{}");
		var next = TagDocument.parseObject("{\"extension\":{\"resources\":[\"Mail\",\"B1\",\"B2\"]}}");
		contributions.validateEdit(previous, next);
		assertEquals("Mail", next.path("extension").path("resources").get(0).asText());
		for (String value : new String[] {"\"B1\"", "[\"B1\",\"B1\"]", "[\"missing\"]", "[false]"}) {
			var invalid = TagDocument.parseObject("{\"extension\":{\"resources\":" + value + "}}");
			assertThrows(IOException.class, () -> contributions.validateEdit(previous, invalid));
		}
	}
	@Test public void missingReferencesCanBePreservedReorderedOrRemovedButNotAdded() throws Exception {
		var contributions = new TagContributions(); contributions.register("extension", TagDocument.parseObject(DESCRIPTOR));
		var previous = TagDocument.parseObject("{\"extension\":{\"resources\":[\"retired\",\"B1\"]}}");
		contributions.validateEdit(previous, TagDocument.parseObject("{\"extension\":{\"resources\":[\"B1\",\"retired\"]}}"));
		contributions.validateEdit(previous, TagDocument.parseObject("{\"extension\":{\"resources\":[\"B1\"]}}"));
		assertThrows(IOException.class, () -> contributions.validateEdit(previous,
				TagDocument.parseObject("{\"extension\":{\"resources\":[\"retired\",\"another missing\"]}}")));
	}
	@Test public void unavailableNamespacesCannotLoseTheirMetadata() throws Exception {
		var contributions = new TagContributions();
		var previous = TagDocument.parseObject("{\"extension\":{\"resources\":[\"B1\"]}}");
		contributions.validateEdit(previous, previous.deepCopy());
		assertThrows(IOException.class, () -> contributions.validateEdit(previous, TagDocument.parseObject("{}")));
	}
}
