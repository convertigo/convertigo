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

package com.twinsoft.convertigo.engine.admin.logmanager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.redisson.client.codec.StringCodec;

import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;
import com.twinsoft.convertigo.engine.sessions.StatefulSessionAttributes;
import com.twinsoft.convertigo.engine.sessions.ConvertigoHttpSessionManager;
import com.twinsoft.convertigo.engine.sessions.RedisClients;

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
				boolean redisMode = ConvertigoHttpSessionManager.isRedisMode();
				Thread.sleep(redisMode ? 60000 : 10000);
				List<Pair<String, HttpSession>> toCleanup = new ArrayList<>();
				synchronized (activeInstance) {
					if (redisMode) {
						Iterator<Map.Entry<String, Pair<Long, HttpSession>>> it = activeInstance.entrySet().iterator();
						while (it.hasNext()) {
							Map.Entry<String, Pair<Long, HttpSession>> entry = it.next();
							try {
								String id = entry.getKey();
								HttpSession session = entry.getValue().getRight();
								if (session == null || !sessionExistsInRedis(session)) {
									toCleanup.add(Pair.of(id, session));
									it.remove();
								}
							} catch (Exception ex) {
								// keep entry on redis errors
							}
						}
					} else {
						long old = System.currentTimeMillis() - 10000;
						Iterator<Map.Entry<String, Pair<Long, HttpSession>>> it = activeInstance.entrySet().iterator();
						while (it.hasNext()) {
							Map.Entry<String, Pair<Long, HttpSession>> entry = it.next();
							try {
								String id = entry.getKey();
								long last = entry.getValue().getLeft();
								if (last < old) {
									HttpSession session = entry.getValue().getRight();
									toCleanup.add(Pair.of(id, session));
									it.remove();
								}
							} catch (Exception ex) {
								toCleanup.add(Pair.of(entry.getKey(), entry.getValue().getRight()));
								it.remove();
							}
						}
					}
				}
				for (Pair<String, HttpSession> entry : toCleanup) {
					cleanupSession(entry.getLeft(), entry.getRight());
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
		var session = request.getSession();
		var logmanager_id = LogServiceHelper.class.getCanonicalName() + ".logmanager_" + ServiceUtils.getAdminInstance(request);
		var logmanager = (LogManager) StatefulSessionAttributes.getStatefulAttribute(session, logmanager_id);
		if (logmanager == null) {
			logmanager = new LogManager();
			StatefulSessionAttributes.setStatefulAttribute(session, logmanager_id, logmanager);
		}
		return logmanager;
	}

	private static boolean sessionExistsInRedis(HttpSession session) {
		try {
			String sessionId = session.getId();
			if (sessionId == null || sessionId.isBlank()) {
				return false;
			}
			var configuration = RedisClients.getConfiguration();
			var client = RedisClients.getClient();
			String key = configuration.getKeyPrefix() + sessionId;
			return client.getMap(key, StringCodec.INSTANCE).isExists();
		} catch (Exception e) {
			return true;
		}
	}

	private static void cleanupSession(String id, HttpSession session) {
		if (session == null) {
			return;
		}
		try {
			Enumeration<String> names = session.getAttributeNames();
			while (names.hasMoreElements()) {
				try {
					String name = names.nextElement();
					if (name != null && name.endsWith(id)) {
						Object obj = session.getAttribute(name);
						if (obj instanceof LogManager) {
							LogManager lm = (LogManager) obj;
							synchronized (lm) {
								lm.close();
							}
						}
						session.removeAttribute(name);
					}
				} catch (Exception ex) {
					System.err.println("[LogServiceHelper] Check failed: " + ex.getMessage());
				}
			}
		} catch (Exception ex) {
			System.err.println("[LogServiceHelper] Cleanup failed: " + ex.getMessage());
		}
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
