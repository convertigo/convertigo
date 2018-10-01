package com.twinsoft.convertigo.engine.helpers;

import java.util.HashSet;
import java.util.Set;

public class BatchOperationHelper {
	private final static ThreadLocal<Set<Runnable>[]> batchOperation = new ThreadLocal<Set<Runnable>[]>() {
		@SuppressWarnings("unchecked")
		@Override
		protected Set<Runnable>[] initialValue() {
			return new Set[1];
		}
		
	};
	
	static public void start() {
		batchOperation.get()[0] = new HashSet<Runnable>();
	}
	
	static public void stop() {
		Set<Runnable>[] array = batchOperation.get();
		if (array[0] != null) {
			for (Runnable runnable: array[0]) {
				runnable.run();
			}
		}
		batchOperation.remove();
	}
	
	static public void cancel() {
		batchOperation.remove();
	}
	
	static public void check(Runnable runnable) {
		Set<Runnable>[] array = batchOperation.get();
		if (array[0] != null) {
			array[0].add(runnable);
		} else {
			runnable.run();
		}
	}
}
