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

import org.quartz.CronTrigger;
import org.quartz.Scheduler;

public class ScheduleCron extends AbstractSchedule {
	public static final String prob_cronSyntax = "the cron value must respect the CRON syntax";
	
	private String cron = "0 0 0 * * ?";

	public String getCron() {
		return cron;
	}

	public void setCron(String cron) {
		this.cron = cron;
	}
	
	@Override
	public void checkProblems(List<String> problems) {
		super.checkProblems(problems);
		try {
			new CronTrigger("0", Scheduler.DEFAULT_GROUP, cron);
		} catch (Exception e) {
			problems.add(prob_cronSyntax);
			problems.add("###[" + e.getMessage() + "]");
		}
	}
}
