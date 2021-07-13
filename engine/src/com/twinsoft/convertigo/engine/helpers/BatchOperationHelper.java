/*
 * Copyright (c) 2001-2021 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.helpers;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.twinsoft.convertigo.engine.Engine;

public class BatchOperationHelper {
	private final static ThreadLocal<Set<Runnable>[]> batchOperation = new ThreadLocal<Set<Runnable>[]>() {
		@SuppressWarnings("unchecked")
		@Override
		protected Set<Runnable>[] initialValue() {
			return new Set[1];
		}
		
	};
	
	static public void start() {
		if (batchOperation.get()[0] == null) {
			batchOperation.get()[0] = new HashSet<Runnable>();
		} else {
			Engine.logEngine.error("(BatchOperationHelper) already started!");
		}
	}
	
	static public void stop() {
		Set<Runnable>[] array = batchOperation.get();
		if (array[0] != null) {
			if (array[0].size() > 0) {
				ExecutorService executor = null;
				try {
					executor = Executors.newFixedThreadPool(
							Math.min(
									Runtime.getRuntime().availableProcessors(),
									array[0].size()
									)
							);
					for (Runnable runnable: array[0]) {
						executor.execute(runnable);
					}
					executor.shutdown();
					executor.awaitTermination(2, TimeUnit.MINUTES);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			Engine.logEngine.error("(BatchOperationHelper) not started or already stopped!");
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
