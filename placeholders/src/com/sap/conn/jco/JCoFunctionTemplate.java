package com.sap.conn.jco;

import java.io.Serializable;

public abstract interface JCoFunctionTemplate extends Serializable {
	public abstract String getName();

	public abstract JCoListMetaData getImportParameterList();

	public abstract JCoListMetaData getExportParameterList();

	public abstract JCoListMetaData getChangingParameterList();

	public abstract JCoListMetaData getTableParameterList();

	public abstract JCoFunction getFunction();

	public abstract JCoListMetaData getFunctionInterface();

	public abstract boolean supportsASXML();
}
