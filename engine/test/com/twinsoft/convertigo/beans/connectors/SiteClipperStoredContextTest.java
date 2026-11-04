package com.twinsoft.convertigo.beans.connectors;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.BeforeClass;
import org.apache.log4j.Logger;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;

public class SiteClipperStoredContextTest {
    @BeforeClass
    public static void setUpEngine() {
        Engine.logEngine = Logger.getLogger("siteclipper-stored-context-test");
        Engine.theApp = new Engine();
    }

    @Test
    public void restoredContextLoadsItsProject() throws Exception {
        Context context = new Context("session_clip");
        context.storedAccessTime = 1;
        Project project = new Project();
        AtomicInteger calls = new AtomicInteger();
        SiteClipperConnector.Shuttle.restoreStoredProject(context, "SiteClipperStoreTest", name -> {
            assertEquals("SiteClipperStoreTest", name);
            calls.incrementAndGet();
            return project;
        });
        assertSame(project, context.project);
        assertEquals("SiteClipperStoreTest", context.projectName);
        SiteClipperConnector.Shuttle.restoreStoredProject(context, "SiteClipperStoreTest", name -> {
            calls.incrementAndGet();
            return null;
        });
        assertSame(project, context.project);
        assertEquals(1, calls.get());
    }

    @Test
    public void newContextIsStillUninitialized() throws Exception {
        Context context = new Context("session_clip");
        context.projectName = "SiteClipperStoreTest";
        SiteClipperConnector.Shuttle.restoreStoredProject(context, "SiteClipperStoreTest", name -> {
            throw new AssertionError("A new context must not load a project");
        });
        assertNull(context.project);
    }
}
