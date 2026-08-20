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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import javax.xml.parsers.DocumentBuilderFactory;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.scheduler.AbstractBase;
import com.twinsoft.convertigo.beans.scheduler.AbstractJob;
import com.twinsoft.convertigo.beans.scheduler.AbstractSchedule;
import com.twinsoft.convertigo.beans.scheduler.JobGroupJob;
import com.twinsoft.convertigo.beans.scheduler.ScheduledJob;
import com.twinsoft.convertigo.beans.scheduler.SchedulerXML;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.UploadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;
import com.twinsoft.convertigo.engine.scheduler.SchedulerManager;

@ServiceDefinition(
	name = "Import",
	roles = { Role.WEB_ADMIN, Role.SCHEDULER_CONFIG },
	parameters = {},
	returnValue = ""
)
public class Import extends UploadService {
	private static final Set<String> ALLOWED_ELEMENTS = new HashSet<String>(Arrays.asList(
		"java", "object", "void", "array", "string", "boolean", "byte", "char", "double", "float",
		"int", "long", "null", "short"
	));
	private static final Set<String> ALLOWED_METHODS = new HashSet<String>(Arrays.asList(
		"add", "put", "getConnectorName", "getContextName", "getCron", "getDescription", "getJob",
		"getJobGroup", "getJobs", "getName", "getParallelJob", "getParameters", "getProjectName",
		"getSchedule", "getScheduledJobs", "getSchedules", "getSequenceName", "getTransactionName",
		"isAllEnabled", "isEnable", "isSerial", "isWriteOutput"
	));
	private static final Set<String> ALLOWED_PROPERTIES = new HashSet<String>(Arrays.asList(
		"jobs", "schedules", "scheduledJobs", "name", "description", "enable", "contextName", "projectName",
		"writeOutput", "parameters", "sequenceName", "connectorName", "transactionName", "jobGroup",
		"parallelJob", "cron", "job", "schedule"
	));

	@Override
	protected void doUpload(HttpServletRequest request, Document document, FileItem item) throws Exception {
		String fileName = item.getName();
		if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".xml")) {
			throw new ServiceException("The scheduler import requires a .xml file");
		}

		byte[] data = IOUtils.toByteArray(item.getInputStream());
		validateSchedulerXML(data);
		SchedulerXML imported = SchedulerManager.readSchedulerXML(new ByteArrayInputStream(data));

		String actionImport = request.getParameter("action-import");
		if (actionImport == null || "on".equals(actionImport)) {
			actionImport = request.getParameter("priority");
		}
		if (actionImport == null) {
			actionImport = "priority-import";
		}
		if (!"clear-import".equals(actionImport) && !"priority-server".equals(actionImport)
				&& !"priority-import".equals(actionImport)) {
			throw new ServiceException("Invalid scheduler import policy");
		}

		SchedulerManager schedulerManager = Engine.theApp.schedulerManager;
		SchedulerXML merged = mergeSchedulerXML(
			schedulerManager.getSchedulerXML(),
			imported,
			"clear-import".equals(actionImport),
			"priority-server".equals(actionImport)
		);
		validateConfiguration(merged);
		backupSchedulerFile();

		SchedulerXML previous = schedulerManager.getSchedulerXML();
		schedulerManager.setSchedulerXML(merged);
		if (!schedulerManager.saveWithResult()) {
			schedulerManager.setSchedulerXML(previous);
			throw new ServiceException("Unable to save the imported scheduler configuration");
		}
		schedulerManager.refreshJobs();

		String message = "The scheduler configuration has been successfully imported.";
		Engine.logAdmin.info(message);
		ServiceUtils.addMessage(document, document.getDocumentElement(), message, "message", false);
	}

	static SchedulerXML mergeSchedulerXML(SchedulerXML server, SchedulerXML imported, boolean replace,
			boolean serverPriority) throws ServiceException {
		server = copySchedulerXML(server);
		imported = copySchedulerXML(imported);
		SchedulerXML result = new SchedulerXML();
		if (replace) {
			result.getJobs().addAll(imported.getJobs());
			result.getSchedules().addAll(imported.getSchedules());
			result.getScheduledJobs().addAll(imported.getScheduledJobs());
		} else {
			merge(result.getJobs(), server.getJobs(), imported.getJobs(), serverPriority);
			merge(result.getSchedules(), server.getSchedules(), imported.getSchedules(), serverPriority);
			merge(result.getScheduledJobs(), server.getScheduledJobs(), imported.getScheduledJobs(), serverPriority);
		}

		rebindReferences(result);
		return result;
	}

	private static SchedulerXML copySchedulerXML(SchedulerXML source) throws ServiceException {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			SchedulerManager.writeSchedulerXML(source, outputStream);
			return SchedulerManager.readSchedulerXML(new ByteArrayInputStream(outputStream.toByteArray()));
		} catch (IOException e) {
			throw new ServiceException("Unable to copy the scheduler configuration", e);
		}
	}

	private static <T extends AbstractBase> void merge(SortedSet<T> target, SortedSet<T> server,
			SortedSet<T> imported, boolean serverPriority) {
		Map<String, T> byName = new HashMap<String, T>();
		for (T element : serverPriority ? imported : server) {
			byName.put(element.getName(), element);
		}
		for (T element : serverPriority ? server : imported) {
			byName.put(element.getName(), element);
		}
		target.addAll(byName.values());
	}

	private static void rebindReferences(SchedulerXML schedulerXML) throws ServiceException {
		Map<String, AbstractJob> jobs = new HashMap<String, AbstractJob>();
		for (AbstractJob job : schedulerXML.getJobs()) {
			jobs.put(job.getName(), job);
		}
		Map<String, AbstractSchedule> schedules = new HashMap<String, AbstractSchedule>();
		for (AbstractSchedule schedule : schedulerXML.getSchedules()) {
			schedules.put(schedule.getName(), schedule);
		}

		for (AbstractJob job : schedulerXML.getJobs()) {
			if (job instanceof JobGroupJob) {
				JobGroupJob group = (JobGroupJob) job;
				List<String> memberNames = new ArrayList<String>();
				for (AbstractJob member : group.getJobGroup()) {
					memberNames.add(member.getName());
				}
				group.delAllJobs();
				for (String memberName : memberNames) {
					AbstractJob member = jobs.get(memberName);
					if (member == null) {
						throw new ServiceException("The job group '" + group.getName()
							+ "' references the missing job '" + memberName + "'");
					}
					group.addJob(member);
				}
			}
		}

		for (ScheduledJob scheduledJob : schedulerXML.getScheduledJobs()) {
			String jobName = scheduledJob.getJob() == null ? null : scheduledJob.getJob().getName();
			String scheduleName = scheduledJob.getSchedule() == null ? null : scheduledJob.getSchedule().getName();
			AbstractJob job = jobs.get(jobName);
			AbstractSchedule schedule = schedules.get(scheduleName);
			if (job == null || schedule == null) {
				throw new ServiceException("The scheduled job '" + scheduledJob.getName()
					+ "' references a missing job or schedule");
			}
			scheduledJob.setJob(job);
			scheduledJob.setSchedule(schedule);
		}
	}

	private static void validateConfiguration(SchedulerXML schedulerXML) throws ServiceException {
		for (AbstractBase element : allElements(schedulerXML)) {
			List<String> problems = schedulerXML.checkProblems(element);
			if (!problems.isEmpty()) {
				throw new ServiceException("Invalid scheduler element '" + element.getName() + "': "
					+ String.join(", ", problems));
			}
		}
	}

	private static List<AbstractBase> allElements(SchedulerXML schedulerXML) {
		List<AbstractBase> elements = new ArrayList<AbstractBase>();
		elements.addAll(schedulerXML.getJobs());
		elements.addAll(schedulerXML.getSchedules());
		elements.addAll(schedulerXML.getScheduledJobs());
		return elements;
	}

	private static void validateSchedulerXML(byte[] data) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(data));

		Element root = document.getDocumentElement();
		if (!"java".equals(root.getTagName()) || !"java.beans.XMLDecoder".equals(root.getAttribute("class"))) {
			throw new ServiceException("The file is not a scheduler.xml configuration");
		}

		NodeList elements = document.getElementsByTagName("*");
		for (int i = 0; i < elements.getLength(); i++) {
			Element element = (Element) elements.item(i);
			if (!ALLOWED_ELEMENTS.contains(element.getTagName())) {
				throw new ServiceException("Unsupported scheduler XML element: " + element.getTagName());
			}
			String className = element.getAttribute("class");
			if (!className.isEmpty()) {
				boolean allowedClass = ("java".equals(element.getTagName()) && "java.beans.XMLDecoder".equals(className))
					|| ("object".equals(element.getTagName())
						&& className.startsWith("com.twinsoft.convertigo.beans.scheduler."))
					|| ("array".equals(element.getTagName()) && "java.lang.String".equals(className));
				if (!allowedClass) {
					throw new ServiceException("Unsupported scheduler XML class: " + className);
				}
			}
			String method = element.getAttribute("method");
			if (!method.isEmpty() && !ALLOWED_METHODS.contains(method)) {
				throw new ServiceException("Unsupported scheduler XML method: " + method);
			}
			String property = element.getAttribute("property");
			if (!property.isEmpty() && !ALLOWED_PROPERTIES.contains(property)) {
				throw new ServiceException("Unsupported scheduler XML property: " + property);
			}
		}
	}

	private void backupSchedulerFile() throws IOException {
		File schedulerFile = new File(Engine.CONFIGURATION_PATH, "scheduler.xml");
		if (!schedulerFile.isFile()) {
			return;
		}

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String baseName = "scheduler_" + dateFormat.format(new Date());
		File backup = new File(schedulerFile.getParentFile(), baseName + ".xml");
		int index = 1;
		while (backup.exists()) {
			backup = new File(schedulerFile.getParentFile(), baseName + "_" + index++ + ".xml");
		}
		FileUtils.copyFile(schedulerFile, backup);
	}

	@Override
	protected String getRepository() {
		return Engine.CONFIGURATION_PATH + "/";
	}
}
