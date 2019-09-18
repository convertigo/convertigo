package com.sap.conn.jco;

import java.io.Serializable;

public abstract interface JCoFunction extends Serializable {
	public abstract String getName();

	public abstract JCoParameterList getImportParameterList();

	public abstract JCoParameterList getExportParameterList();

	public abstract JCoParameterList getChangingParameterList();

	public abstract JCoParameterList getTableParameterList();

	public abstract void execute(JCoDestination paramJCoDestination)
			throws JCoException;

	public abstract void execute(JCoDestination paramJCoDestination,
			String paramString) throws JCoException;

	public abstract void execute(JCoDestination paramJCoDestination,
			String paramString1, String paramString2) throws JCoException;

	public abstract String toXML();

	public abstract JCoFunctionTemplate getFunctionTemplate();
}
