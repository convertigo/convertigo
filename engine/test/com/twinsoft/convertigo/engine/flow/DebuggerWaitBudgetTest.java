package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;
import com.twinsoft.convertigo.engine.debug.DebugRequestableFixture;
import com.twinsoft.convertigo.beans.core.TransactionTimeoutException;

public class DebuggerWaitBudgetTest {
    @Test public void activeSiblingConsumesBudgetThenOnlyThePauseFreezesTheRemainder() throws Exception {
        var paused = new CountDownLatch(1); var resume = new CountDownLatch(1); var siblingDone = new CountDownLatch(1);
        var remaining = new AtomicLong(); var published = new AtomicInteger();
        try (var release = Executors.newSingleThreadExecutor(); var fixture = new DebugRequestableFixture(false, 1, guard -> {
            try (var lanes = ParallelLanes.openDebuggable(2, (domain, lane) -> {
                Object value = domain.next(); if (value == null) return;
                if (((Number)value).intValue() == 0) {
                    try (var pause = domain.debuggerPause()) { paused.countDown(); resume.await(); }
                } else { assertTrue(paused.await(1,TimeUnit.SECONDS)); Thread.sleep(300); siblingDone.countDown(); }
                domain.complete(value);
            }, guard)) {
                assertNotNull(lanes); assertEquals(2,lanes.count()); lanes.submit(0,0); lanes.submit(1,1); lanes.finish();
                release.submit(() -> { siblingDone.await(); Thread.sleep(1200); resume.countDown(); return null; });
                while (published.get() < 2) {
                    Object outcome=lanes.poll(50);
                    if (outcome != null) { if (((Number)outcome).intValue()==1) remaining.set(guard.deadlineNanos()-System.nanoTime()); published.incrementAndGet(); }
                }
                assertTrue(guard.isRunning());
            } finally { resume.countDown(); }
        })) {
            fixture.run(); assertNull(fixture.failure.get()); assertEquals(2,published.get());
            long ms=TimeUnit.NANOSECONDS.toMillis(remaining.get());
            assertTrue("B's 300ms must be billed, remainder was " + ms, ms>350 && ms<850);
        }
    }
    @Test public void siblingOverrunningTheCommonBudgetDoesNotReceiveFreeTimeFromThePause() throws Exception {
        var paused=new CountDownLatch(1); var ranAfterPause=new AtomicBoolean(); var exited=new CountDownLatch(1);
        try (var fixture=new DebugRequestableFixture(false,1,guard -> {
            try (var lanes=ParallelLanes.openDebuggable(2,(domain,lane) -> {
                Object task=domain.next(); if(task==null)return;
                if(((Number)task).intValue()==0) {
                    try(var pause=domain.debuggerPause()) { paused.countDown(); new CountDownLatch(1).await(); }
                    ranAfterPause.set(true);
                } else { assertTrue(paused.await(1,TimeUnit.SECONDS)); Thread.sleep(2000); domain.complete(task); }
            },guard)) {
                lanes.submit(0,0);lanes.submit(1,1);lanes.finish();
                try { while(guard.isRunning())lanes.poll(50); }
                finally { lanes.cancel("owner expired"); }
            } finally { exited.countDown(); }
        })) {
            long start=System.nanoTime(); assertThrows(TransactionTimeoutException.class,fixture::run);
            assertTrue(exited.await(2,TimeUnit.SECONDS)); assertFalse(ranAfterPause.get());
            assertTrue("The shared budget must expire despite A's pause",TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)<2000);
        }
    }
}
