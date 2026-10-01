package com.twinsoft.convertigo.beans.flow;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.EngineException;

public class FlowSourceChangesTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();

	private FlowEngine owner(File directory) {
		var project = new Project() { @Override public File getDirFile() { return directory; } };
		return new FlowEngine() { @Override public Project getProject() { return project; } };
	}

	@Test public void removalIsAnExplicitAbsentStateUntilSaveAndDiscardRestoresSavedFile() throws Exception {
		var root = folder.newFolder();
		var path = root.toPath().resolve("value.json");
		Files.writeString(path, "saved");
		var owner = owner(root);
		owner.applySourceChanges(Map.of(), List.of(path.toString()));
		assertFalse(owner.hasSource(path.toString()));
		assertThrows(EngineException.class, () -> owner.getSource(path.toString()));
		assertTrue(owner.isSourceDirty(path.toString()));
		assertTrue(FlowEngine.hasSourceDrafts(root));
		assertEquals(Set.of(path.toFile().getCanonicalPath()), owner.getSourceRemovals());
		assertTrue(owner.getSourceDrafts().isEmpty());
		assertEquals("saved", Files.readString(path));
		assertTrue(owner.discardSource(path.toString()));
		assertEquals("saved", owner.getSource(path.toString()));
		assertFalse(owner.isSourceDirty(path.toString()));
		assertFalse(FlowEngine.hasSourceDrafts(root));
	}

	@Test public void removalOfDraftOnlyCreationCancelsItWithoutAnEmptyFile() throws Exception {
		var root = folder.newFolder();
		var path = root.toPath().resolve("new/empty.json");
		var owner = owner(root);
		owner.setSource(path.toString(), "");
		owner.applySourceChanges(Map.of(), List.of(path.toString()));
		assertFalse(owner.hasSource(path.toString()));
		assertFalse(owner.isSourceDirty(path.toString()));
		assertTrue(owner.getSourceRemovals().isEmpty());
		owner.saveSources();
		assertFalse(Files.exists(path.getParent()));
	}

	@Test public void explicitRemovalDoesNotChangeNullTextSemantics() throws Exception {
		var root = folder.newFolder();
		var path = root.toPath().resolve("value.json");
		Files.writeString(path, "saved");
		var owner = owner(root);
		owner.applySourceChanges(Map.of(), List.of(path.toString()));
		owner.setSource(path.toString(), null);
		assertTrue(owner.hasSource(path.toString()));
		assertEquals("", owner.getSource(path.toString()));
		assertTrue(owner.getSourceRemovals().isEmpty());
		owner.setSource(path.toString(), "saved");
		assertFalse(owner.isSourceDirty(path.toString()));
	}

	@Test public void writeAndRemovalPlanMovesEffectiveTextOnlyAtSave() throws Exception {
		var root = folder.newFolder();
		var before = root.toPath().resolve("_flow/source/a.block.js");
		var after = root.toPath().resolve("_flow/target/a.block.js");
		Files.createDirectories(before.getParent());
		Files.writeString(before, "saved");
		var owner = owner(root);
		owner.setSource(before.toString(), "draft");
		owner.applySourceChanges(Map.of(after.toString(), owner.getSource(before.toString())), List.of(before.toString()));
		assertFalse(owner.hasSource(before.toString()));
		assertEquals("draft", owner.getSource(after.toString()));
		assertEquals("saved", Files.readString(before));
		assertFalse(Files.exists(after.getParent()));
		owner.saveSources();
		assertFalse(Files.exists(before.getParent()));
		assertEquals("draft", Files.readString(after));
		assertFalse(FlowEngine.hasSourceDrafts(root));
		assertTrue(owner.getSourceDrafts().isEmpty());
		assertTrue(owner.getSourceRemovals().isEmpty());
	}

	@Test public void invalidPlanDoesNotPartiallyChangeWritesOrRemovals() throws Exception {
		var root = folder.newFolder();
		var path = root.toPath().resolve("saved.json");
		Files.writeString(path, "saved");
		var owner = owner(root);
		owner.setSource(path.toString(), "existing draft");
		var writes = Map.of(root.toPath().resolve("new.json").toString(), "new");
		assertThrows(EngineException.class, () -> owner.applySourceChanges(writes, List.of("../outside.json")));
		assertEquals("existing draft", owner.getSource(path.toString()));
		assertEquals(1, owner.getSourceDrafts().size());
		assertTrue(owner.getSourceRemovals().isEmpty());
		assertThrows(EngineException.class, () -> owner.applySourceChanges(writes, List.of(root.toString())));
		assertThrows(EngineException.class, () -> owner.applySourceChanges(Map.of(path.toString(), "new"), List.of(path.toString())));
		assertThrows(EngineException.class, () -> owner.applySourceChanges(Map.of(), List.of(path.toString(), root + "/./saved.json")));
		assertEquals("existing draft", owner.getSource(path.toString()));
		Flow.projectUnloaded(owner.getProject());
	}

	@Test public void projectReloadDropsRemovalAndCreationWithoutChangingDisk() throws Exception {
		var root = folder.newFolder();
		var before = root.toPath().resolve("saved.json");
		var after = root.toPath().resolve("moved.json");
		Files.writeString(before, "saved");
		var owner = owner(root);
		owner.applySourceChanges(Map.of(after.toString(), "draft"), List.of(before.toString()));
		Flow.projectUnloaded(owner.getProject());
		assertEquals("saved", owner(root).getSource(before.toString()));
		assertFalse(owner(root).hasSource(after.toString()));
	}

	@Test public void failedPublicationRollsBackEarlierWritesAndDoesNotLoseOriginalBytes() throws Exception {
		var root = folder.newFolder().toPath();
		var first = root.resolve("first.json");
		var blocked = root.resolve("blocked.json");
		Files.write(first, new byte[] { (byte) 0xef, (byte) 0xbb, (byte) 0xbf, 65 });
		Files.writeString(blocked, "not a directory");
		var writes = new LinkedHashMap<String, String>();
		writes.put(first.toString(), "changed");
		writes.put(blocked.resolve("child.json").toString(), "cannot publish");
		assertThrows(java.io.IOException.class, () -> FlowSourcePublisher.publish(writes, List.of(), root.toFile()));
		assertArrayEquals(new byte[] { (byte) 0xef, (byte) 0xbb, (byte) 0xbf, 65 }, Files.readAllBytes(first));
		assertEquals("not a directory", Files.readString(blocked));
		try (var children = Files.list(root)) {
			assertFalse(children.anyMatch(path -> path.getFileName().toString().startsWith(".flow-save-")));
		}
	}

	@Test public void saveRevalidatesSymlinkConfinementAndRetainsTheDraftOnFailure() throws Exception {
		var root = folder.newFolder();
		var outside = folder.newFolder();
		var directory = root.toPath().resolve("new");
		var path = directory.resolve("value.json");
		var owner = owner(root);
		owner.setSource(path.toString(), "draft");
		var originalKey = path.toFile().getCanonicalPath();
		Files.createSymbolicLink(directory, outside.toPath());
		assertThrows(EngineException.class, owner::saveSources);
		assertFalse(Files.exists(outside.toPath().resolve("value.json")));
		assertEquals("draft", owner.getSourceDrafts().get(originalKey));
		assertTrue(FlowEngine.hasSourceDrafts(root));
		Flow.projectUnloaded(owner.getProject());
	}

	@Test public void failedSaveRetainsMoveDraftsRollsBackPublishedTargetAndCanBeRetried() throws Exception {
		var root = folder.newFolder();
		var before = root.toPath().resolve("original/value.json");
		var after = root.toPath().resolve("moved/nested/value.json");
		Files.createDirectories(before.getParent());
		Files.writeString(before, "saved");
		var owner = owner(root);
		owner.applySourceChanges(Map.of(after.toString(), "draft"), List.of(before.toString()));
		var permission = Files.getPosixFilePermissions(before.getParent());
		try {
			Files.setPosixFilePermissions(before.getParent(), java.nio.file.attribute.PosixFilePermissions.fromString("r-xr-xr-x"));
			org.junit.Assume.assumeFalse("Cannot exercise an I/O refusal with a privileged user", Files.isWritable(before.getParent()));
			assertThrows(EngineException.class, owner::saveSources);
			assertEquals("saved", Files.readString(before));
			assertFalse(Files.exists(after.getParent().getParent()));
			assertFalse(owner.hasSource(before.toString()));
			assertEquals("draft", owner.getSource(after.toString()));
			assertTrue(FlowEngine.hasSourceDrafts(root));
		} finally {
			Files.setPosixFilePermissions(before.getParent(), permission);
		}
		owner.saveSources();
		assertFalse(Files.exists(before));
		assertEquals("draft", Files.readString(after));
		assertFalse(FlowEngine.hasSourceDrafts(root));
	}

	@Test public void oneProjectPlanDoesNotBlockAnotherProjectButItsClonesShareTheLock() throws Exception {
		var firstRoot = folder.newFolder();
		var secondRoot = folder.newFolder();
		var entered = new java.util.concurrent.CountDownLatch(1);
		var release = new java.util.concurrent.CountDownLatch(1);
		var calls = new java.util.concurrent.atomic.AtomicInteger();
		var project = new Project() {
			@Override public File getDirFile() {
				// First resolution selects the lock. The second happens during plan validation.
				if (calls.incrementAndGet() == 2) {
					entered.countDown();
					try { if (!release.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("Plan not released"); }
					catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
				}
				return firstRoot;
			}
		};
		var first = new FlowEngine() { @Override public Project getProject() { return project; } };
		var clone = owner(firstRoot);
		var second = owner(secondRoot);
		var pool = java.util.concurrent.Executors.newFixedThreadPool(3);
		try {
			var firstPlan = pool.submit(() -> { first.setSource(new File(firstRoot, "value.json").getPath(), "first"); return true; });
			assertTrue(entered.await(2, java.util.concurrent.TimeUnit.SECONDS));
			var otherPlan = pool.submit(() -> { second.setSource(new File(secondRoot, "value.json").getPath(), "second"); return true; });
			assertTrue(otherPlan.get(1, java.util.concurrent.TimeUnit.SECONDS));
			var cloneRead = pool.submit(clone::getSourceChanges);
			assertThrows(java.util.concurrent.TimeoutException.class, () -> cloneRead.get(50, java.util.concurrent.TimeUnit.MILLISECONDS));
			release.countDown();
			assertTrue(firstPlan.get(1, java.util.concurrent.TimeUnit.SECONDS));
			assertEquals("first", cloneRead.get(1, java.util.concurrent.TimeUnit.SECONDS).writes().get(new File(firstRoot, "value.json").getCanonicalPath()));
		} finally {
			release.countDown();
			pool.shutdownNow();
			pool.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS);
			Flow.projectUnloaded(project);
			Flow.projectUnloaded(second.getProject());
		}
	}

	@Test public void publicationAndRollbackPreservePermissionsAndNewSourcesUseNormalPermissions() throws Exception {
		var root = folder.newFolder();
		var existing = root.toPath().resolve("existing.json");
		var created = root.toPath().resolve("created.json");
		var control = root.toPath().resolve("control.json");
		Files.writeString(existing, "saved");
		Files.writeString(control, "normal creation");
		var specific = java.nio.file.attribute.PosixFilePermissions.fromString("rw-r-----");
		Files.setPosixFilePermissions(existing, specific);
		var owner = owner(root);
		owner.setSources(Map.of(existing.toString(), "updated", created.toString(), "created"));
		owner.saveSources();
		assertEquals(specific, Files.getPosixFilePermissions(existing));
		assertEquals(Files.getPosixFilePermissions(control), Files.getPosixFilePermissions(created));
		// A later removal can fail after an earlier one succeeded. Restoration must
		// preserve the removed file's mode as well as its bytes.
		var blocked = root.toPath().resolve("readOnly/value.json");
		Files.createDirectories(blocked.getParent());
		Files.writeString(blocked, "saved");
		var directoryPermissions = Files.getPosixFilePermissions(blocked.getParent());
		try {
			Files.setPosixFilePermissions(blocked.getParent(), java.nio.file.attribute.PosixFilePermissions.fromString("r-xr-xr-x"));
			org.junit.Assume.assumeFalse("Cannot exercise an I/O refusal with a privileged user", Files.isWritable(blocked.getParent()));
			assertThrows(java.io.IOException.class, () -> FlowSourcePublisher.publish(Map.of(), List.of(existing.toString(), blocked.toString()), root));
			assertEquals("updated", Files.readString(existing));
			assertEquals(specific, Files.getPosixFilePermissions(existing));
			assertEquals("saved", Files.readString(blocked));
		} finally {
			Files.setPosixFilePermissions(blocked.getParent(), directoryPermissions);
		}
	}
}
