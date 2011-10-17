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

import java.util.SortedSet;
import java.util.TreeSet;

public class JobGroupJob extends AbstractJob {
	private SortedSet<AbstractJob> jobGroup = new TreeSet<AbstractJob>();
	private boolean serial = false;

	public SortedSet<AbstractJob> getJobGroup() {
		return jobGroup;
	}

	public void setJobGroup(SortedSet<AbstractJob> jobGroup) {
		this.jobGroup = jobGroup;
	}

	public void setSerial(boolean serial) {
		this.serial = serial;
	}

	public boolean isSerial() {
		return serial;
	}
	
	public boolean containsJob(AbstractJob job){
		return jobGroup.contains(job);
	}
	
	public boolean addJob(AbstractJob job){
		return jobGroup.add(job);
	}
	
	public boolean delJob(AbstractJob job){
		return jobGroup.remove(job);
	}
	
	public void delAllJobs(){
		jobGroup.clear();
	}
	
	public boolean checkNoRecurse(AbstractJob job){
		if(this.compareTo(job)==0)return false;
		for(AbstractJob subjob : jobGroup){
			if(subjob instanceof JobGroupJob)
				if(!((JobGroupJob)subjob).checkNoRecurse(job)) return false;
		}
		return true;
	}
}
