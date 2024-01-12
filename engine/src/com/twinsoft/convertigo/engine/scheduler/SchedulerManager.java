/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;

import com.twinsoft.convertigo.beans.scheduler.AbstractSchedule;
import com.twinsoft.convertigo.beans.scheduler.ScheduleCron;
import com.twinsoft.convertigo.beans.scheduler.ScheduleRunNow;
import com.twinsoft.convertigo.beans.scheduler.ScheduledJob;
import com.twinsoft.convertigo.beans.scheduler.SchedulerXML;
import com.twinsoft.convertigo.engine.Engine;

public class SchedulerManager {
	private boolean schedulerOn = true;
	private boolean paused = false;
	private Scheduler sched;
	private SchedulerXML schedulerXML;
	private String counterName = "a";

	public SchedulerManager(boolean serverMode) {
		schedulerOn = serverMode;
		if (serverMode) {
			init();
			refreshJobs();
		}
	}

	private void init() {
		SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
		try {
			sched = schedFact.getScheduler();
			sched.start();
			try {
				load();
			} catch (Exception e) {
				schedulerXML = new SchedulerXML();
			}
		} catch (Exception e) {
			schedulerOn = false;
			Engine.logEngine.error("Unexpected exception", e);
		}
	}
	
	public void load() throws FileNotFoundException {
		try (XMLDecoder decoder = new XMLDecoder(new FileInputStream(getFileURL()))) {
			schedulerXML = (SchedulerXML) decoder.readObject();
		}
	}
	
	private String nextCounterName() {
		boolean more = true;
		StringBuffer sb = new StringBuffer();
		for (int i = 0 ; i < counterName.length() ; i++) {
			char c = counterName.charAt(i);
			if (more) {
				more = false;
				switch(c) {
				case 'Z' : c = 'a'; more = true; break;
				case 'z' : c = 'A'; break;
				default  : c = (char) (((int) c) + 1); break;
				}
			}
			sb.append(c);
		}
		if (more) {
			sb.append('a');
		}
		String res = counterName;
		counterName = sb.toString();
		return res;
	}

	public boolean isSchedulerOn() {
		return schedulerOn;
	}

	public void pause() {
		if (schedulerOn && !paused) {
			try {
				sched.pauseAll();
				paused = true;
			} catch (SchedulerException e) {
				schedulerOn = false;
				Engine.logEngine.error("Unexpected exception", e);
			}
		}
	}

	public void resume() {
		if (schedulerOn && paused) {
			try {
				sched.resumeAll();
				paused = false;
			} catch (SchedulerException e) {
				schedulerOn = false;
				Engine.logEngine.error("Unexpected exception", e);
			}
		}
	}

	public boolean isPaused() {
		return paused;
	}

	public void destroy() {
		if (schedulerOn) {
			try {
				sched.shutdown();
			} catch (SchedulerException e) {
				Engine.logEngine.error("Unexpected exception", e);
			} finally {
				schedulerOn = false;
			}
		}
	}

	public void refreshJobs() {
		if (schedulerOn) {
			try {
				Engine.logEngine.debug("(Scheduler Manager) refresh jobs start");
				Set<JobKey> jobs = sched.getJobKeys(GroupMatcher.jobGroupEquals(Scheduler.DEFAULT_GROUP));
				for (JobKey job: jobs) {
					Engine.logEngine.trace("(Scheduler Manager) Delete " + job.getName() + " ...");
					boolean ok = sched.deleteJob(job);
					Engine.logEngine.trace("(Scheduler Manager) ... " + job.getName() + " deleted ? " + ok);
					if (!ok) {
						Engine.logEngine.debug("(Scheduler Manager) Job " + job.getName() + " not deleted for refresh !");
					}
				}
				
				boolean shouldSave = false;
				
				for (ScheduledJob scheduledJob : schedulerXML.getScheduledJobs()) {
					if (scheduledJob.isAllEnabled()) {
						String currentName = nextCounterName() + "[" + scheduledJob.getName() + "]";
						
						JobDetail jd = JobBuilder.newJob().withIdentity(currentName).ofType(SchedulerJob.class).build();
						jd.getJobDataMap().put("scheduledJob", scheduledJob);
						jd.getJobDataMap().put("schedulerManager", this);
						
						AbstractSchedule abstractSchedule = scheduledJob.getSchedule();
						
						if (abstractSchedule != null) {
							Trigger tg = null;
							
							if (abstractSchedule instanceof ScheduleCron) {
								ScheduleCron scheduleCron = (ScheduleCron) abstractSchedule;
								try {
									tg = TriggerBuilder.newTrigger().withIdentity(currentName).withSchedule(CronScheduleBuilder.cronSchedule(scheduleCron.getCron())).build();
								} catch (Exception e) {
									e.printStackTrace();
								}
							} else if (abstractSchedule instanceof ScheduleRunNow) {
								tg = TriggerBuilder.newTrigger().withIdentity(currentName).startNow().build();
								scheduledJob.setEnable(false);
								shouldSave = true;
							}
								
							if (tg != null) {
								try {
									sched.scheduleJob(jd, tg);
									Engine.logEngine.trace("(Scheduler Manager) " + currentName + " scheduled");
								} catch (SchedulerException e) {
									String message = "(Scheduler Manager) " + currentName + " failed to be scheduled: " + e.getMessage();
									if (message != null && message.contains("trigger will never fire")) {
										Engine.logEngine.debug(message);
									} else {
										Engine.logEngine.warn(message);
									}
								}
							}
						}
					}
				}
				if (shouldSave) {
					save();
				}
				Engine.logEngine.debug("(Scheduler Manager) refresh jobs finished");
			} catch (SchedulerException e) {
				Engine.logEngine.error("(Scheduler Manager) refresh jobs failed !", e);
			}
		}
	}
	
	public SortedSet<ScheduledJob> getRunningScheduledJobs() {
		SortedSet<ScheduledJob> ss = new TreeSet<ScheduledJob>();
		try {
			for (JobExecutionContext ctx : (List<JobExecutionContext>) sched.getCurrentlyExecutingJobs()) {
				JobDetail jd = ctx.getJobDetail();
				if (jd.getJobDataMap().containsKey("running")) {
					ss.add((ScheduledJob) jd.getJobDataMap().get("scheduledJob"));
				}
			}
		} catch (SchedulerException e) { }
		return ss;
	}
	
	private String getFileURL() {
		return Engine.CONFIGURATION_PATH + "/scheduler.xml";
	}
	
	public void save() {
		try (XMLEncoder encoder = new XMLEncoder(new FileOutputStream(getFileURL()))) {
			Engine.logEngine.debug("(Scheduler Manager) Start jobs saving ...");
			encoder.writeObject(schedulerXML);
		} catch (FileNotFoundException e) {
			Engine.logEngine.error("(Scheduler Manager) ... jobs saving failed !", e);
		}
		
		try {
			load();
			Engine.logEngine.debug("(Scheduler Manager) ... jobs saving finished !");
		} catch (FileNotFoundException e) {
			Engine.logEngine.error("(Scheduler Manager) ... jobs reloading failed !", e);
		}
	}
	
	public SchedulerXML getSchedulerXML() {
		return schedulerXML;
	}
}
