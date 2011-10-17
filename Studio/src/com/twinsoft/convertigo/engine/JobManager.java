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

import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.cache.CacheManager;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.requesters.Requester;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class JobManager {

    private static Map<String, Job> jobs = new Hashtable<String, Job>(256);
    
    public static Document addJob(CacheManager cacheManager, DatabaseObject requestedObject, Requester requester, Context context) throws EngineException {
        Engine.logJobManager.debug("Adding job #" + context.contextID);
        Job job = new Job(cacheManager, requestedObject, requester, context);
        jobs.put(context.contextID, job);
        job.start();
        return JobManager.getJobStatus(context.contextID);
    }
    
    public static boolean jobExists(String jobID) {
        Job job = (Job) jobs.get(jobID);
        return (job != null);
    }
    
    public static Document abortJob(String jobID) throws EngineException {
        Engine.logJobManager.debug("Requesting job #" + jobID + " abortion");
        Job job = (Job) jobs.get(jobID);
    	
        if (job == null) {
            throw new EngineException("The requested job is not found (jobID=" + jobID + ").");
        }
        
        // Job is still running
        if (!job.isFinished) {
        	final Context jobContext = Engine.theApp.contextManager.get(jobID);
        	if (jobContext == null) {
        		throw new EngineException("The context for (jobID=" + jobID + ") does not exist anymore.");
        	}
        	
			new Thread(new Runnable(){
				public void run() {
					jobContext.abortRequestable();
				}
			}).start();
        	
            Engine.logJobManager.debug("Job #" + jobID + " aborted");
            jobs.remove(jobID);
            
            if (jobContext.outputDocument == null) {
                throw new EngineException("The job has not been successfully aborted! See the engine log file for more details.");
            }

            if (Engine.logJobManager.isTraceEnabled())
            	Engine.logJobManager.trace("JobManager abortJob: isFinished is false, document returned : \n" + XMLUtils.prettyPrintDOM(jobContext.outputDocument));
            return jobContext.outputDocument;
        }
        
        // Job is finished
        Engine.logJobManager.debug("Job #" + jobID + " finished");
        jobs.remove(jobID);
        
        if (job.document == null) {
            throw new EngineException("The job has not been successfully finished! See the engine log file for more details.");
        }
        
        if (Engine.logJobManager.isTraceEnabled())
        	Engine.logJobManager.trace("JobManager abortJob: isFinished is true, document returned:\n" + XMLUtils.prettyPrintDOM(job.document));
        return job.document;
    }
    
    public static Document getJobStatus(String jobID) throws EngineException {
        Engine.logJobManager.debug("Requesting job #" + jobID + " status");
        Job job = (Job) jobs.get(jobID);
        
        if (job == null) {
            throw new EngineException("The requested job is not found (jobID=" + jobID + ").");
        }

        Document document = job.requester.createDOM("UTF-8");
        Engine.logJobManager.debug("Document created");                

        if (job.isFinished) {
            Engine.logJobManager.debug("Job #" + jobID + " finished");
            jobs.remove(jobID);
            
            if (job.document == null) {
                throw new EngineException("The job has not been successfully finished! See the engine log file for more details.");
            }
            
            if (Engine.logJobManager.isTraceEnabled())
            	Engine.logJobManager.trace("JobManager getJobStatus: isFinished is true, document returned:\n" + XMLUtils.prettyPrintDOM(job.document));
            return job.document;
        }
        else {
            Engine.logJobManager.debug("Job #" + jobID + " in progress, not ended.");
                
            Element xStatus = document.createElement("status");
            String servletPath = (String) job.context.servletPath;
            String refreshURL = servletPath.substring(servletPath.lastIndexOf('/') + 1) + "?"+Parameter.Async.getName()+"=true";
            String contextId = job.context.contextID;
            if (contextId.indexOf("_default") == -1) {
            	refreshURL = refreshURL + "&"+Parameter.Context.getName()+"=" + contextId.substring(contextId.indexOf("_")+1, contextId.length());
            }
            xStatus.setAttribute("refresh-url", refreshURL);
                
            Engine.logJobManager.debug("Refresh url computed");

            Element xJob = document.createElement("job");
            xJob.setAttribute("id", jobID);

            Engine.logJobManager.debug("Job element added");

            Vector<String> steps = (Vector<String>) job.context.steps;
            if (steps != null) {
                for (int i = 0 ; i < steps.size() ; i++) {
                    Element xStep = document.createElement("step");
                    xStep.appendChild(document.createTextNode((String) steps.elementAt(i)));
                    xJob.appendChild(xStep);
                }
                Engine.logJobManager.debug("Steps elements added");
            }

            xStatus.appendChild(xJob);
            document.appendChild(xStatus);
            Engine.logJobManager.debug("Status finished");
            
            if (Engine.logJobManager.isTraceEnabled())
            	Engine.logJobManager.trace("JobManager getJobStatus: isFinished is false, document returned : \n" + XMLUtils.prettyPrintDOM(document));
            return document;
        }
    }
}
