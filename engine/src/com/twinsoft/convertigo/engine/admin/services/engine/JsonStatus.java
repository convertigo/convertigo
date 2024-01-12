/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.engine;

import java.io.InputStream;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.PropertiesUtils;
import com.twinsoft.tas.Key;
import com.twinsoft.tas.KeyManager;
import com.twinsoft.util.TWSKey;

@ServiceDefinition(
		name = "Monitor",
		roles = { Role.WEB_ADMIN, Role.MONITOR_AGENT, Role.HOME_VIEW, Role.HOME_CONFIG },
		parameters = {},
		returnValue = "the monitoring data"
	)
public class JsonStatus extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		response.put("locale", Locale.getDefault().toString());
		response.put("timezone", Calendar.getInstance().getTimeZone().getDisplayName(false, TimeZone.SHORT));
		response.put("product", com.twinsoft.convertigo.engine.Version.fullProductVersion);
		response.put("id", com.twinsoft.convertigo.engine.Version.fullProductVersionID);
		response.put("beans", com.twinsoft.convertigo.beans.Version.version);
		response.put("engine", com.twinsoft.convertigo.engine.Version.version);
		response.put("build", com.twinsoft.convertigo.engine.Version.revision);

		// We list each keys to know how are valid and what is the SE key
		Iterator<?> iter = KeyManager.keys.values().iterator();
		int nbValidKey = 0;
		boolean licenceMismatch = true;
		Key seKey = null;		
		while (iter.hasNext()) {
			Key key = (Key)iter.next();
			
			if (key.emulatorID == com.twinsoft.api.Session.EmulIDSE) {
				// check (unlimited key or currentKey expiration date later than previous)
				if ((seKey == null) || (key.expiration == 0) || (key.expiration >= seKey.expiration)) {
					seKey = key;
					licenceMismatch = false;
				}
				continue;	// skip overdated or overriden session key, only ONE is allowed
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

			//We search the license expiration date
			if (iNumberOfDays != 0) {
				expiredDate = new Date((long) (iNumberOfDays) * 1000 * 60 * 60 * 24);
				SimpleDateFormat formater = new SimpleDateFormat("MM/dd/yyyy");
				endDate = formater.format(expiredDate);
			}
		}
		
		response.put("licenceType", iCategory == 15 ? 
				(nbValidKey > 1 ? 
						"Convertigo Extended Edition" + (licenceMismatch ? "(! license mismatch !)": "") : 
							(nbValidKey == 0 ? 
								"Convertigo Community Edition" : 
								(licenceMismatch ? "(! license mismatch !)": "Convertigo Standard Edition") ) ) 
						: "Convertigo Community Edition");
		response.put("licenceNumber", iCategory == 15 ? (990000000 + iStations) + "" : "n/a");
		String licenceEnd = (iNumberOfDays != 0) ? (iNumberOfDays < 0 ? "n/a" : endDate) : "unlimited";
		response.put("licenceEnd", licenceEnd);
		response.put("licenceExpired", iNumberOfDays != 0 ? (iNumberOfDays < 0 ? "n/a" : currentDate.compareTo(expiredDate) > 0) + "" : "false");

		try {
			ServletContext servletContext = request.getSession().getServletContext();
			try (InputStream buildInfoFile = servletContext.getResourceAsStream("/WEB-INF/build.txt")) {
				if (buildInfoFile != null) {
					Properties properties = new Properties();
					PropertiesUtils.load(properties, buildInfoFile);
					response.put("buildDate", properties.getProperty("build.date"));
					response.put("buildFilename", properties.getProperty("build.filename"));
					response.put("buildVersion", properties.getProperty("build.version"));
				}
			}
		} catch (Exception e) {
			// Ignore
			Engine.logAdmin.warn("Unable to get build info", e);
		}
        
        String hostName = "n/a";
		String addresses = "n/a";

		try {
			InetAddress address = InetAddress.getLocalHost();
			hostName = address.getHostName();
		} catch (Exception e) {
			Engine.logAdmin.info("Cannot get localhost address: " + e);
		}

		try {
			InetAddress[] inetAddresses = InetAddress.getAllByName(hostName);
			addresses = "";
			addresses += inetAddresses[0].getHostAddress();
			for (int i = 1 ; i < inetAddresses.length ; i++) {
				addresses += ", " + inetAddresses[i].getHostAddress();
			}
		} catch (Exception e) {
			Engine.logAdmin.info("Cannot get current IP address: " + e);
		}
        
        response.put("javaVersion", System.getProperty("java.version"));
        response.put("javaClassVersion", System.getProperty("java.class.version"));
        response.put("javaVendor", System.getProperty("java.vendor"));
        
        response.put("hostName", hostName);
        response.put("hostAddresses", addresses);
        
        response.put("osName", System.getProperty("os.name"));
        response.put("osVersion", System.getProperty("os.version"));
        response.put("osArchitecture", System.getProperty("os.arch"));
        response.put("osAvailableProcessors", Runtime.getRuntime().availableProcessors());
        
        
        response.put("browser", request.getHeader("User-Agent"));
        response.put("cloud", Engine.isCloudMode());
	}

}
