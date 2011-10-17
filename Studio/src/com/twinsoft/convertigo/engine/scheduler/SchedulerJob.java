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

package com.twinsoft.convertigo.engine.scheduler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.twinsoft.convertigo.beans.scheduler.AbstractConvertigoJob;
import com.twinsoft.convertigo.beans.scheduler.AbstractJob;
import com.twinsoft.convertigo.beans.scheduler.JobGroupJob;
import com.twinsoft.convertigo.beans.scheduler.ScheduledJob;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class SchedulerJob implements Job {
	static Pattern encodingPattern = Pattern.compile(".*encoding=\"([^\"]*)\".*");;

	public void execute(JobExecutionContext context) throws JobExecutionException {
		JobDetail jd = context.getJobDetail();
		ScheduledJob scheduledJob = (ScheduledJob) jd.getJobDataMap().get("scheduledJob");
		SchedulerManager schedulerManager = (SchedulerManager) jd.getJobDataMap().get("schedulerManager");
		AbstractJob job = scheduledJob.getJob();
		
		if (schedulerManager.getRunningScheduledJobs().contains(scheduledJob)) {
			Engine.logScheduler.warn("No start " + jd.getName() + " because another still running.");
		} else {
			jd.getJobDataMap().put("running", true);
			executeJob(job, jd.getName());
		}
	}
	
	public void executeJob(AbstractJob job, String jdName) {
		if (job.isEnable()) {
			long start = System.currentTimeMillis();
			if (job instanceof AbstractConvertigoJob) {
				AbstractConvertigoJob convertigoJob = (AbstractConvertigoJob) job;
				
				String targetUrl = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);
				if (targetUrl.endsWith("/")) {
					targetUrl = targetUrl.substring(targetUrl.length() - 1, targetUrl.length());
				}
				targetUrl += convertigoJob.getConvertigoURL();
				
				Engine.logScheduler.info("Prepare job " + jdName + " for " + targetUrl);
	
				PostMethod method = new PostMethod(targetUrl);
				method.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
	
				// Tells the method to automatically handle authentication.
				method.setDoAuthentication(true);
	
				// Tells the method to automatically handle redirection.
				method.setFollowRedirects(false);
				try {
					int statuscode = Engine.theApp.httpClient.executeMethod(HostConfiguration.ANY_HOST_CONFIGURATION,method, new HttpState());
					String message = "Completed job " + jdName + " with status " + statuscode;
					if (convertigoJob.isWriteOutput()) {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						IOUtils.copy(method.getResponseBodyAsStream(), baos);
						byte[] data = baos.toByteArray();
						int len = Math.min(data.length, 128);
						String encoding = "UTF-8";
						Matcher encodingMatcher = encodingPattern.matcher(new String (data, 0, len, "ASCII"));
						if (encodingMatcher.find()) {
							encoding = encodingMatcher.group(1);
						}
						try {
							message += '\n' + new String(data, encoding);
						} catch (UnsupportedEncodingException e) {
							message += '\n' + new String(data, "ASCII");
						}
					}
					Engine.logScheduler.info(message);
				} catch (HttpException e) {
					Engine.logScheduler.error("Failed job " + jdName, e);
				} catch (IOException e) {
					Engine.logScheduler.error("Failed job " + jdName, e);
				} finally {
					method.releaseConnection();
				}
				JobGroupJob jobGroupJob = (JobGroupJob) job;
				
				SortedSet<AbstractJob> jobs = jobGroupJob.getJobGroup();
				
				Engine.logScheduler.info("Prepare job " + jdName + " for " + jobs.size() + " jobs. Serial ? " + jobGroupJob.isSerial());
				
				if (jobGroupJob.isSerial()) {
					for (AbstractJob abstractJob : jobs) {
						executeJob(abstractJob, jdName + "[" + abstractJob.getName() + "]");
					}
				} else {
					Set<Thread> threads = new HashSet<Thread>();
					for (final AbstractJob abstractJob : jobs) {
						final String subname = jdName + "[" + abstractJob.getName() + "]";
						Thread thread = new Thread(new Runnable() {
							public void run() {
								executeJob(abstractJob, subname);
							}
						});
						threads.add(thread);
						thread.start();
					}
					for (Thread thread : threads) {
						try {
							thread.join();
						} catch (InterruptedException e) {
							Engine.logScheduler.error("Unexpected exception", e);
						}
					}
				}
			}
			Engine.logScheduler.info("Completed job " + jdName + " in " + (System.currentTimeMillis() - start) + "ms");
		} else {
			Engine.logScheduler.info("Trying to start " + jdName + " failed because the job is disabled !");
		}
	}
}