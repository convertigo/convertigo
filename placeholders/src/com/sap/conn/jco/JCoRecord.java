package com.sap.conn.jco;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;

public abstract interface JCoRecord extends Serializable, Cloneable,
		Iterable<JCoField> {
	public abstract JCoMetaData getMetaData();

	public abstract void clear();

	public abstract Object clone();

	public abstract int copyFrom(JCoRecord paramJCoRecord);

	public abstract int getFieldCount();

	public abstract Object getValue(int paramInt);

	public abstract Object getValue(String paramString);

	public abstract String getString(int paramInt);

	public abstract char getChar(int paramInt);

	public abstract byte getByte(int paramInt);

	public abstract byte getByte(String paramString);

	public abstract float getFloat(int paramInt);

	public abstract float getFloat(String paramString);

	public abstract short getShort(int paramInt);

	public abstract int getInt(int paramInt);

	public abstract long getLong(int paramInt);

	public abstract BigInteger getBigInteger(int paramInt);

	public abstract double getDouble(int paramInt);

	public abstract BigDecimal getBigDecimal(int paramInt);

	public abstract Date getDate(int paramInt);

	public abstract Date getTime(int paramInt);

	public abstract byte[] getByteArray(int paramInt);

	public abstract char[] getCharArray(int paramInt);

	public abstract InputStream getBinaryStream(int paramInt);

	public abstract Reader getCharacterStream(int paramInt);

	public abstract JCoTable getTable(int paramInt);

	public abstract JCoStructure getStructure(int paramInt);

	public abstract String toXML(int paramInt);

	public abstract String toXML(String paramString);

	public abstract String toXML();

	public abstract Writer write(int paramInt, Writer paramWriter)
			throws IOException;

	public abstract Writer write(String paramString, Writer paramWriter)
			throws IOException;

	public abstract void setValue(int paramInt, String paramString);

	public abstract String getClassNameOfValue(String paramString);

	public abstract String getString(String paramString);

	public abstract char getChar(String paramString);

	public abstract short getShort(String paramString);

	public abstract int getInt(String paramString);

	public abstract long getLong(String paramString);

	public abstract BigInteger getBigInteger(String paramString);

	public abstract double getDouble(String paramString);

	public abstract BigDecimal getBigDecimal(String paramString);

	public abstract Date getDate(String paramString);

	public abstract Date getTime(String paramString);

	public abstract byte[] getByteArray(String paramString);

	public abstract char[] getCharArray(String paramString);

	public abstract InputStream getBinaryStream(String paramString);

	public abstract Reader getCharacterStream(String paramString);

	public abstract JCoTable getTable(String paramString);

	public abstract JCoStructure getStructure(String paramString);

	public abstract void setValue(int paramInt, char paramChar);

	public abstract void setValue(int paramInt, char[] paramArrayOfChar);

	public abstract void setValue(int paramInt1, char[] paramArrayOfChar,
			int paramInt2, int paramInt3);

	public abstract void setValue(int paramInt, short paramShort);

	public abstract void setValue(int paramInt1, int paramInt2);

	public abstract void setValue(int paramInt, long paramLong);

	public abstract void setValue(int paramInt, double paramDouble);

	public abstract void setValue(int paramInt, byte[] paramArrayOfByte);

	public abstract void setValue(int paramInt, JCoStructure paramJCoStructure);

	public abstract void setValue(int paramInt, JCoTable paramJCoTable);

	public abstract void setValue(int paramInt, Object paramObject);

	public abstract void setValue(String paramString1, String paramString2);

	public abstract void setValue(String paramString, char[] paramArrayOfChar);

	public abstract void setValue(String paramString, char[] paramArrayOfChar,
			int paramInt1, int paramInt2);

	public abstract void setValue(String paramString, char paramChar);

	public abstract void setValue(String paramString, short paramShort);

	public abstract void setValue(String paramString, int paramInt);

	public abstract void setValue(String paramString, long paramLong);

	public abstract void setValue(String paramString, double paramDouble);

	public abstract void setValue(int paramInt, float paramFloat);

	public abstract void setValue(String paramString, float paramFloat);

	public abstract void setValue(int paramInt, BigDecimal paramBigDecimal);

	public abstract void setValue(String paramString, BigDecimal paramBigDecimal);

	public abstract void setValue(int paramInt, byte paramByte);

	public abstract void setValue(String paramString, byte paramByte);

	public abstract void setValue(String paramString, byte[] paramArrayOfByte);

	public abstract void setValue(String paramString,
			JCoStructure paramJCoStructure);

	public abstract void setValue(String paramString, JCoTable paramJCoTable);

	public abstract void setValue(String paramString, Object paramObject);

	public abstract boolean isInitialized(String paramString);

	public abstract boolean isInitialized(int paramInt);

	public abstract Iterator<JCoField> iterator();

	public abstract JCoFieldIterator getFieldIterator();
}
