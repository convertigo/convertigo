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

import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.IScriptComponent;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionStack;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
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
	
	static public FileFilter copyFileFilter = new FileFilter() {
		public boolean accept(File file) {
			return !file.getName().endsWith(".temp.ts");
		}
	};
	
	private Map<String, Set<String>> consumers = new HashMap<String, Set<String>>();
	
	private ComponentRefManager() {
		
	}

	private void clear() {
		synchronized (consumers) {
			consumers.clear();
		}
	}
	
	public void addConsumer(final String compQName, final String useQName) {
		synchronized (consumers) {
			if (consumers.get(compQName) == null) {
				consumers.put(compQName, new HashSet<String>());
			}
			if (consumers.get(compQName).add(useQName)) {
				Engine.logEngine.trace(useQName + " has been added as consumer for comp: "+ compQName + ", consummers:"+consumers.get(compQName).size());
			}
		}
	}
	
	public void removeConsumer(final String compQName, final String useQName) {
		synchronized (consumers) {
			if (consumers.get(compQName) != null) {
				consumers.get(compQName).remove(useQName);
			}
		}
	}
	
	public void copyKey(final String old_qname, final String new_qname) {
		synchronized (consumers) {
			if (consumers.get(old_qname) != null) {
				Set<String> newSet = new HashSet<String>(consumers.get(old_qname));
				consumers.put(new_qname, newSet);
			}
		}
	}
	
	public void removeKey(final String compQName) {
		synchronized (consumers) {
			if (consumers.get(compQName) != null) {
				consumers.remove(compQName);
			}
		}
	}
	
	static public boolean isEnabled(String qname) {
		if (qname != null && !qname.isEmpty()) {
			DatabaseObject dbo = getDatabaseObjectByQName(qname);
			if (dbo != null && dbo.getParent() != null) {
				return isEnabled(dbo);
			}
		}
		return false;
	}
	
    static public boolean isEnabled(DatabaseObject dbo) {
    	try {
    		if (dbo != null) {
    			DatabaseObject databaseObject = dbo;
    			while (!(databaseObject instanceof IScriptComponent) && databaseObject != null) { 
    				if (databaseObject instanceof UIComponent) {
    					if (!((UIComponent)databaseObject).isEnabled()) {
    						return false;
    					}
    					
    					if (databaseObject instanceof UIUseShared) {
    						UIUseShared uius = (UIUseShared)databaseObject;
    						UISharedComponent uisc = uius.getTargetSharedComponent();
    						if (uisc == null || (uisc != null && !uisc.isEnabled())) {
    							return false;
    						}
    					}
    					if (databaseObject instanceof UIDynamicInvoke) {
    						UIDynamicInvoke uidi = (UIDynamicInvoke)databaseObject;
    						UIActionStack uias = uidi.getTargetSharedAction();
    						if (uias == null || (uias != null && !uias.isEnabled())) {
    							return false;
    						}
    					}
    				}
    				databaseObject = databaseObject.getParent();
    				if (databaseObject instanceof UIActionStack) {
    					break;
    				}
    			}
    			if (databaseObject != null) {
	    			if (databaseObject instanceof ApplicationComponent) {
	    				return true;
	    			} else if (databaseObject instanceof PageComponent) {
	    				return ((PageComponent)databaseObject).isEnabled();
	    			} else if (databaseObject instanceof UISharedComponent) {
	    				return ((UISharedComponent)databaseObject).isEnabled();
	    			} else if (databaseObject instanceof UIActionStack) {
	    				return ((UIActionStack)databaseObject).isEnabled();
	    			}
    			}
    		}
     	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	return false;
    }
    
    static public DatabaseObject getDatabaseObjectByQName(String qname) {
    	try {
			qname = qname.replaceFirst("\\.\\w+?:$", "");
			String[] name = qname.split("\\.");
			String project = name[0];
			DatabaseObject dbo = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(project, false);
			for (int i = 1; i < name.length; i++) {
				dbo = dbo.getDatabaseObjectChild(name[i]);
			}
			return dbo;
    	} catch (Exception e) {
    		return null;
    	}
	}
    
    static public String projectName(String qname) {
    	if (qname != null && !qname.isBlank()) {
    		try {
    			return qname.split("\\.")[0];
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
    	return "none";
    }
    
    static public boolean isCompUsedBy(String compQName, String projectName) {
		for (String useQName: getAllCompConsumers(compQName)) {
			if (projectName(useQName).equals(projectName)) {
				return true;
			}
		}
    	return false;
    }
    
    static public Set<String> getCompConsumersUsedBy(String compQName, String projectName) {
    	Set<String> set = new HashSet<String>();
    	for (String useQName: getAllCompConsumers(compQName)) {
			if (projectName(useQName).equals(projectName)) {
				set.add(useQName);
			}
		}
    	return Collections.unmodifiableSet(set);
    }

    static public Set<String> getProjectsForUpdate(String projectName) {
    	Set<String> set = new HashSet<String>();
		for (String compQName: getCompKeys()) {
			for (String useQName: getAllCompConsumers(compQName)) {
				if (projectName(useQName).equals(projectName)) {
					set.add(projectName(compQName));
				}
			}
		}
    	return Collections.unmodifiableSet(set);
    }

    static public Set<String> getCompKeys() {
    	return ComponentRefManager.get(Mode.use).getKeys();
    }
    
    static public Set<String> getCompConsumersForUpdate(String compQName, Project project, Project to) {
    	Set<String> set = new HashSet<String>();
		for (String useQName: getAllCompConsumers(compQName)) {
			if (projectName(useQName).equals(project.getName()))
				continue;
			if (to != null && !to.getName().equals(projectName(useQName)))
				continue;
			set.add(useQName);
		}
    	return Collections.unmodifiableSet(set);
    }
    
    static public Set<String> getAllCompConsumers(String compQName) {
    	return Collections.unmodifiableSet(ComponentRefManager.get(Mode.use).getAllConsumers(compQName));
    }
    
    static public Set<String> getCompConsumers(String compQName) {
    	return Collections.unmodifiableSet(ComponentRefManager.get(Mode.use).getConsumers(compQName));
    }
    
    
	private synchronized Set<String> getAllConsumers(String compQName) {
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
		
		//System.out.println("consumers for "+ compQName + ": "+ set);
		return Collections.unmodifiableSet(set);
	}
	
	private Set<String> getConsumers(final String compQName) {
		synchronized (consumers) {
			if (consumers.get(compQName) != null) {
				return Collections.unmodifiableSet(consumers.get(compQName));
			}
			return Collections.emptySet();
		}
	}

	private Set<String> getKeys() {
		synchronized (consumers) {
			Set<String> set = new HashSet<String>();
			set.addAll(Collections.unmodifiableSet(consumers.keySet()));
			return set;
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
		if (dbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke) {
			com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke uidi = GenericUtils.cast(dbo);
			String useQName = uidi.getQName();
			String compQName = uidi.getSharedActionQName();
			addConsumer(compQName, useQName);
		}
	}
	
}
