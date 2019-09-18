package com.sap.conn.jco;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

public abstract interface JCoField {
	public abstract boolean isInitialized();

	public abstract boolean isStructure();

	public abstract boolean isTable();

	public abstract String getName();

	public abstract int getType();

	public abstract int getByteLength();

	public abstract int getUnicodeByteLength();

	public abstract int getLength();

	public abstract int getDecimals();

	public abstract String getDescription();

	public abstract String getTypeAsString();

	public abstract JCoExtendedFieldMetaData getExtendedFieldMetaData();

	public abstract JCoRecordMetaData getRecordMetaData();

	public abstract String getClassNameOfValue();

	public abstract Object getValue();

	public abstract String getString();

	public abstract char getChar();

	public abstract char[] getCharArray();

	public abstract short getShort();

	public abstract int getInt();

	public abstract long getLong();

	public abstract BigInteger getBigInteger();

	public abstract double getDouble();

	public abstract byte getByte();

	public abstract float getFloat();

	public abstract BigDecimal getBigDecimal();

	public abstract Date getDate();

	public abstract Date getTime();

	public abstract byte[] getByteArray();

	public abstract InputStream getBinaryStream();

	public abstract Reader getCharacterStream();

	public abstract JCoTable getTable();

	public abstract JCoStructure getStructure();

	public abstract void setValue(Object paramObject);

	public abstract void setValue(String paramString);

	public abstract void setValue(char paramChar);

	public abstract void setValue(char[] paramArrayOfChar, int paramInt1,
			int paramInt2);

	public abstract void setValue(short paramShort);

	public abstract void setValue(int paramInt);

	public abstract void setValue(long paramLong);

	public abstract void setValue(double paramDouble);

	public abstract void setValue(byte[] paramArrayOfByte);

	public abstract void setValue(byte paramByte);

	public abstract void setValue(JCoStructure paramJCoStructure);

	public abstract void setValue(JCoTable paramJCoTable);

	public abstract Writer write(Writer paramWriter) throws IOException;
}
