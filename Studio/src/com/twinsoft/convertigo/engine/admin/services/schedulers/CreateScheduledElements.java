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

package com.twinsoft.convertigo.engine.admin.services.schedulers;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;
import com.twinsoft.convertigo.engine.scheduler.SchedulerManager;
import com.twinsoft.convertigo.engine.util.GenericUtils;

@ServiceDefinition(name = "CreateScheduledElements", roles = { Role.WEB_ADMIN }, parameters = {}, returnValue = "")
public class CreateScheduledElements extends XmlService {
	static private Pattern prefixPattern = Pattern.compile("requestable_parameter_(.*)");
	
	enum Type {
		schedulersNewScheduledJob (ScheduledJob.class),
		schedulersNewTransactionConvertigoJob (TransactionConvertigoJob.class),
		schedulersNewSequenceConvertigoJob (SequenceConvertigoJob.class),
		schedulersNewJobGroupJob (JobGroupJob.class),
		schedulersNewScheduleCron (ScheduleCron.class),
		schedulersNewScheduleRunNow (ScheduleRunNow.class);
		
		Class<? extends AbstractBase> c;
		
		Type(Class<? extends AbstractBase> c) {
			this.c = c;
		}
	}

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		SchedulerManager schedulerManager = Engine.theApp.schedulerManager;
		SchedulerXML schedulerXML = schedulerManager.getSchedulerXML();
		
		boolean edit = (request.getParameter("edit") != null);
		boolean del = (request.getParameter("del") != null);
		
		Type type = null;
		try {
			type = Type.valueOf(request.getParameter("type"));
		} catch (Exception e) {
			throw new ServiceException("invalide \"type\" parameter");
		}

		Element rootElement = document.getDocumentElement();
		AbstractBase ab = null;
		
		if (edit || del) {
			String exname = ServiceUtils.getRequiredParameter(request, "exname");
			if (ScheduledJob.class.isAssignableFrom(type.c)) {
				ab = schedulerXML.getScheduledJob(exname);
			} else if (AbstractSchedule.class.isAssignableFrom(type.c)) {
				ab = schedulerXML.getSchedule(exname);
			} else if (AbstractJob.class.isAssignableFrom(type.c)) {
				ab = schedulerXML.getJob(exname);
			}
			
			if (del) {
				schedulerXML.delAbstractBase(ab);
				ab = null;
			}
		} else {
			ab = type.c.newInstance();
		}
		
		if (ab != null) {
			ab.setName(ServiceUtils.getRequiredParameter(request, "name"));
			ab.setDescription(ServiceUtils.getRequiredParameter(request, "description"));
			ab.setEnable("true".equals(ServiceUtils.getRequiredParameter(request, "enabled")));
			
			if (ScheduledJob.class.isAssignableFrom(type.c)) {
				ScheduledJob sj = (ScheduledJob) ab;
				sj.setJob(schedulerXML.getJob(ServiceUtils.getRequiredParameter(request, "jobName")));
				sj.setSchedule(schedulerXML.getSchedule(ServiceUtils.getRequiredParameter(request, "scheduleName")));
			} else if (AbstractSchedule.class.isAssignableFrom(type.c)) {
				AbstractSchedule as = (AbstractSchedule) ab;
				if (ScheduleCron.class.isAssignableFrom(type.c)) {
					ScheduleCron sc = (ScheduleCron) as;
					sc.setCron(ServiceUtils.getRequiredParameter(request, "cron"));
				}
			} else if (AbstractJob.class.isAssignableFrom(type.c)) {
				AbstractJob aj = (AbstractJob) ab;
				if (JobGroupJob.class.isAssignableFrom(type.c)) {
					JobGroupJob jgj = (JobGroupJob) aj;
					jgj.setSerial("true".equals(ServiceUtils.getRequiredParameter(request, "serial")));
					jgj.delAllJobs();
					
					for (String jobname : request.getParameterValues("jobsname")) {
						AbstractJob jobToAdd = schedulerXML.getJob(jobname);
						if (jgj.checkNoRecurse(jobToAdd)) {
							jgj.addJob(jobToAdd);
						}
					}
				} else if (AbstractConvertigoJob.class.isAssignableFrom(type.c)) {
					AbstractConvertigoJob acj = (AbstractConvertigoJob) aj;
					acj.setContextName(ServiceUtils.getRequiredParameter(request, "context"));
					acj.setProjectName(ServiceUtils.getRequiredParameter(request, "project"));
					acj.setWriteOutput("true".equals(ServiceUtils.getRequiredParameter(request, "writeOutput")));
					
					
					
					if (TransactionConvertigoJob.class.isAssignableFrom(type.c)) {
						TransactionConvertigoJob tcj = (TransactionConvertigoJob) acj;
						tcj.setConnectorName(ServiceUtils.getParameter(request, "connector", ""));
						tcj.setTransactionName(ServiceUtils.getParameter(request, "transaction", ""));
					} else if (SequenceConvertigoJob.class.isAssignableFrom(type.c)) {
						SequenceConvertigoJob scj = (SequenceConvertigoJob) acj;
						scj.setSequenceName(ServiceUtils.getParameter(request, "sequence", ""));
					}
					
					Map<String, String> parameters = new HashMap<String, String>();					
					Matcher prefix = prefixPattern.matcher("");
					for (String pname : Collections.list(GenericUtils.<Enumeration<String>>cast(request.getParameterNames()))) {
						prefix.reset(pname);
						if (prefix.find()) {
							parameters.put(prefix.group(1), request.getParameter(pname));
						}
					}	
					
					acj.setParameters(parameters);
				}
			}
			
			List<String> problems = schedulerXML.checkProblems(ab);
			
			if (edit) {
				problems.remove(SchedulerXML.prob_alreadyExist);
			}
			
			if (problems.size() > 0) {
				for (String problem : problems) {
					rootElement.appendChild(document.createElement("problem")).appendChild(document.createTextNode(problem));
				}
			} else {
				if (!edit) {
					schedulerXML.addAbstractBase(ab);
				}
			}
		}
		
		schedulerManager.save();
		schedulerManager.refreshJobs();
	}
}