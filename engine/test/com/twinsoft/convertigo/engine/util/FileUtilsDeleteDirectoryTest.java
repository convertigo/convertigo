/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.rules.TemporaryFolder;

/** A small tree is removed in Java, a large one still by the external command (#1253). */
public class FileUtilsDeleteDirectoryTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();

	@Test public void emptyFolderIsRemovedWithoutStartingAProcess() throws Throwable {
		var dir = folder.newFolder("empty");
		assertEquals(List.of(), processStarts(() -> FileUtils.deleteDirectory(dir)));
		assertFalse(dir.exists());
	}

	@Test public void nestedFilesAreRemovedWithoutFollowingTheLinks() throws Throwable {
		var dir = folder.newFolder("nested");
		var outside = folder.newFolder("outside");
		Files.writeString(new File(outside, "kept.txt").toPath(), "kept");
		Files.createDirectories(new File(dir, "1/2/3").toPath());
		Files.writeString(new File(dir, "1/2/3/deep.txt").toPath(), "deep");
		Files.writeString(new File(dir, "1/file.bin").toPath(), "file");
		Files.createSymbolicLink(new File(dir, "1/link").toPath(), outside.toPath());
		assertEquals(List.of(), processStarts(() -> FileUtils.deleteDirectory(dir)));
		assertFalse(dir.exists());
		assertTrue(new File(outside, "kept.txt").exists());
	}

	@Test public void readOnlyEntriesAreRemoved() throws Throwable {
		var dir = folder.newFolder("read-only");
		var file = new File(dir, "file.txt");
		Files.writeString(file.toPath(), "read-only");
		var sub = new File(dir, "sub");
		Files.createDirectories(new File(sub, "inner").toPath());
		Files.writeString(new File(sub, "inner/file.txt").toPath(), "in a read-only folder");
		assertTrue(file.setWritable(false, false));
		assertTrue(new File(sub, "inner").setWritable(false, false));
		assertTrue(sub.setWritable(false, false));
		assertTrue(dir.setWritable(false, false));
		assertEquals(List.of(), processStarts(() -> FileUtils.deleteDirectory(dir)));
		assertFalse(dir.exists());
	}

	@Test public void largeTreeIsStillRemovedByTheExternalCommand() throws Throwable {
		assumeTrue(FlightRecorder.getFlightRecorder().getEventTypes().stream().anyMatch(type -> type.getName().equals("jdk.ProcessStart")));
		var dir = folder.newFolder("large");
		for (int i = 0; i < 40; i++) {
			var sub = new File(dir, "sub" + i);
			sub.mkdir();
			for (int j = 0; j < 30; j++) {
				Files.writeString(new File(sub, "file" + j).toPath(), "content");
			}
		}
		var starts = processStarts(() -> FileUtils.deleteDirectory(dir));
		assertFalse(dir.exists());
		assertEquals(1, starts.size());
		assertTrue(starts.get(0).getString("command").contains(dir.getCanonicalPath()));
	}

	@Test public void largerTreeIsLeftUntouched() throws Exception {
		var dir = folder.newFolder("bounded");
		Files.createDirectories(new File(dir, "a/b").toPath());
		Files.writeString(new File(dir, "a/b/file.txt").toPath(), "file");
		Files.writeString(new File(dir, "a/other.txt").toPath(), "other");
		// 5 entries with the folder itself
		assertFalse(FileUtils.deleteSmallDirectory(dir, 4));
		assertTrue(new File(dir, "a/b/file.txt").exists());
		assertTrue(new File(dir, "a/other.txt").exists());
		assertTrue(FileUtils.deleteSmallDirectory(dir, 5));
		assertFalse(dir.exists());
	}

	/** The processes started while the action runs, as recorded by JFR: the jdk.ProcessStart events. */
	public static List<RecordedEvent> processStarts(ThrowingRunnable action) throws Throwable {
		var dump = Files.createTempFile("process-starts", ".jfr");
		try (var recording = new Recording()) {
			recording.enable("jdk.ProcessStart");
			recording.start();
			action.run();
			recording.stop();
			recording.dump(dump);
			return RecordingFile.readAllEvents(dump);
		} finally {
			Files.deleteIfExists(dump);
		}
	}
}
