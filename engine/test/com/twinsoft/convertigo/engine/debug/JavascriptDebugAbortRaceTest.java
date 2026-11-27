package com.twinsoft.convertigo.engine.debug;

import static org.junit.Assert.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;

public class JavascriptDebugAbortRaceTest {
    @org.junit.Rule public final FrozenInspectionClock inspectionClock = new FrozenInspectionClock();
    @org.junit.Before public void editingEnabledFixture() throws Exception {
        com.twinsoft.convertigo.engine.EnginePropertiesManager.initProperties();
        com.twinsoft.convertigo.engine.EnginePropertiesManager.setProperty(com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName.WEB_STUDIO,"enabled");
    }

    private void cancellation(boolean transaction, boolean stopRace) throws Exception {
        var sessionReady=new CompletableFuture<JavascriptDebugSession>(); var finished=new CountDownLatch(1);
        var io=new AtomicInteger();var publication=new AtomicInteger();var opens=new AtomicInteger();var closes=new AtomicInteger();var interrupted=new AtomicBoolean();
        try(var pool=Executors.newFixedThreadPool(2); var fixture=new DebugRequestableFixture(transaction,3,guard -> {
            try(var session=new JavascriptDebugSession(1000);var wake=guard.onCancellation(session::cancellationChanged);var cx=Context.enter()) {
                Object root=session.joinParticipantWorker();session.identifyRoot(root,"root",()->!guard.isRunning());
                try {
                    var parent=session.parent(root);session.breakpoint("fake:child",1,true,false);
                    var child=pool.submit(()->{
                        try(var childCx=Context.enter()) {
                            Object worker=session.joinChild(parent,()->!guard.isRunning(),()->{
                                opens.incrementAndGet();var real=guard.pauseDebugger();return ()->{closes.incrementAndGet();if(real!=null)real.close();};
                            });
                            var scope=childCx.initStandardObjects();var inspect=(Function)childCx.evaluateString(scope,"(function(){return '{}';})","inspect",1,null);
                            var plugin=session.participant("fake",worker,inspect);
                            try {
                                plugin.enter("child","{\"url\":\"fake:child\",\"line\":1}");
                                plugin.at("child","before"); io.incrementAndGet();plugin.at("child","after");publication.incrementAndGet();
                                return false;
                            } catch(CancellationException expected) { interrupted.set(Thread.currentThread().isInterrupted());return true; }
                            finally { plugin.leave("child");session.finishParticipantWorker(); }
                        }
                    });
                    sessionReady.complete(session);assertTrue(child.get(2,TimeUnit.SECONDS));
                    assertFalse(session.state().getBoolean("stopped"));
                } finally { session.finishParticipantWorker(); }
            } finally { finished.countDown(); }
        })) {
            var caller=pool.submit(()->{fixture.run();return null;});var session=sessionReady.get(2,TimeUnit.SECONDS);
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
            while(!session.state().getBoolean("stopped")&&System.nanoTime()<end)Thread.sleep(2);
            assertTrue(session.state().getBoolean("stopped"));
            long start=System.nanoTime();fixture.root.abort(); // publishes only the business flag, no interrupt
            if(stopRace)session.close();
            assertTrue(finished.await(1,TimeUnit.SECONDS));caller.get(2,TimeUnit.SECONDS);
            assertTrue("Cancellation wake should not wait for the 1s pause expiry",TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)<500);
            assertNull(fixture.failure.get());assertFalse(interrupted.get());assertEquals(0,io.get());assertEquals(0,publication.get());
            assertEquals(1,opens.get());assertEquals(1,closes.get());
        }
    }
    @Test public void sequenceAbortWithoutInterruptReleasesTheChild()throws Exception{cancellation(false,false);}
    @Test public void transactionAbortWithoutInterruptReleasesTheChild()throws Exception{cancellation(true,false);}
    @Test public void publishedSequenceCancellationWinsOverStop()throws Exception{cancellation(false,true);}
    @Test public void publishedTransactionCancellationWinsOverStop()throws Exception{cancellation(true,true);}

    @Test public void expiryAndStopCloseEachPauseLeaseExactlyOnce()throws Exception {
        for(boolean stop:new boolean[]{false,true})try(var session=new JavascriptDebugSession(60);var pool=Executors.newSingleThreadExecutor()){
            var opened=new AtomicInteger();var closed=new AtomicInteger();
            try(var cx=Context.enter()){
                Object root=session.joinParticipantWorker();var parent=session.parent(root);
                session.breakpoint("fake:x",1,true,false);
                var child=pool.submit(()->{try(var cc=Context.enter()){
                    Object worker=session.joinChild(parent,()->false,()->{opened.incrementAndGet();return ()->closed.incrementAndGet();});
                    var scope=cc.initStandardObjects();var f=(Function)cc.evaluateString(scope,"(function(){return '{}';})","inspect",1,null);
                    var p=session.participant("fake",worker,f);try{p.enter("f","{\"url\":\"fake:x\",\"line\":1}");p.at("f","before");return 44;}finally{p.leave("f");session.finishParticipantWorker();}
                }});
                long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);while(!session.state().getBoolean("stopped")&&!child.isDone()&&System.nanoTime()<end)Thread.sleep(1);
                if(stop)session.close();assertEquals(44,(int)child.get(2,TimeUnit.SECONDS));assertEquals(1,opened.get());assertEquals(1,closed.get());session.finishParticipantWorker();
            }
        }
    }
    @Test public void publishedCancellationWinsWhileExpiryAndStopReleaseTheSamePause() throws Exception {
        try(var session=new JavascriptDebugSession(30);var pool=Executors.newSingleThreadExecutor();var cx=Context.enter()) {
            var cancelled=new AtomicBoolean();var closing=new CountDownLatch(1);var release=new CountDownLatch(1);var closed=new AtomicInteger();var business=new AtomicInteger();
            Object root=session.joinParticipantWorker();var parent=session.parent(root);session.breakpoint("fake:race",1,true,false);
            var child=pool.submit(()->{try(var cc=Context.enter()){
                Object worker=session.joinChild(parent,cancelled::get,()->()->{closed.incrementAndGet();closing.countDown();try{release.await();}catch(InterruptedException e){throw new AssertionError(e);}});
                var scope=cc.initStandardObjects();var f=(Function)cc.evaluateString(scope,"(function(){return '{}';})","inspect",1,null);var p=session.participant("fake",worker,f);
                try{p.enter("f","{\"url\":\"fake:race\",\"line\":1}");p.at("f","before");business.incrementAndGet();return false;}
                catch(CancellationException expected){return true;}
                finally{p.leave("f");session.finishParticipantWorker();}
            }});
            try { assertTrue(closing.await(2,TimeUnit.SECONDS));cancelled.set(true);session.close();release.countDown();assertTrue(child.get(2,TimeUnit.SECONDS));assertEquals(0,business.get());assertEquals(1,closed.get()); }
            finally {release.countDown();session.finishParticipantWorker();}
        }
    }
    @Test public void duplicateCloseAndOverlappingLeasesCreditOnlyTheirUnion() throws Exception {
        try(var fixture=new DebugRequestableFixture(false,1,guard->{
            var first=guard.pauseDebugger();var second=guard.pauseDebugger();
            Thread.sleep(600);first.close();first.close();Thread.sleep(600);second.close();second.close();
            long remaining=TimeUnit.NANOSECONDS.toMillis(guard.deadlineNanos()-System.nanoTime());
            assertTrue("Union, not sum, of leases: "+remaining,remaining>700&&remaining<=1000);
        })) {fixture.run();assertNull(fixture.failure.get());}
    }

    @Test public void oldInvocationGuardCannotCreditTheNextRunOfTheSameRequestable() throws Exception {
        var previous=new AtomicReference<com.twinsoft.convertigo.beans.core.RequestableObject.InvocationGuard>();
        try(var fixture=new DebugRequestableFixture(false,2,guard->{
            var old=previous.getAndSet(guard);
            if(old!=null){assertFalse(old.isRunning());long deadline=guard.deadlineNanos();try(var ignored=old.pauseDebugger()){Thread.sleep(30);}assertEquals(deadline,guard.deadlineNanos());}
        })) {fixture.run();assertNull(fixture.failure.get());fixture.run();assertNull(fixture.failure.get());}
    }

}
