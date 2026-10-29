/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * The walk of the source files of a root for the dependency fingerprint: a file added, removed, renamed or modified, a
 * link pointing elsewhere and a root resolving to another folder change it; a cycle of links ends. A file of the same
 * size and another date is read, and unchanged when its content has the hash kept by the walk before; the same files
 * written again with their dates, as a deployment of the same archive writes them, leave it unchanged. The fingerprint
 * of a root is walked once for its concurrent callers, the value of a few seconds ago served meanwhile unless its stamp
 * changed, and a walk started before the root is forgotten is not kept.
 */
public class FlowSourceFingerprintTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	private Path root;

	@Before
	public void create() throws Exception {
		root = folder.newFolder("project").toPath().toRealPath();
		write(root, "c8oProject.yaml", "convertigo: 8.5.0\n");
		write(root, "_flow/modules/a.js", "var a = 1;\n");
		write(root, "_flow/blocks/proof/b.block.js", "const _meta = {};\n");
	}

	@After
	public void release() {
		pool.shutdownNow();
		FlowEngineBridge.clearCaches();
	}

	private final java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newCachedThreadPool();

	private static Path write(Path base, String relative, String text) throws Exception {
		var file = base.resolve(relative);
		Files.createDirectories(file.getParent());
		Files.writeString(file, text);
		return file;
	}

	private static String walk(Path base) {
		return walked(base, null).text();
	}

	/** A walk following another one, which gives the state of its files. */
	private static FlowSourceFingerprint walked(Path base, FlowSourceFingerprint before) {
		return new FlowSourceFingerprint(base.toFile(), before == null ? null : before.files()).header("project")
				.entry("c8oProject.yaml").entry("_flow/Engine.js").entry("_flow/modules").entry("_flow/blocks");
	}

	/** Another date for a file: the next second, as a file system of coarse dates shows it. */
	private static void touch(Path file) throws Exception {
		Files.setLastModifiedTime(file, FileTime.fromMillis(Files.getLastModifiedTime(file).toMillis() + TimeUnit.SECONDS.toMillis(2)));
	}

	@Test
	public void aWalkOfTheSameFilesGivesTheSameFingerprint() {
		var first = walk(root);
		assertEquals(first, walk(root));
		assertTrue("Named relative to its root", first.contains("f:_flow/modules/a.js:"));
		assertTrue(first.contains("m:_flow/Engine.js"));
		assertEquals("The root is named once", 1, first.split(java.util.regex.Pattern.quote(root.toString()), -1).length - 1);
	}

	@Test
	public void addedRemovedRenamedAndModifiedFilesChangeIt() throws Exception {
		var previous = walk(root);
		var added = write(root, "_flow/modules/c.js", "var c;\n");
		previous = changed(previous, "added");
		Files.delete(added);
		previous = changed(previous, "removed");
		Files.move(root.resolve("_flow/modules/a.js"), root.resolve("_flow/modules/z.js"));
		previous = changed(previous, "renamed");
		write(root, "_flow/modules/z.js", "var a = 22;\n");
		previous = changed(previous, "modified, another size");
		touch(root.resolve("_flow/modules/z.js"));
		previous = changed(previous, "modified, another date");
		write(root, "_flow/Engine.js", "({})");
		changed(previous, "a missing file written");
	}

	private String changed(String previous, String what) {
		var next = walk(root);
		assertNotEquals(what, previous, next);
		return next;
	}

	@Test
	public void aRootBehindALinkResolvingToAnotherFolderChangesIt() throws Exception {
		var copy = folder.getRoot().toPath().resolve("copy");
		org.apache.commons.io.FileUtils.copyDirectory(root.toFile(), copy.toFile(), true);
		var link = Files.createSymbolicLink(folder.getRoot().toPath().resolve("current"), root);
		var first = walk(link);
		assertTrue(first.contains(root.toString()));
		Files.delete(link);
		Files.createSymbolicLink(link, copy);
		var second = walk(link);
		assertNotEquals("The same files elsewhere", first, second);
		assertEquals(first.replace(root.toString(), copy.toRealPath().toString()), second);
	}

	@Test
	public void aLinkPointingElsewhereChangesIt() throws Exception {
		var one = write(folder.getRoot().toPath(), "shared/one.js", "var s = 1;\n");
		var two = write(folder.getRoot().toPath(), "shared/two.js", "var s = 2;\n");
		Files.setLastModifiedTime(two, Files.getLastModifiedTime(one));
		var link = Files.createSymbolicLink(root.resolve("_flow/modules/shared.js"), one);
		var modules = root.resolve("_flow/modules");
		var date = Files.getLastModifiedTime(modules);
		var first = walk(root);
		assertTrue(first, first.contains("l:_flow/modules/shared.js->" + one));
		Files.delete(link);
		Files.createSymbolicLink(link, two);
		Files.setLastModifiedTime(modules, date);
		var second = walk(root);
		assertNotEquals("Another file of the same date and size", first, second);
		assertEquals(first.replace(one.toString(), two.toString()), second);
	}

	@Test
	public void aCycleOfLinksEnds() throws Exception {
		Files.createSymbolicLink(root.resolve("_flow/blocks/proof/loop"), root.resolve("_flow/blocks"));
		var walked = walk(root);
		assertTrue(walked, walked.contains("cycle:_flow/blocks/proof/loop"));
		assertEquals(walked, walk(root));
	}

	@Test
	public void aFileRewrittenWithTheSameContentIsReadOnceThenUnchanged() throws Exception {
		var file = root.resolve("_flow/modules/a.js");
		var first = walked(root, null);
		assertEquals("Nothing is read of files of the same date and size", 0, first.reads());
		touch(file);
		var second = walked(root, first);
		assertEquals("Its hash is not known yet: read", 1, second.reads());
		assertNotEquals("and a change", first.text(), second.text());
		touch(file);
		var third = walked(root, second);
		assertEquals(1, third.reads());
		assertEquals("The content of the walk before: unchanged", second.text(), third.text());
		var fourth = walked(root, third);
		assertEquals("Its new date kept: not read again", 0, fourth.reads());
		assertEquals(second.text(), fourth.text());
	}

	@Test
	public void aFileOfTheSameSizeWithAnotherContentIsChanged() throws Exception {
		var file = root.resolve("_flow/modules/a.js");
		var first = walked(root, null);
		touch(file);
		var second = walked(root, first);
		Files.writeString(file, "var a = 2;\n");
		touch(file);
		var third = walked(root, second);
		assertEquals(1, third.reads());
		assertNotEquals(second.text(), third.text());
	}

	@Test
	public void aFileOfAnotherSizeIsChangedWithoutBeingRead() throws Exception {
		var file = root.resolve("_flow/modules/a.js");
		var first = walked(root, null);
		touch(file);
		var second = walked(root, first);
		Files.writeString(file, "var a = 333;\n");
		var third = walked(root, second);
		assertEquals(0, third.reads());
		assertNotEquals(second.text(), third.text());
	}

	@Test
	public void theSameFilesWrittenAgainWithTheirDatesLeaveItUnchanged() throws Exception {
		// as the deployment of the same archive: the folders created again, the files written with the dates of the archive
		var sources = root.resolve("_flow");
		var saved = new java.util.TreeMap<String, Object[]>();
		try (var files = Files.walk(sources)) {
			for (var file : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
				saved.put(sources.relativize(file).toString(), new Object[] { Files.readAllBytes(file), Files.getLastModifiedTime(file) });
			}
		}
		var first = walked(root, null);
		org.apache.commons.io.FileUtils.deleteDirectory(sources.toFile());
		Thread.sleep(20);
		for (var entry : saved.entrySet()) {
			var file = sources.resolve(entry.getKey());
			Files.createDirectories(file.getParent());
			Files.write(file, (byte[]) entry.getValue()[0]);
			Files.setLastModifiedTime(file, (FileTime) entry.getValue()[1]);
		}
		var second = walked(root, first);
		assertEquals(first.text(), second.text());
		assertEquals(0, second.reads());
		assertEquals("Without the walk before as well", first.text(), walk(root));
	}

	/** The cached fingerprint of the root, walked as the label tells. */
	private String root(Path base, String label, java.util.function.Consumer<FlowSourceFingerprint> before) {
		return FlowEngineBridge.cachedRootFingerprint("project", base.toFile(), base.resolve("_flow").toFile(), base.toFile(), walk -> {
			before.accept(walk);
			walk.header(label).entry("_flow/modules");
		});
	}

	private static java.util.function.Consumer<FlowSourceFingerprint> held(java.util.concurrent.CountDownLatch started,
			java.util.concurrent.CountDownLatch release, java.util.concurrent.atomic.AtomicInteger walks) {
		return walk -> {
			walks.incrementAndGet();
			started.countDown();
			try {
				assertTrue("The walk stayed held", release.await(20, TimeUnit.SECONDS));
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
		};
	}

	private static java.util.function.Consumer<FlowSourceFingerprint> never() {
		return walk -> {
			throw new AssertionError("Walked again");
		};
	}

	/** The fingerprints of the roots expire, as five seconds after their walk. */
	@SuppressWarnings("unchecked")
	private static void expire() throws Exception {
		var field = FlowEngineBridge.class.getDeclaredField("rootFingerprints");
		field.setAccessible(true);
		var fingerprints = (java.util.Map<Object, Object>) field.get(null);
		var type = Class.forName(FlowEngineBridge.class.getName() + "$RootFingerprint");
		var constructor = type.getDeclaredConstructors()[0];
		constructor.setAccessible(true);
		for (var entry : fingerprints.entrySet()) {
			var value = entry.getValue();
			var components = type.getRecordComponents();
			var arguments = new Object[components.length];
			for (int i = 0; i < components.length; i++) {
				var accessor = components[i].getAccessor();
				accessor.setAccessible(true);
				arguments[i] = components[i].getName().equals("computedAt") ? 0L : accessor.invoke(value);
			}
			entry.setValue(constructor.newInstance(arguments));
		}
	}

	@Test
	public void theCallersOfARootWaitForItsSingleWalkWhileAnotherRootIsWalked() throws Exception {
		var other = folder.newFolder("other").toPath();
		var started = new java.util.concurrent.CountDownLatch(1);
		var release = new java.util.concurrent.CountDownLatch(1);
		var walks = new java.util.concurrent.atomic.AtomicInteger();
		var arrived = new java.util.concurrent.CountDownLatch(6);
		var callers = new java.util.ArrayList<java.util.concurrent.Future<String>>();
		for (int i = 0; i < 6; i++) {
			callers.add(pool.submit(() -> {
				arrived.countDown();
				return root(root, "walked", held(started, release, walks));
			}));
		}
		assertTrue(started.await(10, TimeUnit.SECONDS));
		assertTrue(arrived.await(10, TimeUnit.SECONDS));
		Thread.sleep(100);
		try {
			assertTrue(pool.submit(() -> root(other, "other", walk -> { })).get(5, TimeUnit.SECONDS).startsWith("other:"));
		} finally {
			release.countDown();
		}
		var first = callers.get(0).get(20, TimeUnit.SECONDS);
		for (var caller : callers) {
			assertEquals(first, caller.get(20, TimeUnit.SECONDS));
		}
		assertEquals(1, walks.get());
		assertEquals(first, root(root, "again", never()));
	}

	@Test
	public void anExpiredRootIsServedWhileOneCallerWalksItButAChangedStampWaits() throws Exception {
		var first = root(root, "first", walk -> { });
		expire();
		var started = new java.util.concurrent.CountDownLatch(1);
		var release = new java.util.concurrent.CountDownLatch(1);
		var walks = new java.util.concurrent.atomic.AtomicInteger();
		var walking = pool.submit(() -> root(root, "second", held(started, release, walks)));
		assertTrue(started.await(10, TimeUnit.SECONDS));
		try {
			assertEquals("Its value of a few seconds ago, without waiting", first, pool.submit(() -> root(root, "x", never())).get(5, TimeUnit.SECONDS));
		} finally {
			release.countDown();
		}
		var second = walking.get(20, TimeUnit.SECONDS);
		assertTrue(second.startsWith("second:"));
		// the descriptor of the project changed: its stamp tells, the callers wait for the walk
		Files.writeString(root.resolve("c8oProject.yaml"), "convertigo: 8.5.1\n");
		var heldAgain = new java.util.concurrent.CountDownLatch(1);
		var releaseAgain = new java.util.concurrent.CountDownLatch(1);
		var third = pool.submit(() -> root(root, "third", held(heldAgain, releaseAgain, walks)));
		assertTrue(heldAgain.await(10, TimeUnit.SECONDS));
		var waiting = pool.submit(() -> root(root, "y", never()));
		try {
			Thread.sleep(200);
			assertFalse("A changed root is not served from before its change", waiting.isDone());
		} finally {
			releaseAgain.countDown();
		}
		assertEquals(third.get(20, TimeUnit.SECONDS), waiting.get(20, TimeUnit.SECONDS));
	}

	@Test
	public void aWalkStartedBeforeTheRootIsForgottenIsNotKept() throws Exception {
		var started = new java.util.concurrent.CountDownLatch(1);
		var release = new java.util.concurrent.CountDownLatch(1);
		var walks = new java.util.concurrent.atomic.AtomicInteger();
		var before = pool.submit(() -> root(root, "before", held(started, release, walks)));
		assertTrue(started.await(10, TimeUnit.SECONDS));
		String after;
		try {
			// a change saved in the project: the walk in progress may predate it
			pool.submit(() -> { FlowEngineBridge.invalidateProjectDataCaches(root.toString()); return null; }).get(5, TimeUnit.SECONDS);
			after = pool.submit(() -> root(root, "after", walk -> { })).get(5, TimeUnit.SECONDS);
			assertTrue(after.startsWith("after:"));
		} finally {
			release.countDown();
		}
		assertTrue(before.get(20, TimeUnit.SECONDS).startsWith("before:"));
		assertEquals(after, root(root, "again", never()));
	}

	@Test
	public void aFailedWalkIsSeenByItsCallersAndNotKept() throws Exception {
		var started = new java.util.concurrent.CountDownLatch(1);
		var release = new java.util.concurrent.CountDownLatch(1);
		var walks = new java.util.concurrent.atomic.AtomicInteger();
		var failure = new IllegalStateException("unreadable");
		var hold = held(started, release, walks);
		var owner = pool.submit(() -> root(root, "x", walk -> { hold.accept(walk); throw failure; }));
		assertTrue(started.await(10, TimeUnit.SECONDS));
		var arrived = new java.util.concurrent.CountDownLatch(1);
		var waiter = pool.submit(() -> { arrived.countDown(); return root(root, "y", never()); });
		assertTrue(arrived.await(10, TimeUnit.SECONDS));
		Thread.sleep(100);
		release.countDown();
		for (var caller : java.util.List.of(owner, waiter)) {
			try {
				caller.get(20, TimeUnit.SECONDS);
				fail();
			} catch (java.util.concurrent.ExecutionException e) {
				assertSame(failure, e.getCause());
			}
		}
		assertTrue(root(root, "walked", walk -> { }).startsWith("walked:"));
	}
}
