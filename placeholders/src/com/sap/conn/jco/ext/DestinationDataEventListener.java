package com.sap.conn.jco.ext;

public abstract interface DestinationDataEventListener {
	public abstract void deleted(String paramString);

	public abstract void updated(String paramString);
}
