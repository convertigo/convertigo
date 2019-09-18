package com.sap.conn.jco;

import java.util.Properties;

public abstract interface JCoDestination {
	public abstract JCoAttributes getAttributes() throws JCoException;

	public abstract String getDestinationName();

	public abstract String getDestinationID();

	public abstract char getType();

	public abstract String getApplicationServerHost();

	public abstract String getSAPRouterString();

	public abstract String getGatewayHost();

	public abstract String getGatewayService();

	public abstract String getSystemNumber();

	public abstract String getLogonGroup();

	public abstract String getMessageServerHost();

	public abstract String getMessageServerService();

	public abstract String getR3Name();

	public abstract String getTPHost();

	public abstract String getTPName();

	public abstract String getSncLibrary();

	public abstract String getSncMode();

	public abstract String getSncMyName();

	public abstract String getSncPartnerName();

	public abstract String getSncQOP();

	public abstract String getSncSSO();

	public abstract String getAliasUser();

	public abstract String getClient();

	public abstract String getLanguage();

	public abstract String getUser();

	public abstract String getLogonCheck();

	public abstract String getExternalIDData();

	public abstract String getExternalIDType();

	public abstract int getPeakLimit();

	public abstract int getPoolCapacity();

	public abstract long getExpirationTime();

	public abstract long getExpirationCheckPeriod();

	public abstract long getMaxGetClientTime();

	public abstract Properties getProperties();

	public abstract JCoRepository getRepository() throws JCoException;

	public abstract String getRepositoryUser();

	public abstract JCoCustomDestination createCustomDestination();

	public abstract void confirmTID(String paramString) throws JCoException;

	public abstract String createTID() throws JCoException;

	public abstract void removeThroughput();

	public abstract void ping() throws JCoException;

	public abstract boolean isValid();

	public abstract void changePassword(String paramString1, String paramString2)
			throws JCoException;

}
