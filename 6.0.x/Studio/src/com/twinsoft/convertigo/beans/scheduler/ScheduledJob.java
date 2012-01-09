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

import java.util.List;

public class ScheduledJob extends AbstractBase {
	public static final String prob_noJob = "the job cannot be empty";
	public static final String prob_noSchedule = "the schedule cannot be empty";
	
	private AbstractJob job = null;
	private AbstractSchedule schedule = null;
	
	public AbstractJob getJob() {
		return job;
	}
	
	public void setJob(AbstractJob job) {
		this.job = job;
	}
	
	public AbstractSchedule getSchedule() {
		return schedule;
	}
	
	public void setSchedule(AbstractSchedule abstractSchedule) {
		this.schedule = abstractSchedule;
	}
	
	@Override
	public String getName() {
		String jobName = (job == null ? "..." : job.getName());
		String scheduleName = (schedule == null ? "..." : schedule.getName());
		return jobName + "@" + scheduleName;
	}
	
	public boolean isAllEnabled() {
		return isEnable() && job != null && schedule != null && job.isEnable() && schedule.isEnable();
	}
	
	@Override
	public void checkProblems(List<String> problems) {
		super.checkProblems(problems);
		if (job == null) {
			problems.add(prob_noJob);
		}
		if (schedule == null){
			problems.add(prob_noSchedule);
		}
	}
}
