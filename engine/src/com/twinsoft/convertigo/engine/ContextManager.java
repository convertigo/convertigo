/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.engine;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.tuple.Pair;

import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Pool;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.events.PropertyChangeEvent;
import com.twinsoft.convertigo.engine.events.PropertyChangeEventListener;
import com.twinsoft.convertigo.engine.requesters.DefaultRequester;
import com.twinsoft.convertigo.engine.requesters.HttpSessionListener;
import com.twinsoft.convertigo.engine.requesters.PoolRequester;
import com.twinsoft.convertigo.engine.requesters.Requester;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.twinj.iJavelin;
import com.twinsoft.util.DevicePool;

public class ContextManager extends AbstractRunnableManager {
	public class MyPropertyChangeEventListener implements PropertyChangeEventListener{
		public void onEvent(PropertyChangeEvent event) {
			if (event.getKey() == PropertyName.POOL_MANAGER_TIMEOUT)
				loadParameters();				
		}
	};
	MyPropertyChangeEventListener myPropertyChangeEventListener;
	
	public static final String POOL_CONTEXT_ID_PREFIX = "/";
	public static final String STUDIO_CONTEXT_PREFIX = "studio_";
	public static final String CONTEXT_TYPE_UNKNOWN		= "";
	public static final String CONTEXT_TYPE_TRANSACTION	= "C";
	public static final String CONTEXT_TYPE_SEQUENCE 	= "S";
	
	
    private Map<String, Context> contexts;
    private int currentContextNum;
    
    private Map<String, DevicePool> devicePools;
    private long manage_poll_timeout = -1;

	private final Object contextCreationMutex = new Object();
	
	public void init() throws EngineException {
		Engine.logContextManager.info("ContextManager initialization...");

		try {
			contexts = new ConcurrentHashMap<String, Context>();
			currentContextNum = 0;
			
			devicePools = new HashMap<String, DevicePool>();
			Engine.theApp.eventManager.addListener(myPropertyChangeEventListener = new MyPropertyChangeEventListener(), PropertyChangeEventListener.class);

			loadParameters();
		} finally {
			Engine.logContextManager.debug("End of initialization");
		}
	}
    
	@Override
	public void destroy() throws EngineException {
		Engine.logContextManager.info("Destroying ContextManager...");

		super.destroy();
		
		// remove all contexts
		removeAll();
		contexts = null;
		
		// remove all devicePools
		removeDevicePools();
		devicePools = null;
		
		Engine.theApp.eventManager.removeListener(myPropertyChangeEventListener, PropertyChangeEventListener.class);
	}

	private void loadParameters() {
        try {
        	manage_poll_timeout = -1;
        	manage_poll_timeout = Integer.parseInt(EnginePropertiesManager.getProperty(PropertyName.POOL_MANAGER_TIMEOUT));
        	manage_poll_timeout = manage_poll_timeout <=0 ? -1 : manage_poll_timeout * 1000;
        } catch (Exception e) {}
	}
	
	public void add(Context context) {
			contexts.put(context.contextID, context);
		int size = contexts.size();
			Engine.logContextManager.debug("Context " + context.contextID + " has been added");
			Engine.logContext.debug("[" + context.contextID + "] Context created, project: " + context.projectName);
		Engine.logContextManager.info("Current in-use contexts: " + size);
		Engine.logUsageMonitor.info("[Contexts] Current in-use contexts: " + size);
	}

	private void addDevicePool(String poolID, DevicePool devicePool) {
		synchronized(devicePools) {
			devicePools.put(poolID, devicePool);
			Engine.logContextManager.info("DevicePool for '" + poolID + "' has been added.");
		}
	}

	public String computeStudioContextName(String type, String projectName, String typeName) {
		return STUDIO_CONTEXT_PREFIX + projectName + ":"+ type +":" + typeName;
	}
	
	public Context get(Requester requester, String contextName, String projectName) throws Exception {
		return get(requester, contextName, null, null, projectName, null);
	}

	public Context get(Requester requester, String contextName, String contextIdPrefix, String poolName, String projectName, String connectorName) throws Exception {
		return get(requester, contextName, null, null, projectName, null, null);
	}
	
	public Context get(Requester requester, String contextName, String contextIdPrefix, String poolName, String projectName, String connectorName, String sequenceName) throws Exception {
		Context context = null;

		// Try to find the context in pool
		if ((poolName != null) && (poolName.length() > 0)) {
			context = findPoolContext(contextName, projectName, connectorName, poolName);
			if (context != null) {
				return context;
			}
		}

		// If not found, try the legacy contexts

		// Studio mode?
		if (Engine.isStudioMode()) {
			// Execution from the Studio : do nothing
			if ((contextName != null) && (contextName.startsWith(STUDIO_CONTEXT_PREFIX))) {
				Engine.logContextManager.info("Using studio given context name : " + contextName);
			}
			// Execution out of the Studio (e.g: test platform) or call step
			else {
				if ((sequenceName != null) && !(sequenceName.equals(""))) {
					contextName = computeStudioContextName(CONTEXT_TYPE_SEQUENCE, projectName, sequenceName);
				} else if(connectorName != null && !(connectorName.equals(""))) {
					contextName = computeStudioContextName(CONTEXT_TYPE_TRANSACTION, projectName, connectorName);
				} else {
					try {
						Project project = Engine.objectsProvider.getProject(projectName);
						contextName = computeStudioContextName(CONTEXT_TYPE_TRANSACTION, projectName, project.getDefaultConnector().getName());
					} catch (EngineException ee) { // project not opened in studio
						contextName = computeStudioContextName(CONTEXT_TYPE_UNKNOWN, projectName,"");
					}
				}
				Engine.logContextManager.info("Dynamic studio context name computed: " + contextName);
			}
		}

		// Dynamic context name
		if (contextName.endsWith("*")) {
			String contextID;
			String prefix = contextName.substring(0, contextName.length() - 1);
			int i = 1;
			do {
				contextName = prefix + i;
				contextID = (contextIdPrefix == null ? "" : contextIdPrefix  + "_") + contextName;
				i++;
			} while ((context = get(contextID)) != null);
			Engine.logContextManager.info("Dynamic context name computed: " + contextName);
			context = get(contextID, contextName, projectName);
		}
		// Studio context
		else if (contextName.startsWith(STUDIO_CONTEXT_PREFIX)) {
			String contextID = contextName;
			context = get(contextID, contextName, projectName);
		}
		// Classic context
		else {
			String contextID = contextIdPrefix + "_" + contextName;
			context = get(contextID, contextName, projectName);
		}

		return context;
	}
    
	private Context get(String contextID, String contextName, String projectName) throws EngineException {
		Context context = get(contextID);
		// Create a new context
		if (context == null) {
			long numberOfContext = contexts.size();
			long maxNumberOfContext = EnginePropertiesManager.getPropertyAsLong(PropertyName.CONVERTIGO_MAX_CONTEXTS);
			if (numberOfContext >= maxNumberOfContext) {
				Engine.logContextManager.warn("Max number of contexts reached: " + numberOfContext + "/" + maxNumberOfContext);
				throw new MaxNumberOfContextsException("Maximum number of contexts reached, please try later");
			} else {
				Engine.logContextManager.debug("Current number of contexts: " + numberOfContext + "/" + maxNumberOfContext);
			}
			
			context = new Context(contextID);
			context.name = contextName;
			context.cacheEntry = null;
			currentContextNum++;
			context.contextNum = currentContextNum;
			long creationTime = System.currentTimeMillis();
			context.creationTime = creationTime;
			context.lastAccessTime = creationTime;
			context.projectName = projectName;
			
			synchronized (contextCreationMutex) {
				Context existing = contexts.get(context.contextID);
				if (existing == null) {
					Engine.logContextManager.debug("Context \"" + contextName + "\" not found; creating the execution context");
					Engine.logContextManager.debug("Setting the creation time for context " + contextID + ": " + creationTime);
					add(context);
				} else {
					Engine.logContextManager.debug("Context \"" + contextName + "\" found (prevented parallel creation).");
					context = existing;
				}
			}
		} else {
			Engine.logContextManager.debug("Context \"" + contextName + "\" found.");
		}
		return context;
	}
    
	public Context getContextByName(String contextName) {
		for (Context ctx : contexts.values()) {
			if (ctx.name.equals(contextName)) {
				return ctx;
			}
		}
		return null;
	}
	
    public Context get(String contextID) {
            return contexts.get(contextID);
        }

    private DevicePool getDevicePool(String poolID) {
        synchronized(devicePools) {
            return devicePools.get(poolID);
        }
    }

    public synchronized DevicePool getDevicePool(String poolID, int iStart, int iStop, int iIncr, int iDigits) {
    	DevicePool devicePool = getDevicePool(poolID);
        if (devicePool == null) {
        	devicePool = new DevicePool();
        	devicePool.init(iStart,iStop,iIncr,iDigits);
        	addDevicePool(poolID,devicePool);
        }
    	return devicePool;
    }
    
    public List<Context> getContexts(HttpSession httpSession) {
		try {
			synchronized (httpSession) {
				return GenericUtils.cast(SessionAttribute.contexts.get(httpSession));
			}
		}
		catch (Exception e) {
		}
		return Collections.emptyList();
    }
    
    public boolean isSessionEmtpy(HttpSession httpSession) {
		int size = getContexts(httpSession).size();
		Engine.logContextManager.debug("(ContextManager) Contexts from the session " + httpSession.getId() + ": "+ size);
		return size <= 0;
	}
    
    @Deprecated
    public Enumeration<?> getAll() {
    	return Collections.enumeration(contexts.values());
    }

    public Collection<String> getContextIds(){
    	return contexts.keySet();
    }
    
    public Collection<Context> getContexts() {
		Collection<Context> res = new ArrayList<Context>(contexts.values());
    	return res;
    }
    
    public int getNumberOfContexts() {
        return contexts.size();
    }

    public void abort(String contextID) {
        	abort(get(contextID));
        }
    
    public void abort(Context context) {
			if (context == null) {
				// Silently ignore
				Engine.logContextManager.warn("Requestable thread cannot be stopped because context does not exist any more!");
				return;
			}
			
			String contextID = context.contextID;
			if ((context.requestedObject != null) && (context.requestedObject.runningThread != null)) {
				Engine.logContextManager.info("Stopping requestable thread for context " + contextID);
				context.abortRequestable();
			}
    	}
    
    public void remove(String contextID) {
		Engine.logContextManager.info("Removing context '" + contextID + "'");
		Context context;
        	context = contexts.remove(contextID);
        if (context != null) {
        	remove(context);
        }
    }
    
    public void remove(Context context) {
		if (context == null) {
			// Silently ignore
			Engine.logContextManager.warn("The context cannot be removed because it does not exist any more!");
			return;
		}
		if (context.isDestroying) {
			return;
		}
		context.isDestroying = true;
		context.requireRemoval(false);
		
		try {
			String contextID = context.contextID;
			Engine.logContextManager.info("Removing context " + contextID);
			
	        	contexts.remove(contextID);
	
			if ((context.requestedObject != null) && (context.requestedObject.runningThread != null)) {
				Engine.logContextManager.debug("Stopping requestable thread for context " + contextID);
				//context.requestedObject.runningThread.bContinue = false;
				context.abortRequestable();
			}
			
			// Trying to execute the end transaction (only in the engine mode)
			if (Engine.isEngineMode()) {
				for (Connector connector: context.getOpenedConnectors()) {
					// Execute the end transaction
					String endTransactionName = "n/a";
					try {
						endTransactionName = connector.getEndTransactionName();
						if (endTransactionName != null && !endTransactionName.equals("")) {
							Engine.logContextManager.debug("Trying to execute the end transaction: \"" + endTransactionName + "\"");
							context.connectorName = connector.getName();
							context.connector = connector;
							context.transactionName = endTransactionName;
							context.sequenceName = null;
							DefaultRequester defaultRequester = new DefaultRequester();
							// #4910 - prevent loop for destroying context renew
							context.isDestroying = false;
							context.requireRemoval(false);
							defaultRequester.processRequest(context);
							Engine.logContextManager.debug("End transaction successfull");
						}
					} catch (Throwable e) {
						Engine.logContextManager.error("Unable to execute the end transaction; " +
							"context: " + context.contextID + ", " +
							"project: " + context.projectName + ", " +
							"connector: " + context.connectorName + ", " +
							"end transaction: " + endTransactionName,
							e);
					} finally {
						context.isDestroying = true;					
					}
					// Unlocks device if any
					// WARNING: removing the device pool MUST BE DONE AFTER the end transaction!!!
					String connectorQName = connector.getQName();
					DevicePool devicePool = getDevicePool(connectorQName);
					if (devicePool != null) {
						long contextNum = (Long.valueOf(Integer.toString(context.contextNum,10))).longValue();
						Engine.logContextManager.trace("DevicePool for '"+ connectorQName +"' exist: unlocking device for context number "+ contextNum +".");
						devicePool.unlockDevice(contextNum);
					}
					
					Engine.logContextManager.trace("Releasing " + connector.getName() + " connector (" + connector.getClass().getName() + ") for context id " + context.contextID);
					Engine.execute(new Runnable() {
						public void run() {
							connector.release();
						}
					});
				}
			}
	
			context.clearConnectors();
			
			// Set TwsCachedXPathAPI to null
			context.cleanXpathApi();
			
	        Engine.theApp.sessionManager.removeSession(contextID);
			String projectName = (String) context.projectName;
			
			/* Fix: #1754 - Slower transaction execution with many session */
			// HTTP session maintain its own context list in order to
			// improve context removal on session unbound process
			// See also #4198 which fix a regression
			String sessionID = context.httpSession != null ? context.httpSession.getId() :
									context.contextID.substring(0,context.contextID.indexOf("_"));
			HttpSession httpSession = HttpSessionListener.getHttpSession(sessionID);
			if (httpSession != null) {
				synchronized (httpSession) {
					try {
						List<Context> contextList = GenericUtils.cast(SessionAttribute.contexts.get(httpSession));
						if ((contextList != null) && contextList.contains(context)) {
							contextList.remove(context);
							Engine.logContextManager.debug("(ContextManager) context " + contextID + " has been removed from http session's context list");
						}
					}
					catch (Exception e) {
						// Ignore: HTTP session may have already been invalidated
					}
				}
			}
	
	        Engine.logContextManager.debug("Context " + contextID + " has been removed");
	        Engine.logContext.debug("[" + contextID + "] Context removed, project: " + projectName);
	        Engine.logContextManager.info("Current in-use contexts: " + contexts.size());
	        Engine.logUsageMonitor.info("[Contexts] Current in-use contexts: " + contexts.size());
		} catch (Exception e) {
			Engine.logContextManager.warn("Failed to remove the context " + context.contextID, e);
		}
    }

    public void removeAll(String sessionID) {
    	HttpSession httpSession = HttpSessionListener.getHttpSession(sessionID);
    	if (httpSession != null) {
    		removeAll(httpSession);
    	}
    }

	public void removeAll(HttpSession httpSession) {
		String sessionID = httpSession.getId();
		Engine.logContextManager.debug("Removing all contexts for " + sessionID + "...");
		try {
			Object o = SessionAttribute.contexts.get(httpSession);
			if (o == null) {
				return;
			}

			List<Context> contextList = GenericUtils.clone(GenericUtils.cast(o));
			
			for (Context context: contextList) {
				remove(context);
			}
		} catch (Exception e) {
			if (e instanceof IllegalStateException || e instanceof NullPointerException) {
				try {
					for (String contextID: contexts.keySet()) {
						if (contextID.startsWith(sessionID)) {
							remove(contextID);
						}
					}
				} catch (Exception e2) {
					Engine.logContextManager.debug("Failed to removeAll for " + sessionID, e2);
					// prevent exception propagation
				}
			}
		}
	}
    
	public void removeAll() {
		Engine.logContextManager.debug("Removing all contexts...");
		try {
			for (String contextID : contexts.keySet()) {
				remove(contextID);
			}
		} catch(NullPointerException e) {
			// Nothing to do: the Engine object has yet been deleted
		}
	}

	public void removeDevicePools() {
		Engine.logContextManager.debug("Removing all devicePools...");
		try {
			for (String poolID : GenericUtils.clone(devicePools).keySet()) {
				removeDevicePool(poolID);
			}
		} catch(NullPointerException e) {
			// Nothing to do: the Engine object has yet been deleted
		}
	}
	
	public void removeDevicePool(String poolID) {
		DevicePool devicePool = getDevicePool(poolID);
		if (devicePool != null) {
			synchronized(devicePool) {
				devicePool.clean("");
			}
		}
		synchronized(devicePools) {
			devicePools.remove(poolID);
		}
	}

	public void run() {
		Engine.logContextManager.info("Starting the vulture thread for context management");

		if (Engine.isStudioMode()) {
			Engine.logContextManager.warn("Studio context => pools won't be initialized!");
		}

        while (isRunning) {
            Engine.logContextManager.debug("Vulture task in progress");
        	long sleepTime = System.currentTimeMillis() + 30000;
            try {
				Engine.theApp.usageMonitor.setUsageCounter("[Contexts] Number", contexts.size());
				int maxNbCurrentWorkerThreads = Integer.parseInt(EnginePropertiesManager.getProperty(PropertyName.DOCUMENT_THREADING_MAX_WORKER_THREADS));
				Engine.theApp.usageMonitor.setUsageCounter("[Contexts] [Worker threads] In use", com.twinsoft.convertigo.beans.core.RequestableObject.nbCurrentWorkerThreads + " (" + 100 * com.twinsoft.convertigo.beans.core.RequestableObject.nbCurrentWorkerThreads / maxNbCurrentWorkerThreads + "%)");
				Engine.theApp.usageMonitor.setUsageCounter("[Contexts] [Worker threads] Max", maxNbCurrentWorkerThreads);

                removeExpiredContexts();
                managePoolContexts();
                clearOldLogs();
                Engine.logContextManager.debug("Vulture task done");
            } catch(Throwable e) {
                Engine.logContextManager.error("An unexpected error has occured in the ContextManager vulture.", e);
            } finally {
            	if ((sleepTime -= System.currentTimeMillis()) > 0) {
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						// Ignore
						Engine.logContextManager.debug("InterruptedException received: probably a request for stopping the vulture.");
					}
				}
            }
        }

		Engine.logContextManager.info("The vulture thread has been stopped.");
    }
	
	private void removeExpiredContexts() {
        Engine.logContextManager.debug("Executing vulture thread for context expiration");
        
		for (Map.Entry<String, Context> entry : contexts.entrySet()) {
            if (!isRunning) return;
            String contextID = entry.getKey();
            long expirationTime;
            try {
                Context context = entry.getValue();
                
                context.checkXulRecorder();

				// Ignoring removing of pool contexts only if the pooled context has not been locked.
                // If the pooled context has been locked, it may be a zombie context and then must be
                // removed (it will be recreated after).
				if (contextID.startsWith(POOL_CONTEXT_ID_PREFIX) && !context.lockPooledContext) {
					context.lastAccessTime = Calendar.getInstance().getTime().getTime();
					continue;
				}
				
                if ((context.project == null) || (context.lastAccessTime == 0))
					continue; // The context has not been completely created, so we ignore this context...
                else expirationTime = context.lastAccessTime + context.project.getContextTimeout() * 1000;

                // Engine mode (studio contexts don't expire)
                if (Engine.isEngineMode()) {
	                Engine.logContextManager.debug("Analyzing contextID " + contextID + ": expiration time = " + expirationTime);
	
	                long rightNow = Calendar.getInstance().getTime().getTime();
	                if (rightNow > expirationTime) {
	                    Engine.logContextManager.info("The context " + contextID + " has expired!");
	                    remove(contextID);
	                }
                }
            } catch(Exception e) {
                Engine.logContextManager.error("An unexpected error has occured in the ContextManager vulture while analyzing the context \"" + contextID + "\".", e);
            }
        }
	}
	
	private int pooledContextsInUse = 0;
	private int pooledContextsLocked = 0;
	private int pooledContextsZombie = 0;
	private int pooledContextsToCreate = 0;
	private Set<Pair<Pool, Integer>> pooledContextsToCreateSet= new HashSet<Pair<Pool,Integer>>();

	private void managePoolContexts() {
		if (Engine.isStudioMode()) {
			return;
		}
		
		if (!Engine.isStarted) {
			Engine.logContextManager.debug("Engine is stopped => do not manage pools");
			return;
		}

        Engine.logContextManager.debug("Executing vulture thread for context pooling");

        try {
        	long timeout = manage_poll_timeout;
        	long now = System.currentTimeMillis();
        	if (timeout != -1) {
        		timeout += now;
        	}
			
        	pooledContextsToCreateSet.clear();
        	Map<String, Integer> counters = new HashMap<String, Integer>();
        	
			// Create the pooled contexts and initialize the pooled contexts
			// with the auto-start transaction
			for (String projectName : Engine.theApp.databaseObjectsManager.getAllProjectNamesList()) {
				if (!isRunning) return;
				
				Engine.logContextManager.trace("Analyzing project " + projectName);
				Project project = null;
				try {
					project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
				} catch (Exception e) {
					Engine.logContextManager.warn("Unable to load project '" + projectName
							+ "'; avorting pool research for this project", e);
					continue;
				}

				Collection<Connector> vConnectors = project.getConnectorsList();
				Engine.logContextManager.trace("Connectors: " + vConnectors);
				
				for (Connector connector : vConnectors) {
					if (!isRunning) return;
		            Engine.logContextManager.trace("Connector: " + connector);

					Collection<Pool> vPools = connector.getPoolsList();
					Engine.logContextManager.trace("Pools: " + vPools);
					String poolName;
					for (Pool pool : vPools) {
						if (!isRunning) return;
			            poolName = pool.getName();
						Engine.logContextManager.trace("Pool: " + poolName);
						int pooledContexts = pool.getNumberOfContexts();
						Engine.logContextManager.debug("Pool size: " + pooledContexts);
						String poolNameWithPath = pool.getNameWithPath();
						
						pooledContextsInUse = 0;
						pooledContextsLocked = 0;
						pooledContextsZombie = 0;
						pooledContextsToCreate = 0;
						counters.put(poolNameWithPath, 0);

						if (pooledContexts > 0) {
							for (int i = 1 ; i <= pool.getNumberOfContexts() ; i++) {
								if (!isRunning) return;
					            Project localProject = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
								Connector localConnector = localProject.getConnectorByName(connector.getName());
								Pool localPool = localConnector.getPoolByName(pool.getName());
								String servCode = localPool.getServiceCode();
								if (servCode != null && !servCode.equals("")) {
									if (localConnector instanceof JavelinConnector) {
										((JavelinConnector)localConnector).setServiceCode(servCode);
										Engine.logContextManager.trace("Connector service code overridden to : " + servCode);
									}
									// TODO add code for each specific connector to use pools serviceCode property
								}
								managePoolContext(localProject, localConnector, localPool, i);
							}
							
							int pooledContextsInUsePercentage = 100 * pooledContextsInUse / pooledContexts;
							int pooledContextsLockedPercentage = 100 * pooledContextsLocked / pooledContexts;
							
							String poolStatistics = "Pool '" + poolNameWithPath + 
								"' usage: pool size: " + pooledContexts + "; in use contexts: " + pooledContextsInUse +
								" (" + pooledContextsInUsePercentage + "%); zombie contexts: " + pooledContextsZombie;;
							
							if (pooledContextsZombie > 0) {
								Engine.logContextManager.warn("Pool '" + poolNameWithPath + "' had zombie contexts!");
								Engine.logContextManager.warn(poolStatistics);
							}
							
							if (pooledContextsInUsePercentage > 80) {
								Engine.logContextManager.warn("Pool '" + poolNameWithPath  + "' is overloaded!");
								Engine.logContextManager.warn(poolStatistics);
							}

							Engine.theApp.usageMonitor.setUsageCounter("[Pool] '" + poolNameWithPath + "' size", pooledContexts);
							Engine.theApp.usageMonitor.setUsageCounter("[Pool] '" + poolNameWithPath + "' in use contexts", pooledContextsInUse + " (" + pooledContextsInUsePercentage + "%)");
							Engine.theApp.usageMonitor.setUsageCounter("[Pool] '" + poolNameWithPath + "' locked contexts", pooledContextsLocked + " (" + pooledContextsLockedPercentage + "%)");
							Engine.theApp.usageMonitor.setUsageCounter("[Pool] '" + poolNameWithPath + "' zombie contexts", pooledContextsZombie);
							Engine.theApp.usageMonitor.setUsageCounter("[Pool] '" + poolNameWithPath + "' to be created contexts", pooledContextsToCreate);
						}
					}
				}
			}
			
			for (Pair<Pool, Integer> pooledContextToCreate : pooledContextsToCreateSet) {
				if (!isRunning) return;
	            String key = pooledContextToCreate.getKey().getNameWithPath();
				createPoolContext(pooledContextToCreate.getKey(), pooledContextToCreate.getValue());
				counters.put(key, counters.get(key)+1);
				if(timeout != -1 && (now = System.currentTimeMillis()) > timeout) break;
			}
			for (Entry<String, Integer> entry : counters.entrySet()) {
				if (!isRunning) return;
	            Engine.theApp.usageMonitor.setUsageCounter("[Pool] '" + entry.getKey() + "' (re)created contexts", entry.getValue());
			}
		} catch (EngineException e) {
			Engine.logContextManager.error("An unexpected error has occured in the ContextManager vulture while managing the pool contexts.", e);
		}

		Engine.logContextManager.debug("Pools creation successfully finished");
	}

	private void managePoolContext(Project project, Connector connector, Pool pool, int contextNumber) {
		String projectName = project.getName();
		String connectorName = connector.getName();
		String poolName = pool.getName();
		String poolContextID = getPoolContextID(projectName, connectorName, poolName, "" + contextNumber);
		Engine.logContextManager.trace("Managing the context " + poolContextID);
		
		Context context = get(poolContextID);
		if (context != null) { // Context already created
			if (context.waitingRequests == 0) { // Context not currently used
				// Ignore locked contexts
				if (context.lockPooledContext) {
					pooledContextsLocked++;
					Engine.logContextManager.debug("Context has been locked; ignoring possible zombie state");
					return;
				}
				
				// Checking pool context state
				if (verifyPoolContext(context))
					return;
				
				Engine.logContextManager.debug("Zombie context => destroying it!");
				remove(context);
				pooledContextsZombie++;
			} else {
				Engine.logContextManager.debug("Aborting pool context analysis because the context is currently used");
				pooledContextsInUse++;
				return;
			}
		}
	
		// Context not yet created or removed (detected as a zombie context)
		pooledContextsToCreate++;
		pooledContextsToCreateSet.add(Pair.<Pool, Integer>of(pool, contextNumber));
	}
	
	private void createPoolContext(Pool pool, int contextNumber){
		try {
			if (!isRunning) return;
            Connector connector = pool.getConnector();
			Project project = connector.getProject();
			
			String poolContextID = getPoolContextID(project.getName(), connector.getName(), pool.getName(), "" + contextNumber);
			
			Engine.logContextManager.info("Creating context");
			Context context = get(poolContextID, contextNumber + "", project.getName());
			
			context.project = project;
			context.projectName = project.getName();
			//context.sequence = null;
			//context.sequenceName = null;
			context.setConnector(connector);
			context.pool = pool;
			context.poolContextNumber = contextNumber;
			context.transactionName = pool.getStartTransaction();
			if ((context.transactionName != null) && !context.transactionName.equals("")) {
				context.requestedObject = connector.getTransactionByName(context.transactionName);

				// For compatibility with older javelin projects, set the transaction context property
				context.transaction = (Transaction)context.requestedObject;
				
				Engine.logContextManager.debug("Launching the auto-start transaction \"" + context.transactionName + "\" for the context " + context.contextID);

				context.remoteAddr = "127.0.0.1";
				context.remoteHost = "localhost";
				context.userAgent = "Convertigo ContextManager pools launcher";

				try {
					if (!isRunning) return;
		            PoolRequester poolRequester = new PoolRequester();
					poolRequester.processRequest(context);
				} catch (Exception e) {
					Engine.logContextManager.error("Unable to launch the context " + context.contextID, e);
				}
			}
		} catch (EngineException e) {
			Engine.logContextManager.error("An unexpected error has occured in the ContextManager vulture while creating the pool context.", e);
		}
	}
	
	public static String getPoolContextID(String projectName, String connectorName, String poolName, String sessionName) {
		return POOL_CONTEXT_ID_PREFIX + projectName + "/" + connectorName + "/" + poolName + "_" + sessionName;
	}
	
	private Context findPoolContext(String contextName, String projectName, String connectorName, String poolName) throws EngineException {
			Engine.logContextManager.debug("Trying to find a pooled context");
			Engine.logContextManager.debug("   contextName=" + contextName);
			Engine.logContextManager.debug("   projectName=" + projectName);
			Engine.logContextManager.debug("   connectorName=" + connectorName);
			Engine.logContextManager.debug("   poolName=" + poolName);
			
			Project project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
			Connector connector;
			if (connectorName == null) {
				connector = project.getDefaultConnector();
				connectorName = connector.getName();
			} else connector = project.getConnectorByName(connectorName);

			// If we cannot find the pool, abort the process
			Pool pool = connector.getPoolByName(poolName);
			if (pool == null) {
				Engine.logContextManager.debug("No pool named '" + poolName + "'; aborting pool management");
				return null;
			}
			Engine.logContextManager.debug("Found pool=" + pool);
			pool.checkSymbols();

			String contextIDPrefix = ContextManager.getPoolContextID(projectName, connectorName, poolName, "");

			if (contextName != null && !contextName.equals("default") && !contextName.equals("default*")) {
				Engine.logContextManager.debug("Explicit pooled context '" + contextIDPrefix + contextName + "' has been required");
				Context context = get(contextIDPrefix + contextName);
				if (context == null) throw new EngineException("Explicit pooled context '" + contextIDPrefix + contextName + "' does not exist!");
				Engine.logContextManager.debug("context.waitingRequests=" + context.waitingRequests);
				Engine.logContextManager.debug("context.lockPooledContext=" + context.lockPooledContext);
				if (!context.lockPooledContext) throw new EngineException("Explicit pooled context '" + contextIDPrefix + contextName + "' has not been locked!");
				Engine.logContextManager.debug("The context has been previously locked and has been explicitely requested");
				return context;
			} else {
				Engine.logContextManager.debug("Searching for good candidate");
				for (Map.Entry<String, Context> entry : contexts.entrySet()) {
					Engine.logContextManager.debug("Analyzing context " + entry.getKey());				
					if (entry.getKey().startsWith(contextIDPrefix)) {
						Context context = entry.getValue();
						
						Engine.logContextManager.debug("context.waitingRequests=" + context.waitingRequests);
						Engine.logContextManager.debug("context.lockPooledContext=" + context.lockPooledContext);
						
						if ((context.waitingRequests == 0) && (!context.lockPooledContext) && verifyPoolContext(context)) {
							Engine.logContextManager.debug("Good candidate for election: " + context.contextID);				
							return entry.getValue();
						}
					}
				}
				throw new EngineException("No more available context on the pool " + poolName + "; please try again later.");
			}
		}

	private boolean verifyPoolContext(Context context) {
		JavelinConnector javelinConnector = (JavelinConnector) context.getConnector();
		
		if (javelinConnector == null) {
			return true;
		}
		
		// TODO: find why the javelin is null sometimes with pools
		if (javelinConnector.javelin == null) {
			return true;
		}
		
		Engine.logContextManager.trace("verifyPoolContext() context=" + context.contextID);
		Engine.logContextManager.trace("verifyPoolContext() connector=" + Integer.toHexString(javelinConnector.hashCode()));
		Engine.logContextManager.trace("verifyPoolContext() javelin=" + Integer.toHexString(javelinConnector.javelin.hashCode()));

		boolean isConnected = ((iJavelin) javelinConnector.javelin).isConnected();
		Engine.logContextManager.trace("verifyPoolContext() isConnected=" + isConnected);

		boolean isInExpectedScreenClass = true;
		String initialScreenClass = context.pool.getInitialScreenClass();
		String currentScreenClassName = "none";
		if (initialScreenClass.length() > 0) {
			ScreenClass currentScreenClass = javelinConnector.getCurrentScreenClass();
			currentScreenClassName = currentScreenClass.getName();
			isInExpectedScreenClass = initialScreenClass.equals(currentScreenClass.getName());
		}

		Engine.logContextManager.trace("verifyPoolContext() expected screen class: " + context.pool.getInitialScreenClass());
		Engine.logContextManager.trace("verifyPoolContext() current screen class: " + currentScreenClassName);
		Engine.logContextManager.trace("verifyPoolContext() isInExpectedScreenClass=" + isInExpectedScreenClass);
		
		boolean b = isConnected && isInExpectedScreenClass;
		if (!b) {
			Engine.logContextManager.warn("Zombie context detected! context: " + context.contextID);
		}
		return b;
	}
	
	private void clearOldLogs() {
		int iDot = Engine.LOG_ENGINE_NAME.indexOf('.');
		if (iDot < 1) {
			return;
		}
		long nb = EnginePropertiesManager.getPropertyAsLong(PropertyName.LOG4J_APPENDER_AUDITAPPENDER_MAXBACKUPINDEX);
		String prefix = Engine.LOG_ENGINE_NAME.substring(0, iDot + 1);
		String[] files = new File(Engine.LOG_PATH).list();
		Arrays.sort(files);
		for (int i = files.length - 1; i >= 0; i--) {
			if (files[i].startsWith(Engine.LOG_ENGINE_NAME)) {
				nb--;
			}
		}
		StringBuilder sb = null;
		for (int i = files.length - 1; i >= 0; i--) {
			if (files[i].startsWith(prefix) && !files[i].startsWith(Engine.LOG_ENGINE_NAME)) {
				if (nb > 0) {
					nb--;
				} else {
					File toDelete = new File(Engine.LOG_PATH, files[i]);
					boolean deleted = toDelete.delete();
					if (Engine.logEngine.isInfoEnabled()) {
						if (sb == null) {
							sb = new StringBuilder("Purging old log files:");
						}
						sb.append("\n - " + toDelete + " is deleted: " + deleted);
					}
				}
			}
		}
		if (sb != null) {
			Engine.logEngine.info(sb);
		}
	}
}