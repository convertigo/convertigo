/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine.scheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import javax.servlet.http.HttpServletRequest;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.scheduler.AbstractConvertigoJob;
import com.twinsoft.convertigo.beans.scheduler.AbstractJob;
import com.twinsoft.convertigo.beans.scheduler.JobGroupJob;
import com.twinsoft.convertigo.beans.scheduler.ScheduledJob;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.requesters.HttpSessionListener;
import com.twinsoft.convertigo.engine.requesters.InternalRequester;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class SchedulerJob implements Job {
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		JobDetail jd = context.getJobDetail();
		ScheduledJob scheduledJob = (ScheduledJob) jd.getJobDataMap().get("scheduledJob");
		SchedulerManager schedulerManager = (SchedulerManager) jd.getJobDataMap().get("schedulerManager");
		AbstractJob job = scheduledJob.getJob();
		if (job != null) {
			if (schedulerManager.getRunningScheduledJobs().contains(scheduledJob)) {
				Engine.logScheduler.warn("No start " + jd.getKey().getName() + " because another still running.");
			} else {
				jd.getJobDataMap().put("running", true);
				executeJob(job, jd.getKey().getName());
			}
		}
	}

	private void executeJob(AbstractJob job, String jdName) {
		if (job.isEnable()) {
			long start = System.currentTimeMillis();
			if (job instanceof AbstractConvertigoJob) {
				AbstractConvertigoJob convertigoJob = (AbstractConvertigoJob) job;
				HttpServletRequest request = null;
				try {
					Engine.logScheduler.info("Prepare job " + jdName + " for " + convertigoJob.getProjectName());
					
					Map<String, String[]> parameters = convertigoJob.getConvertigoParameters();
					InternalRequester requester = new InternalRequester(GenericUtils.<Map<String, Object>>cast(parameters));
					HttpSessionListener.checkSession(request = requester.getHttpServletRequest());
					Object response = requester.processRequest();
					String message = "Completed job " + jdName + " with success";
					if (convertigoJob.isWriteOutput()) {
						if (response instanceof Document) {
							response = XMLUtils.prettyPrintDOM((Document) response);
						}
						message += "\n" + response;
					}
					Engine.logScheduler.info(message);
				} catch (Exception e) {
					Engine.logScheduler.error("Failed job " + jdName, e);
				} finally {
					if (request != null) {
						request.getSession(true).invalidate();
					}
				}
			} else if (job instanceof JobGroupJob) {
				JobGroupJob jobGroupJob = (JobGroupJob) job;

				SortedSet<AbstractJob> jobs = jobGroupJob.getJobGroup();

				Engine.logScheduler.info("Prepare job " + jdName + " for " + jobs.size() + " jobs. Serial ? " + jobGroupJob.isSerial());
				int parallelJob = jobGroupJob.getParallelJob();
				if (parallelJob <= 1) {
					for (AbstractJob abstractJob : jobs) {
						executeJob(abstractJob, jdName + "[" + abstractJob.getName() + "]");
					}
				} else {
					int[] jobCount = {0};
					Set<Thread> threads = new HashSet<>();
					List<AbstractJob> list = new ArrayList<>(jobs);
					while (!list.isEmpty()) {
						synchronized (jobCount) {
							if (jobCount[0] == parallelJob) {
								try {
									jobCount.wait();
								} catch (InterruptedException e) {
								}
							}
							jobCount[0]++;
							AbstractJob abstractJob = list.remove(0);
							final String subname = jdName + "[" + abstractJob.getName() + "]";
							Thread thread = new Thread(() -> {
								try {
									executeJob(abstractJob, subname);
								} finally {
									synchronized (jobCount) {
										jobCount[0]--;
										jobCount.notify();
									}
								}
							});
							threads.add(thread);
							thread.setDaemon(true);
							thread.start();
						}
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