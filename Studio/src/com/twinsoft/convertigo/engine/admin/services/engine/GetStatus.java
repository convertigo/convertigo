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

package com.twinsoft.convertigo.engine.admin.services.engine;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.tas.Key;
import com.twinsoft.tas.KeyManager;
import com.twinsoft.util.TWSKey;

@ServiceDefinition(
		name = "GetStatus",
		roles = { Role.TEST_PLATFORM },
		parameters = {},
		returnValue = "the engine status"
		)
public class GetStatus extends XmlService {

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element rootElement = document.getDocumentElement();

		Locale locale = Locale.getDefault();
		String timezone = Calendar.getInstance().getTimeZone().getDisplayName(false, TimeZone.SHORT);
		long now = System.currentTimeMillis();
		long currentTimeSec = now / 1000;
		long startDateSec = Engine.startStopDate / 1000;
		long runningElapseDays = (currentTimeSec - startDateSec) / 86400;
		long runningElapseHours = ((currentTimeSec - startDateSec) / 3600) % 24;
		long runningElapseMin = ((currentTimeSec - startDateSec) / 60) % 60;
		long runningElapseSec = (currentTimeSec - startDateSec) % 60;

		Element versionElement = document.createElement("version");
		versionElement.setAttribute("product", com.twinsoft.convertigo.engine.Version.fullProductVersion);
		versionElement.setAttribute("id", com.twinsoft.convertigo.engine.Version.fullProductVersionID);
		versionElement.setAttribute("beans", com.twinsoft.convertigo.beans.Version.version);
		versionElement.setAttribute("engine", com.twinsoft.convertigo.engine.Version.version);
		versionElement.setAttribute("build", com.twinsoft.convertigo.engine.Version.revision);

		// We list each keys to know how are valid and what is the SE key
		Iterator<?> iter = KeyManager.keys.values().iterator();
		int nbValidKey = 0;
		boolean licenceMismatch = true;
		Key seKey = null;
		while (iter.hasNext()) {
			Key key = (Key)iter.next();
			if (key.emulatorID == com.twinsoft.api.Session.EmulIDSE) {
				seKey = key;
				licenceMismatch = false;
				
			}
			nbValidKey += KeyManager.hasExpired(key.emulatorID) ? 0 : (key.bDemo ? 0 : 1);
		}
		
		int iCategory = 0;
		int iStations = 0;
		String endDate = null;
		Date currentDate = new Date();
		Date expiredDate = null;
		int iNumberOfDays = -1;
		
		TWSKey twsKey = new TWSKey(); 	twsKey.CreateKey(3);
		
		if (seKey != null) {			
			iCategory = 15;
			iStations = seKey.licence;
			iNumberOfDays = seKey.expiration;

			//We search the licence expiry date
			if (iNumberOfDays != 0) {
				expiredDate = new Date((long) (iNumberOfDays) * 1000 * 60 * 60 * 24);
				SimpleDateFormat formater = new SimpleDateFormat("MM/dd/yyyy");
				endDate = formater.format(expiredDate);
			}
		}
		
		versionElement.setAttribute("licence-type", iCategory == 15 ? 
				(nbValidKey > 1 ? 
						"Convertigo Extended Edition" + (licenceMismatch ? "(! licence mismatch !)": "") : 
							(nbValidKey == 0 ? 
								"Convertigo Community Edition" : 
								(licenceMismatch ? "(! licence mismatch !)": "Convertigo Standard Edition") ) ) 
						: "Convertigo Community Edition");
		versionElement.setAttribute("licence-number", iCategory == 15 ? (990000000 + iStations) + "" : "n/a");
		versionElement.setAttribute("licence-end", iNumberOfDays != 0 ? (iNumberOfDays < 0 ? "n/a" : endDate) : "unlimited");
		versionElement.setAttribute("licence-expired", iNumberOfDays != 0 ? (iNumberOfDays < 0 ? "n/a" : currentDate.compareTo(expiredDate) > 0) + "" : "false");
		rootElement.appendChild(versionElement);

		try {
			Element buildElement = document.createElement("build");
			Properties properties = new Properties();
			ServletContext servletContext = request.getSession().getServletContext();
			InputStream buildInfoFile = servletContext.getResourceAsStream("/WEB-INF/build.txt");
			if (buildInfoFile != null) {
				properties.load(buildInfoFile);
				buildElement.setAttribute("date", properties.getProperty("build.date"));
				buildElement.setAttribute("filename", properties.getProperty("build.filename"));
				buildElement.setAttribute("version", properties.getProperty("build.version"));
				rootElement.appendChild(buildElement);
			}
		} catch (Exception e) {
			// Ignore
			Engine.logAdmin.error("Unable to get build info", e);
		}

		Element EngineState = document.createElement("engineState");
		Text textStart = null;
		if (Engine.isStarted) {
			textStart = document.createTextNode("started");
		} else {
			textStart = document.createTextNode("stopped");
		}
		EngineState.appendChild(textStart);
		rootElement.appendChild(EngineState);

		Element startStopDateElement = document.createElement("startStopDate");
		Text textNode = document.createTextNode(String.valueOf(Engine.startStopDate));
		startStopDateElement.appendChild(textNode);
		startStopDateElement.setAttribute("localeFormatted", DateFormat.getDateInstance(DateFormat.LONG, locale).format(Engine.startStopDate) + " - " + DateFormat.getTimeInstance(DateFormat.MEDIUM, locale).format(Engine.startStopDate));
		startStopDateElement.setAttribute("locale", locale.toString());
		startStopDateElement.setAttribute("timezone", timezone);
		rootElement.appendChild(startStopDateElement);

		Element runningElapseElement = document.createElement("runningElapse");
		runningElapseElement.setAttribute("days", String.valueOf(runningElapseDays));
		runningElapseElement.setAttribute("hours", String.valueOf(runningElapseHours));
		runningElapseElement.setAttribute("minutes", String.valueOf(runningElapseMin));
		runningElapseElement.setAttribute("seconds", String.valueOf(runningElapseSec));
		textNode = document.createTextNode(String.valueOf(now - Engine.startStopDate));
		runningElapseElement.appendChild(textNode);
		rootElement.appendChild(runningElapseElement);

		Element dateTime = document.createElement("time");
		dateTime.setTextContent(String.valueOf(now));
		dateTime.setAttribute("localeFormatted", DateFormat.getDateInstance(DateFormat.LONG, locale).format(now) + " - " + DateFormat.getTimeInstance(DateFormat.MEDIUM, locale).format(now));
		dateTime.setAttribute("locale", locale.toString());
		dateTime.setAttribute("timezone", timezone);
		rootElement.appendChild(dateTime);
	}
}
