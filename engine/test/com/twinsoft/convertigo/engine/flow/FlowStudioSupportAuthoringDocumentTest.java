package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FlowStudioSupportAuthoringDocumentTest {
	private static final byte[] KEY = "test-installation-key-of-32-byte".getBytes(StandardCharsets.UTF_8);
	private static final String DOCUMENT_KEY = "ab".repeat(32);
	private static final long SLOW = TimeUnit.SECONDS.toNanos(1);
	private Path folder;

	@Before
	public void create() throws Exception {
		folder = Files.createTempDirectory("authoring-documents-");
		FlowAuthoringDiskCache.use(new FlowAuthoringDiskCache(folder, FlowAuthoringDiskCache.DEFAULT_MAX_BYTES, KEY));
	}

	@After
	public void remove() throws Exception {
		FlowAuthoringDiskCache.use(null);
		FlowAuthoringDiskCache.secrets = List::of;
		FileUtils.deleteDirectory(folder.toFile());
	}

	@Test
	public void keepsADocumentInTheAuthoringCacheAndReadsItBack() {
		var text = "{\"id\":\"doc-1\",\"result\":{\"model\":{\"label\":\"é ☃\"}}}";
		assertNull(FlowStudioSupport.authoringDocument(DOCUMENT_KEY));
		assertTrue(FlowStudioSupport.keepAuthoringDocument(DOCUMENT_KEY, text, SLOW));
		FlowAuthoringDiskCache.flush();
		assertTrue("in the folder of the cache", Files.isRegularFile(folder.resolve(DOCUMENT_KEY + ".arc")));
		assertEquals(text, FlowStudioSupport.authoringDocument(DOCUMENT_KEY));
	}

	@Test
	public void neverKeepsADocumentComputedFastNorOneShowingASecret() {
		FlowStudioSupport.keepAuthoringDocument(DOCUMENT_KEY, "{\"result\":{}}", TimeUnit.MILLISECONDS.toNanos(1));
		FlowAuthoringDiskCache.secrets = () -> List.of("s3cr3t-value");
		var other = "cd".repeat(32);
		FlowStudioSupport.keepAuthoringDocument(other, "{\"result\":{\"text\":\"s3cr3t-value\"}}", SLOW);
		FlowAuthoringDiskCache.flush();
		assertNull(FlowStudioSupport.authoringDocument(DOCUMENT_KEY));
		assertNull(FlowStudioSupport.authoringDocument(other));
	}

	@Test
	public void keepsNothingWhenTheCacheIsOff() {
		FlowAuthoringDiskCache.use(null);
		assertFalse("no engine: the cache is off", FlowStudioSupport.keepAuthoringDocument(DOCUMENT_KEY, "{}", SLOW));
		assertNull(FlowStudioSupport.authoringDocument(DOCUMENT_KEY));
		assertNull("not a key", FlowStudioSupport.authoringDocument(null));
	}
}
