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

package com.twinsoft.convertigo.beans.scheduler;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class SchedulerXML {
	public static final String prob_alreadyExist = "the name is already used";
	
	private SortedSet<AbstractJob> jobs = new TreeSet<AbstractJob>();
	private SortedSet<AbstractSchedule> schedules = new TreeSet<AbstractSchedule>();
	private SortedSet<ScheduledJob> scheduledjobs = new TreeSet<ScheduledJob>();

	public SortedSet<AbstractJob> getJobs() {
		return jobs;
	}

	public void setJobs(SortedSet<AbstractJob> jobs) {
		this.jobs = jobs;
	}

	public SortedSet<AbstractSchedule> getSchedules() {
		return schedules;
	}

	public void setSchedules(SortedSet<AbstractSchedule> schedules) {
		this.schedules = schedules;
	}

	public boolean addJob(AbstractJob job) {
		return jobs.add(job);
	}
	
	public boolean delJob(AbstractJob job) {
		scheduledjobs.removeAll(getScheduledJobsForJob(job));
		for (AbstractJob ijob : jobs) {
			if (ijob instanceof JobGroupJob) {
				((JobGroupJob) ijob).delJob(job);
			}
		}
		return jobs.remove(job);
	}

	public boolean addSchedule(AbstractSchedule sched) {
		return schedules.add(sched);
	}

	public boolean delSchedule(AbstractSchedule sched) {
		scheduledjobs.removeAll(getScheduledJobsForSchedule(sched));
		return schedules.remove(sched);
	}
	
	public SortedSet<ScheduledJob> getScheduledJobs() {
		return scheduledjobs;
	}

	public void setScheduledJobs(SortedSet<ScheduledJob> scheduledjobs) {
		this.scheduledjobs = scheduledjobs;
	}
	
	public boolean addScheduledJob(ScheduledJob sj) {
		return scheduledjobs.add(sj);
	}

	public boolean delScheduledJob(ScheduledJob sj) {
		return scheduledjobs.remove(sj);
	}
	
	private <AB extends AbstractBase> AB getAbstractBase(SortedSet<AB> set, String name) {
		for (AB res : set) {
			if (res.getName().equals(name)) {
				return res;
			}
		}
		return null;
	}
	
	public AbstractJob getJob(String name) {
		return getAbstractBase(jobs, name);
	}
	
	public AbstractSchedule getSchedule(String name) {
		return (AbstractSchedule) getAbstractBase(schedules, name);
	}
	
	public ScheduledJob getScheduledJob(String name) {
		return (ScheduledJob) getAbstractBase(scheduledjobs, name);
	}
	
	public SortedSet<ScheduledJob> getScheduledJobsForJob(AbstractJob job) {
		SortedSet<ScheduledJob> ss = new TreeSet<ScheduledJob>();
		for (ScheduledJob scheduledJob : scheduledjobs) {
			if (job.equals(scheduledJob.getJob())) {
				ss.add(scheduledJob);
			}
		}
		return ss;
	}
	
	public SortedSet<ScheduledJob> getScheduledJobsForSchedule(AbstractSchedule job) {
		SortedSet<ScheduledJob> ss = new TreeSet<ScheduledJob>();
		for (ScheduledJob scheduledJob : scheduledjobs) {
			if (job.equals(scheduledJob.getSchedule())) {
				ss.add(scheduledJob);
			}
		}
		return ss;
	}
	
	public List<String> checkProblems(AbstractBase base) {
		AbstractBase exist = null;
		List<String> problems = new LinkedList<String>();
		base.checkProblems(problems);
		exist = (base instanceof AbstractJob) ? (AbstractBase) getJob(base.getName()) :
			(base instanceof AbstractSchedule) ? (AbstractBase) getSchedule(base.getName()) :
			(base instanceof ScheduledJob) ? (AbstractBase) getScheduledJob(base.getName()) : null;
		if (exist != null) {
			problems.add(prob_alreadyExist);
		}
		return problems;
	}
	
	public boolean addAbstractBase(AbstractBase base) {
		return (base instanceof AbstractJob) ? addJob((AbstractJob) base) :
			(base instanceof AbstractSchedule) ? addSchedule((AbstractSchedule) base) :
			(base instanceof ScheduledJob) ? addScheduledJob((ScheduledJob) base) : false;
	}
	
	public boolean delAbstractBase(AbstractBase base) {
		return (base instanceof AbstractJob) ? delJob((AbstractJob) base) :
			(base instanceof AbstractSchedule) ? delSchedule((AbstractSchedule) base) :
			(base instanceof ScheduledJob) ? delScheduledJob((ScheduledJob) base) : false;
	}
}