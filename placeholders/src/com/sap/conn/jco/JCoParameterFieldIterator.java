package com.sap.conn.jco;

public abstract interface JCoParameterFieldIterator extends JCoFieldIterator {
	public abstract boolean hasPreviousField();

	public abstract JCoParameterField nextParameterField();

	public abstract JCoParameterField previousParameterField();
}
