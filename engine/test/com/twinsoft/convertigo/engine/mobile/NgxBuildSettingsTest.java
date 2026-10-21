/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.mobile;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Application assets must remain available to consumers and the in-memory dev server. */
public class NgxBuildSettingsTest {
	@Rule public TemporaryFolder temp = new TemporaryFolder();

	private File work(String project) throws Exception {
		return Files.createDirectories(temp.getRoot().toPath().resolve(project + "/_private/ionic")).toFile();
	}

	private JSONObject options() throws Exception {
		return new JSONObject("{outputPath:{base:'../../DisplayObjects/mobile',browser:''}}");
	}

	private JSONObject asset(String input, String output) throws Exception {
		return new JSONObject().put("glob", "**/*").put("input", input).put("output", output);
	}

	@Test public void redundantCopyIsSkippedOnlyInItsOwner() throws Exception {
		var declaration = asset("../../../A/DisplayObjects/mobile/assets/A/icon", "/assets/A/icon/");
		assertTrue(NgxBuilder.isSelfCopyAsset(work("A"), declaration, options()));
		assertFalse(NgxBuilder.isSelfCopyAsset(work("B"), declaration, options()));
	}

	@Test public void normalizesRelativeAbsoluteAndLeadingSlashPaths() throws Exception {
		var work = work("Project with spaces");
		assertTrue(NgxBuilder.isSelfCopyAsset(work,
				asset("../../DisplayObjects/mobile/assets/A/./icon/../icon", "assets/A/icon"), options()));
		var input = new File(work, "../../DisplayObjects/mobile/assets/A/icon").getCanonicalPath();
		assertTrue(NgxBuilder.isSelfCopyAsset(work, asset(input, "/assets/A/icon/"), options()));
	}

	@Test public void preservesDifferentDestinationsAndExternalSources() throws Exception {
		var work = work("A");
		assertFalse(NgxBuilder.isSelfCopyAsset(work,
				asset("../../DisplayObjects/mobile/assets/A/icon", "/assets/A/other/"), options()));
		assertFalse(NgxBuilder.isSelfCopyAsset(work, asset("../../assets/icon", "/assets/A/icon/"), options()));
		assertFalse(NgxBuilder.isSelfCopyAsset(work, asset("src/assets", "assets"), options()));
	}

	@Test public void respectsOutputPathsAndLegacyTemplates() throws Exception {
		var work = work("A");
		var declaration = asset("../../DisplayObjects/mobile/assets/A/icon", "/assets/A/icon/");
		assertTrue(NgxBuilder.isSelfCopyAsset(work, declaration,
				new JSONObject("{outputPath:'../../DisplayObjects/mobile'}")));
		assertFalse(NgxBuilder.isSelfCopyAsset(work, declaration,
				new JSONObject("{outputPath:{base:'../../DisplayObjects/mobile'}}")));
		assertFalse(NgxBuilder.isSelfCopyAsset(work, declaration,
				new JSONObject("{outputPath:'www'}")));
		assertFalse(NgxBuilder.isSelfCopyAsset(work, declaration, new JSONObject()));
		assertFalse(NgxBuilder.isSelfCopyAsset(work, new JSONObject("{glob:'**/*'}"), options()));
	}

	@Test public void doesNotSkipFilesRemovedByThePreBuildCleanup() throws Exception {
		assertFalse(NgxBuilder.isSelfCopyAsset(work("A"),
				asset("../../DisplayObjects/mobile/other", "/other/"), options()));
	}

	@Test public void resolvesSymbolicLinks() throws Exception {
		var work = work("A");
		var source = Files.createDirectories(new File(work, "../../DisplayObjects/mobile/assets/A/icon").toPath());
		var link = work.toPath().resolve("linked-assets");
		try {
			Files.createSymbolicLink(link, source.toRealPath());
		} catch (UnsupportedOperationException | java.io.IOException e) {
			org.junit.Assume.assumeNoException("Symbolic links unavailable", e);
		}
		assertTrue(NgxBuilder.isSelfCopyAsset(work, asset("linked-assets", "assets/A/icon"), options()));
	}
}
