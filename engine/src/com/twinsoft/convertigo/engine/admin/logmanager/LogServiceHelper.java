/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.logmanager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

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

	private static final Map<String, Pair<Long, HttpSession>> activeInstance = new HashMap<>();
	private static final Thread logmanagerCleaner = new Thread(() -> {
		while(true) {
			try {
				Thread.sleep(10000);
				synchronized (activeInstance) {
					long old = System.currentTimeMillis() - 10000;
					activeInstance.entrySet().removeIf(e -> {
						try {
							String id = e.getKey();
							long last = e.getValue().getLeft();
							if (last < old) {
								HttpSession session = e.getValue().getRight();
								Enumeration<String> names = session.getAttributeNames();
								while (names.hasMoreElements()) {
									try {
										String name = names.nextElement();
										if (name.endsWith(id)) {
											Object obj = session.getAttribute(name);
											if (obj instanceof LogManager) {
												LogManager lm = (LogManager) obj;
												lm.close();
											}
											session.removeAttribute(name);
										}
									} catch (Exception ex) {
										System.err.println("[LogServiceHelper] Check failed: " + ex.getMessage());
									}
								}
								return true;
							}
						} catch (Exception ex) {
							return true;
						}
						return false;
					});
				}
			} catch (Exception e) {
				System.err.println("[LogServiceHelper] Loop failed: " + e.getMessage());
			}
		}
	});
	
	static {
		logmanagerCleaner.setDaemon(true);
		logmanagerCleaner.setName("LogManager Cleaner");
		logmanagerCleaner.start();
	}
	
	private static final DateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
	
	public static void aliveAdminInstance(HttpServletRequest request) {
		String instance = ServiceUtils.getAdminInstance(request);
		if (StringUtils.isNotBlank(instance)) {
			HttpSession session = request.getSession();
			synchronized (activeInstance) {
				activeInstance.put(instance, Pair.of(System.currentTimeMillis(), session));
			}
		}
	}
	
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
