package com.sap.conn.jco;

public abstract interface JCoParameterField extends JCoField {
	public abstract boolean isActive();

	public abstract boolean isOptional();

	public abstract boolean isImport();

	public abstract boolean isExport();

	public abstract boolean isChanging();

	public abstract String getDefault();
}
