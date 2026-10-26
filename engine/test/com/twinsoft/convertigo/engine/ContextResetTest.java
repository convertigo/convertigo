/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Context.reset, run at the start of each request (GenericRequester.initContext), forgets the native state of the
 * request before: a classic request answered from the XML cache after a native one (run() not called, so
 * nativeExecution not set again) is no longer handled as native.
 */
public class ContextResetTest {
	@BeforeClass
	public static void logger() {
		if (Engine.logContext == null) {
			Engine.logContext = Logger.getLogger("cems.Context");
		}
	}

	/** a context without the engine its constructor needs (as BillingInternalCallsTest): reset only needs its table */
	private static Context newContext() {
		try {
			java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			Context context = (Context) ((sun.misc.Unsafe) field.get(null)).allocateInstance(Context.class);
			java.lang.reflect.Field table = AbstractContext.class.getDeclaredField("internalTable");
			table.setAccessible(true);
			table.set(context, new HashMap<String, Object>());
			return context;
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	@Test
	public void theNativeStateOfTheRequestBeforeIsForgotten() {
		Context context = newContext();
		// what a native request leaves in its context
		context.nativeExecution = true;
		context.nativeResult = "{\"answer\":1}";
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("__sequence", "Native");
		context.nativeParameters = parameters;
		context.jsonNativeOutputs = new java.util.LinkedHashMap<>();

		context.reset();

		assertFalse(context.nativeExecution);
		assertNull(context.nativeResult);
		assertNull(context.nativeParameters);
		assertNull(context.jsonNativeOutputs);
	}
}
