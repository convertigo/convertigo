package com.sap.conn.jco;

public abstract interface JCoRecordField extends JCoField {
	public abstract int getUnicodeByteOffset();

	public abstract int getByteOffset();
}
