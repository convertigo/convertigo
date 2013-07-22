/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsageMonitor implements Runnable, AbstractManager {

	private boolean bContinue = false;
	
	private Map<String, Object> usageCounters;
	
	public void setUsageCounter(String counter, Object value) {
		usageCounters.put(counter, value);
	}
	
	public void run() {
		bContinue = true;
		
		try {
			while (bContinue) {
		        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
		        MemoryUsage memoryUsageHeap = memoryMXBean.getHeapMemoryUsage(); 
		        MemoryUsage memoryUsageNonHeap = memoryMXBean.getNonHeapMemoryUsage();
		        
		        setUsageCounter("[Memory] Heap memory usage", memoryUsageHeap);
		        setUsageCounter("[Memory] Non heap memory usage", memoryUsageNonHeap);
		        setUsageCounter("[Memory] Total used", (memoryUsageHeap.getUsed() + memoryUsageNonHeap.getUsed()) / 1024 + "K");
		        
				synchronized (usageCounters) {
					List<String> ls = new ArrayList<String>();
					ls.addAll(usageCounters.keySet());
					Collections.sort(ls);
					for (String counter : ls) {
						Engine.logUsageMonitor.info(counter + ": " + usageCounters.get(counter));
					}
				}
			
				Thread.sleep(30000);
			}
		} catch (NullPointerException npe) {
			if (Engine.theApp == null) {
				// Engine.theApp == null probably means the engine is getting stopped
				// so gracefully ignore and exit thread
				return;
			}
			else {
				npe.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		usageCounters = null;
	}

	public void destroy() throws EngineException {
		bContinue = false;
	}

	public void init() throws EngineException {
		usageCounters = new HashMap<String, Object>();
	}

}
