/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.scheduler;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.scheduler.AbstractJob;
import com.twinsoft.convertigo.beans.scheduler.AbstractSchedule;
import com.twinsoft.convertigo.beans.scheduler.JobGroupJob;
import com.twinsoft.convertigo.beans.scheduler.ScheduledJob;
import com.twinsoft.convertigo.beans.scheduler.SchedulerXML;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.scheduler.SchedulerManager;

@ServiceDefinition(
	name = "Export",
	roles = { Role.WEB_ADMIN, Role.SCHEDULER_CONFIG, Role.SCHEDULER_VIEW },
	parameters = {},
	returnValue = "return the selected scheduler elements as a scheduler.xml file"
)
public class Export extends DownloadService {

	@Override
	protected void writeResponseResult(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String elements = request.getParameter("elements");
		if (StringUtils.isBlank(elements)) {
			throw new ServiceException("The scheduler element selection is missing");
		}

		SchedulerXML source = Engine.theApp.schedulerManager.getSchedulerXML();
		SchedulerXML exported = new SchedulerXML();
		JSONArray selection = new JSONArray(elements);
		Set<String> includedJobs = new HashSet<String>();

		for (int i = 0; i < selection.length(); i++) {
			JSONObject selected = selection.getJSONObject(i);
			String category = selected.getString("category");
			String name = selected.getString("name");

			if ("jobs".equals(category)) {
				includeJob(source.getJob(name), exported, includedJobs);
			} else if ("schedules".equals(category)) {
				includeSchedule(source.getSchedule(name), exported);
			} else if ("scheduledJobs".equals(category)) {
				ScheduledJob scheduledJob = source.getScheduledJob(name);
				if (scheduledJob != null) {
					exported.addScheduledJob(scheduledJob);
					includeJob(scheduledJob.getJob(), exported, includedJobs);
					includeSchedule(scheduledJob.getSchedule(), exported);
				}
			} else {
				throw new ServiceException("Invalid scheduler element category: " + category);
			}
		}

		if (exported.getJobs().isEmpty() && exported.getSchedules().isEmpty()
				&& exported.getScheduledJobs().isEmpty()) {
			throw new ServiceException("No scheduler element was selected for export");
		}

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		SchedulerManager.writeSchedulerXML(exported, outputStream);

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		String fileName = "scheduler_" + dateFormat.format(new Date()) + ".xml";
		HeaderName.ContentDisposition.setHeader(response, "attachment; filename=\"" + fileName + "\"");
		response.setContentType(MimeType.Xml.value());
		response.setCharacterEncoding("UTF-8");
		response.getOutputStream().write(outputStream.toByteArray());

		Engine.logAdmin.info("The scheduler configuration has been exported.");
	}

	private void includeJob(AbstractJob job, SchedulerXML exported, Set<String> includedJobs) {
		if (job == null || !includedJobs.add(job.getName())) {
			return;
		}

		exported.addJob(job);
		if (job instanceof JobGroupJob) {
			for (AbstractJob member : ((JobGroupJob) job).getJobGroup()) {
				includeJob(member, exported, includedJobs);
			}
		}
	}

	private void includeSchedule(AbstractSchedule schedule, SchedulerXML exported) {
		if (schedule != null) {
			exported.addSchedule(schedule);
		}
	}
}
