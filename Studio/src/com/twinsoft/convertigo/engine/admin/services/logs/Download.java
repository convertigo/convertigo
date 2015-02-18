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

package com.twinsoft.convertigo.engine.admin.services.logs;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;

import com.twinsoft.convertigo.engine.admin.logmanager.LogServiceHelper;
import com.twinsoft.convertigo.engine.admin.logmanager.LogManager;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.util.GenericUtils;

@ServiceDefinition(
		name = "Download",
		roles = { Role.WEB_ADMIN },
		parameters = {},
		returnValue = ""
	)
public class Download extends DownloadService {
	private static final DateFormat date_format = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
	private Matcher bn = Pattern.compile("\n").matcher("");

	@Override
	protected void writeResponseResult(HttpServletRequest request, HttpServletResponse response) throws Exception { 
		String host = request.getHeader("Host");
		if (host == null) {
			host = "";
		} else { 
			host = host.replaceFirst(":", "-");
		}
		String basename = "cems_" + host + "_" + date_format.format(new Date());
		LogManager logmanager = LogServiceHelper.getLogManager(request);
		synchronized (logmanager) {
			LogServiceHelper.prepareLogManager(request, logmanager);
			logmanager.setTimeout(5000);
			logmanager.setMaxLines(200);
			logmanager.setContinue(false);
			
			response.setHeader("Content-Disposition", "attachment; filename=\"" + basename + "_log.zip\"");
			response.setContentType("application/zip");
			
			ZipOutputStream zop = new ZipOutputStream(response.getOutputStream());
			Writer out = new OutputStreamWriter(zop, "UTF-8");
			
			zop.putNextEntry(new ZipEntry(basename + "_options.log"));
			for (String name : Collections.list(GenericUtils.<Enumeration<String>>cast(request.getParameterNames()))) {
				out.write(name + "=" + request.getParameter(name) + "\r\n");
			}
			out.flush();
			
			zop.putNextEntry(new ZipEntry(basename + ".log"));
			while (logmanager.hasMoreResults()) {
				JSONArray lines = logmanager.getLines();
				for (int i = 0 ; i < lines.length() ; i++) {
					JSONArray line = lines.getJSONArray(i);
					StringBuffer extra = new StringBuffer();
					for (int j = 5 ; j < line.length() ; j++) {
						extra.append(" | $" + line.getString(j));
					}
					bn.reset(line.getString(4));
					out.write(String.format("!%-28s | %s | %-5s | %-32s%s | %s\r\n", line.getString(0), line.getString(1), line.getString(2), line.getString(3), extra.toString(), bn.replaceAll("\r\n")));			
				}
				logmanager.setContinue(true);
			}
			out.flush();
			zop.close();	
		}
	}
}