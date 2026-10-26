/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;

import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.transactions.HttpTransaction;
import com.twinsoft.convertigo.engine.util.NativeRunning;

/**
 * The billing tickets, and the POST of the ticket managers behind them, as in origin/hotfix: one per requestable, the
 * request that came in and each internal call, for a classic sequence and its steps as for a native one and its calls.
 */
public class BillingInternalCallsTest {
	@After
	public void tearDown() {
		NativeRunning.restore(null);
	}

	/** a context without the engine its constructor needs: only its fields matter for the billing */
	private static Context newContext() {
		try {
			java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			return (Context) ((sun.misc.Unsafe) field.get(null)).allocateInstance(Context.class);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private static Context context(RequestableObject requested, Context parent, boolean nativeExecution) {
		Context context = newContext();
		context.requestedObject = requested;
		context.parentContext = parent;
		context.nativeExecution = nativeExecution;
		return context;
	}

	private static RequestableObject sequence(String name) throws EngineException {
		GenericSequence sequence = new GenericSequence();
		sequence.setName(name);
		return sequence;
	}

	private static RequestableObject transaction(String name) throws EngineException {
		HttpTransaction transaction = new HttpTransaction();
		transaction.setName(name);
		return transaction;
	}

	@Test
	public void aClassicSequenceCallingATransactionAndASubSequenceHasOneTicketPerRequestable() throws EngineException {
		Context main = context(sequence("Main"), null, false);
		Context[] requests = { main, context(transaction("Read"), main, false), context(sequence("Sub"), main, false) };
		Boolean mark = NativeRunning.enter(false);
		int tickets = 0;
		for (Context request : requests) {
			tickets += Engine.isBilled(request) ? 1 : 0;
		}
		NativeRunning.restore(mark);
		assertEquals(3, tickets);
	}

	@Test
	public void aNativeSequenceMakingNativeCallsHasOneTicketPerRequestable() throws EngineException {
		Context main = context(sequence("Main"), null, true);
		Context[] requests = { main, context(transaction("Read"), main, true), context(sequence("Sub"), main, true),
				context(sequence("Classic"), main, false) };
		Boolean mark = NativeRunning.enter(true);
		int tickets = 0;
		for (Context request : requests) {
			tickets += Engine.isBilled(request) ? 1 : 0;
		}
		NativeRunning.restore(mark);
		assertEquals(4, tickets);
	}

	@Test
	public void noTicketWithoutRequestable() {
		Context context = newContext();
		assertEquals(false, Engine.isBilled(context));
	}
}
