package com.sap.conn.jco;

import java.util.List;

public abstract class JCoDestinationManager {	
	/** @deprecated */
	public abstract JCoDestination getDestinationInstance(String paramString1,
			String paramString2) throws JCoException;

	/** @deprecated */
	public abstract List<String> getDestinationIDs();

	/** @deprecated */
	public abstract List<String> getCustomDestinationIDs(String paramString);

	public static JCoDestination getDestination(String destinationName)
			throws JCoException {
		throw new FakeJarException(JCoDestinationManager.class);
	}

	public static JCoDestination getDestination(String destinationName,
			String scopeType) throws JCoException {
		throw new FakeJarException(JCoDestinationManager.class);
	}
}
