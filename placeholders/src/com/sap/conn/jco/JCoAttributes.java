package com.sap.conn.jco;

import java.io.Serializable;

public abstract interface JCoAttributes extends Serializable {
	public abstract String getDestination();

	public abstract String getHost();

	public abstract String getPartnerHost();

	public abstract String getSystemID();

	public abstract String getSystemNumber();

	public abstract String getClient();

	public abstract String getUser();

	public abstract String getLanguage();

	public abstract String getISOLanguage();

	public abstract String getOwnCodepage();

	public abstract String getOwnCharset();

	public abstract String getOwnEncoding();

	public abstract int getOwnBytesPerChar();

	public abstract String getPartnerCodepage();

	public abstract String getPartnerCharset();

	public abstract String getPartnerEncoding();

	public abstract int getPartnerBytesPerChar();

	public abstract String getRelease();

	public abstract String getPartnerRelease();

	public abstract String getKernelRelease();

	public abstract char getPartnerType();

	public abstract boolean getTrace();

	public abstract char getRfcRole();

	public abstract char getType();

	public abstract String getCPICConversationID();

	public abstract String getSSOTicket();

	public abstract int getPartnerReleaseNumber();
}
