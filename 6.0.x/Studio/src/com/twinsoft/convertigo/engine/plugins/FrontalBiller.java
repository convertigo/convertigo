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

package com.twinsoft.convertigo.engine.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.engine.CertificateManager;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;

public abstract class FrontalBiller extends Biller {
	protected static Object semaphore = new Object();
	
	public static final String PROPERTIES_SQL_REQUEST_INSERT_REQUEST = "sql.request.insert_request";
	public static final String PROPERTIES_SQL_REQUEST_GET_REQUEST = "sql.request.get_request";
	public static final String PROPERTIES_SQL_REQUEST_DELETE_REQUESTS = "sql.request.delete_requests";

	public static final String POBI_PROPERTIES_FILE_NAME = "/frontal.properties";
	
	public FrontalBiller() throws IOException {
		super();
	}
	
	Properties frontalProperties;
	String certificate;
	
	protected String getService(Context context, Object data) {
		return context.tasServiceCode;
	}
	
	public double getCost(Context context, Object data) {
		try {
			frontalProperties = new Properties();
			frontalProperties.load(new FileInputStream(Engine.CONFIGURATION_PATH + POBI_PROPERTIES_FILE_NAME));
			
			CertificateManager certificateManager = ((HttpConnector) context.getConnector()).certificateManager;
			certificate = new File(certificateManager.keyStore).getName();
			int idx = certificate.indexOf('.');
			if (idx != -1 ) {
				certificate = certificate.substring(0, idx);
			}
		
			return getCostImpl(context, data);
		}
		catch(Throwable e) {
			Engine.logBillers.error("[FrontalBiller] Unable to calculate the transaction cost; aborting billing.", e);
		}
		
		return -1;
	}
	
	public abstract double getCostImpl(Context context, Object data) throws Exception;
	
}
