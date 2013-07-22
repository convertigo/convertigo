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

package com.twinsoft.convertigo.engine.admin.services.scheduler;

import java.util.ArrayList;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.scheduler.AbstractBase;
import com.twinsoft.convertigo.beans.scheduler.AbstractConvertigoJob;
import com.twinsoft.convertigo.beans.scheduler.AbstractJob;
import com.twinsoft.convertigo.beans.scheduler.AbstractSchedule;
import com.twinsoft.convertigo.beans.scheduler.JobGroupJob;
import com.twinsoft.convertigo.beans.scheduler.ScheduleCron;
import com.twinsoft.convertigo.beans.scheduler.ScheduleRunNow;
import com.twinsoft.convertigo.beans.scheduler.ScheduledJob;
import com.twinsoft.convertigo.beans.scheduler.SchedulerXML;
import com.twinsoft.convertigo.beans.scheduler.SequenceConvertigoJob;
import com.twinsoft.convertigo.beans.scheduler.TransactionConvertigoJob;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.scheduler.SchedulerManager;

@ServiceDefinition(
		name = "List",
		roles = { Role.WEB_ADMIN },
		parameters = {},
		returnValue = ""
	)
public class List extends XmlService{
	private enum Category {
		scheduledJobs,
		jobs,
		schedules
	}
	
	protected Element prepareElement(Document doc, Category category, AbstractBase abstractBase) {
		Element element = doc.createElement("element");
		element.setAttribute("type", abstractBase.getClass().getSimpleName());
		element.setAttribute("enabled", Boolean.toString(abstractBase.isEnable()));
		element.setAttribute("name", abstractBase.getName());
		element.setAttribute("description", abstractBase.getDescription());
		element.setAttribute("category", category.toString());
		return element;
	}
	
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Category category = null;
		try {
			category = Category.valueOf(request.getParameter("category"));
		} catch (Exception e) {
			// just ignore parse exception
		}
		
		Element rootElement = document.getDocumentElement();
        
		SchedulerManager schedulerManager = Engine.theApp.schedulerManager;
		SchedulerXML schedulerXML = schedulerManager.getSchedulerXML();
		
		if (category == null || category == Category.scheduledJobs) {
			for (ScheduledJob sj : schedulerXML.getScheduledJobs()) {				
				Element element = prepareElement(document, Category.scheduledJobs, sj);
				AbstractJob job = sj.getJob();
				AbstractSchedule schedule = sj.getSchedule();
				element.setAttribute("jobName", job == null ? "..." : job.getName());
				element.setAttribute("scheduleName", schedule == null ? "..." : schedule.getName());
				
				StringBuffer info = new StringBuffer();
				if (schedulerManager.getRunningScheduledJobs().contains(sj)) {
					info.append("[ currently running ] ");
				}
				if (sj.isAllEnabled()) {
					info.append("[ ready to run ] ");	
				} else {
					info.append("[ not ready to run ] ");
					if (sj.isEnable()) {
						info.append("caused by ");
						if (job == null) {
							info.append("[ job is not defined ]");
						} else if (!job.isEnable()) {
							info.append("[ job is disabled ]");
						}
						if (schedule == null) {
							info.append("[ schedule is not defined ]");
						} else if (!schedule.isEnable()) {
							info.append("[ schedule is disabled ]");
						}
					}
				}
				element.setAttribute("info", info.toString());
				
				rootElement.appendChild(element);
			}
		}
		
		if (category == null || category == Category.jobs) {
			for (AbstractJob aj : schedulerXML.getJobs()) {
				Element element = prepareElement(document, Category.jobs, aj);
				
				if (aj instanceof AbstractConvertigoJob) {
					AbstractConvertigoJob acj = (AbstractConvertigoJob) aj;
					element.setAttribute("writeOutput", Boolean.toString(acj.isWriteOutput()));
					element.setAttribute("project", acj.getProjectName());
					element.setAttribute("context", acj.getContextName());
					element.setAttribute("info", "URL : " + acj.getConvertigoURL());
		            for (Map.Entry<String, String> entry : acj.getParameters().entrySet()){
		            	Element parameter = document.createElement("parameter");
		            	parameter.setAttribute("name", entry.getKey());
		            	parameter.setTextContent(entry.getValue());
		            	element.appendChild(parameter);	              
		            }
		            if (acj instanceof SequenceConvertigoJob) {
		            	SequenceConvertigoJob scj = (SequenceConvertigoJob) acj;
		            	element.setAttribute("sequence", scj.getSequenceName());
		            } else if (acj instanceof TransactionConvertigoJob) {
		            	TransactionConvertigoJob tcj = (TransactionConvertigoJob) acj;
		            	element.setAttribute("connector", ((TransactionConvertigoJob) acj).getConnectorName());
		            	element.setAttribute("transaction", tcj.getTransactionName());
		            }
				} else if (aj instanceof JobGroupJob) {
					JobGroupJob jgj = (JobGroupJob) aj;
					element.setAttribute("serial", Boolean.toString(jgj.isSerial()));
					
					for (AbstractJob sub : jgj.getJobGroup()) {
						Element job = document.createElement("job_group_member");
						job.appendChild(document.createTextNode(sub.getName()));
						element.appendChild(job);
					}
					element.setAttribute("info", (jgj.isSerial() ? "serial " : "parallel ") + new ArrayList<AbstractJob>(jgj.getJobGroup()).toString());
				}
				
				rootElement.appendChild(element);
			}
		}
		
		if (category == null || category == Category.schedules) {
			for (AbstractSchedule as : schedulerXML.getSchedules()) {
				Element element = prepareElement(document, Category.schedules, as);
				
				if (as instanceof ScheduleCron) {
					ScheduleCron sc = (ScheduleCron) as;
					element.setAttribute("cron", sc.getCron());
					element.setAttribute("info", "CRON [" + sc.getCron() + "]");
				}
				
				if (as instanceof ScheduleRunNow) {
					element.setAttribute("info", "RunNow");
				}
				
				rootElement.appendChild(element);
			}
		}
	}
}