package com.sap.conn.jco;

public abstract interface JCoFieldIterator {
	public abstract void reset();

	public abstract boolean hasNextField();

	public abstract boolean hasPreviousField();

	public abstract JCoField nextField();

	public abstract JCoField previousField();
}
