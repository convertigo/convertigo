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

package com.twinsoft.convertigo.engine.admin.logmanager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;

public class LogServiceHelper {
	public enum LogManagerParameter {
		nbLines,
		timeout,
		moreResults,
		filter,
		startDate,
		endDate
	}

	private static final DateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

	public static LogManager getLogManager(HttpServletRequest request) {
		HttpSession session = request.getSession();
		String logmanager_id = LogServiceHelper.class.getCanonicalName() + ".logmanager_" + ServiceUtils.getAdminInstance(request);
		LogManager logmanager = (LogManager) session.getAttribute(logmanager_id);
		if (logmanager == null) {
			logmanager = new LogManager();
			session.setAttribute(logmanager_id, logmanager);
		}
		return logmanager;
	}

	public static void prepareLogManager(HttpServletRequest request, LogManager logmanager, LogManagerParameter ... parameters) throws ServiceException {
		if (parameters.length == 0) {
			parameters = LogManagerParameter.values();
		}

		for (LogManagerParameter parameter : parameters) {
			try {
				String sParameter = request.getParameter(parameter.name());
				switch (parameter) {
				case nbLines:
					logmanager.setMaxLines(Integer.parseInt(sParameter));
					break;
				case timeout:
					logmanager.setTimeout(Long.parseLong(sParameter));
					break;
				case moreResults:
					logmanager.setContinue(Boolean.parseBoolean(sParameter));
					break;
				case filter:
					logmanager.setFilter(sParameter);
					break;
				case startDate:
					logmanager.setDateStart(date_format.parse(sParameter));
					break;
				case endDate:
					logmanager.setDateEnd(date_format.parse(sParameter));
					break;
				}
			} catch (ServiceException e) {
				throw e;
			} catch (Exception e) {
				// ignore some parser error
			}
		}
	}
}
