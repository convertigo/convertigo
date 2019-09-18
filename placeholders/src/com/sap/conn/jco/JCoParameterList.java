package com.sap.conn.jco;

public abstract interface JCoParameterList extends JCoRecord {
	public abstract JCoListMetaData getListMetaData();

	public abstract JCoParameterFieldIterator getParameterFieldIterator();

	public abstract boolean isActive(int paramInt);

	public abstract boolean isActive(String paramString);

	public abstract void setActive(int paramInt, boolean paramBoolean);

	public abstract void setActive(String paramString, boolean paramBoolean);
}
