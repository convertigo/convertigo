package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.*;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.mozilla.javascript.ClassCache;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class RhinoClassCacheTest {
	@Test public void sharesMetadataWithoutSharingRequestBindingsOrPrototypes() throws Exception {
		Engine.logEngine = Logger.getLogger("test");
		EnginePropertiesManager.initProperties();
		EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_SHARED_CLASS_CACHE, "true");
		try (var cx = Context.enter()) {
			var first = RhinoClassCache.newScope(cx);
			var second = RhinoClassCache.newScope(cx);
			assertSame(ClassCache.get(first), ClassCache.get(second));
			cx.evaluateString(first, "var privateValue = 1; Array.prototype.privateValue = 2; java.lang.String.valueOf(42)", "first", 1, null);
			assertEquals("undefined/undefined/42.0", Context.toString(cx.evaluateString(second,
				"typeof privateValue + '/' + typeof [].privateValue + '/' + java.lang.String.valueOf(42)", "second", 1, null)));
			var field = ClassCache.class.getDeclaredField("associatedScope"); field.setAccessible(true);
			var neutral = (ScriptableObject) field.get(ClassCache.get(first));
			assertNotSame(first, neutral); assertNotSame(second, neutral);
			assertFalse(neutral.has("privateValue", neutral));
			var thread = Thread.currentThread(); var old = thread.getContextClassLoader();
			try {
				thread.setContextClassLoader(new ClassLoader(old) {});
				assertNotSame(ClassCache.get(first), ClassCache.get(RhinoClassCache.newScope(cx)));
			} finally { thread.setContextClassLoader(old); }
			EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_SHARED_CLASS_CACHE, "false");
			assertNotSame(ClassCache.get(first), ClassCache.get(RhinoClassCache.newScope(cx)));
		} finally { EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_SHARED_CLASS_CACHE, "false"); }
	}
}
