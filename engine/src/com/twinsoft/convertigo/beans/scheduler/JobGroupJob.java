/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

package com.twinsoft.convertigo.beans.scheduler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class JobGroupJob extends AbstractJob {
	private SortedSet<AbstractJob> jobGroup = new TreeSet<AbstractJob>();
	private int parallelJob = 1;

	public SortedSet<AbstractJob> getJobGroup() {
		return jobGroup;
	}

	public void setJobGroup(SortedSet<AbstractJob> jobGroup) {
		this.jobGroup = jobGroup;
	}

	public boolean isSerial() {
		return parallelJob == 1;
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
	
	public void checkProblems(List<String> problems) {
		super.checkProblems(problems);
		Set<JobGroupJob> all = new HashSet<>();
		if (checkNoRecurse(all)) {
			problems.add("jobGroup recursion detected");
		}
	}
	
	public boolean checkNoRecurse(Set<JobGroupJob> all) {
		boolean recurse = !all.add(this);
		if (!recurse) {
			for (AbstractJob job : jobGroup) {
				if (job instanceof JobGroupJob && !recurse) {
					recurse = ((JobGroupJob) job).checkNoRecurse(all);
				}
			}
		}
		return recurse;
	}

	public int getParallelJob() {
		return parallelJob;
	}

	public void setParallelJob(int parallelJob) {
		this.parallelJob = parallelJob;
	}

	public void setSerial(boolean serial) {
		this.parallelJob = serial ? 1 : Runtime.getRuntime().availableProcessors();
	}
}
