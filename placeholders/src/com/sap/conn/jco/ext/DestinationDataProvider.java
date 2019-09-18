package com.sap.conn.jco.ext;

import java.util.Properties;

public abstract interface DestinationDataProvider {
	public static final String JCO_AUTH_TYPE = "jco.destination.auth_type";
	public static final String JCO_AUTH_TYPE_CONFIGURED_USER = "CONFIGURED_USER";
	public static final String JCO_AUTH_TYPE_CURRENT_USER = "CURRENT_USER";

	/** @deprecated */
	public static final String JCO_USER_ID = "";
	public static final String JCO_CLIENT = "";
	public static final String JCO_USER = "";
	public static final String JCO_ALIAS_USER = "";
	public static final String JCO_PASSWD = "";
	public static final String JCO_LANG = "";
	public static final String JCO_CODEPAGE = "";
	public static final String JCO_PCS = "";
	public static final String JCO_ASHOST = "";
	public static final String JCO_SYSNR = "";
	public static final String JCO_MSHOST = "";
	public static final String JCO_MSSERV = "";
	public static final String JCO_R3NAME = "";
	public static final String JCO_GROUP = "";
	public static final String JCO_SAPROUTER = "";
	public static final String JCO_MYSAPSSO2 = "";
	public static final String JCO_GETSSO2 = "";
	public static final String JCO_X509CERT = "";
	public static final String JCO_EXTID_DATA = "";
	public static final String JCO_EXTID_TYPE = "";
	public static final String JCO_LCHECK = "";
	public static final String JCO_DELTA = "";
	public static final String JCO_SNC_PARTNERNAME = "";
	public static final String JCO_SNC_QOP = "";
	public static final String JCO_SNC_MYNAME = "";
	public static final String JCO_SNC_MODE = "";
	public static final String JCO_SNC_SSO = "";
	public static final String JCO_SNC_LIBRARY = "";
	public static final String JCO_DEST = "";
	public static final String JCO_PEAK_LIMIT = "";
	public static final String JCO_POOL_CAPACITY = "";
	public static final String JCO_EXPIRATION_TIME = "";
	public static final String JCO_EXPIRATION_PERIOD = "";
	public static final String JCO_MAX_GET_TIME = "";
	public static final String JCO_REPOSITORY_DEST = "";
	public static final String JCO_REPOSITORY_USER = "";
	public static final String JCO_REPOSITORY_PASSWD = "";
	public static final String JCO_REPOSITORY_SNC = "";
	public static final String JCO_CPIC_TRACE = "";
	public static final String JCO_TRACE = "";
	public static final String JCO_GWHOST = "";
	public static final String JCO_GWSERV = "";
	public static final String JCO_TPHOST = "";
	public static final String JCO_TPNAME = "";
	public static final String JCO_TYPE = "";
	public static final String JCO_USE_SAPGUI = "";
	public static final String JCO_DENY_INITIAL_PASSWORD = "";
	public static final String JCO_REPOSITORY_ROUNDTRIP_OPTIMIZATION = "";

	public abstract Properties getDestinationProperties(String paramString);

	public abstract boolean supportsEvents();

	public abstract void setDestinationDataEventListener(
			DestinationDataEventListener paramDestinationDataEventListener);
}
