/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.engine.mobile;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.DatabaseObjectImportedEvent;
import com.twinsoft.convertigo.engine.DatabaseObjectListener;
import com.twinsoft.convertigo.engine.DatabaseObjectLoadedEvent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class ComponentRefManager implements DatabaseObjectListener {
	
	public enum Mode {
		start,
		stop,
		use
	}
	
	static private ComponentRefManager cm = new ComponentRefManager();
	
	static public ComponentRefManager get(Mode mode) {
		if (mode.equals(Mode.stop)) {
			cm.clear();
		}
		return cm;
	}
	
	private Map<String, Set<String>> consumers = new HashMap<String, Set<String>>();
	
	private ComponentRefManager() {
		
	}

	private void clear() {
		synchronized (consumers) {
			consumers.clear();
		}
	}
	
	public void addConsumer(String compQName, String useQName) {
		if (compQName.startsWith(MobileBuilder.projectName(useQName)))
			return;
		synchronized (consumers) {
			if (consumers.get(compQName) == null) {
				consumers.put(compQName, new HashSet<String>());
			}
			if (consumers.get(compQName).add(useQName)) {
				Engine.logEngine.trace(useQName + " has been added as consumer for comp: "+ compQName + ", consummers:"+consumers.get(compQName).size());
			}
		}
	}
	
	public void removeConsumer(String compQName, String useQName) {
		synchronized (consumers) {
			if (consumers.get(compQName) != null) {
				consumers.get(compQName).remove(useQName);
			}
		}
	}
	
	public void copyKey(String old_qname, String new_qname) {
		synchronized (consumers) {
			if (consumers.get(old_qname) != null) {
				Set<String> newSet = new HashSet<String>(consumers.get(old_qname));
				consumers.put(new_qname, newSet);
			}
		}
	}
	
	public void removeKey(String compQName) {
		synchronized (consumers) {
			if (consumers.get(compQName) != null) {
				consumers.remove(compQName);
			}
		}
	}
	
	public Set<String> getAllConsumers(String compQName) {
		Set<String> set = new HashSet<String>();
		try {
	    	for (String keyQName: getKeys()) {
	    		if (!keyQName.equals(compQName)) {
		    		for (String useQName: getConsumers(compQName)) {
		    			if (useQName.startsWith(keyQName)) {
		    				if (!set.contains(useQName)) {
		    					set.addAll(getAllConsumers(keyQName));
		    				}
		    			}
		    		}
	    		}
	    	}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		synchronized (consumers) {
			if (consumers.get(compQName) != null) {
				set.addAll(consumers.get(compQName));
			}
		}

		return Collections.unmodifiableSet(set);
	}
	
	private Set<String> getConsumers(String compQName) {
		synchronized (consumers) {
			if (consumers.get(compQName) != null) {
				return Collections.unmodifiableSet(consumers.get(compQName));
			}
			return Collections.emptySet();
		}
	}

	public Set<String> getKeys() {
		synchronized (consumers) {
			return Collections.unmodifiableSet(consumers.keySet());
		}
	}
	
	@Override
	public void databaseObjectLoaded(DatabaseObjectLoadedEvent event) {
		
	}

	@Override
	public void databaseObjectImported(DatabaseObjectImportedEvent event) {
		DatabaseObject dbo = (DatabaseObject)event.getSource();
		
		if (dbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIUseShared) {
			com.twinsoft.convertigo.beans.ngx.components.UIUseShared uius = GenericUtils.cast(dbo);
			String useQName = uius.getQName();
			String compQName = uius.getSharedComponentQName();
			addConsumer(compQName, useQName);
		}
	}
	
}
