package com.twinsoft.convertigo.engine.util;
import java.math.BigInteger;

import javax.xml.namespace.QName;

import jakarta.xml.bind.DatatypeConverter;

public class XsdTypeParser {

	public static Object parse(QName xsdType, String value) {
		if (value == null) return null;

		String localPart = xsdType.getLocalPart();
		switch (localPart) {
		case "string":
		case "anyURI":
		case "token":
		case "normalizedString":
		case "language":
		case "Name":
		case "NCName":
		case "ID":
		case "IDREF":
		case "ENTITY":
			return value;

		case "boolean":
			return DatatypeConverter.parseBoolean(value);

		case "byte":
			return DatatypeConverter.parseByte(value);

		case "unsignedByte":
			return Short.parseShort(value); // No unsigned byte in Java

		case "short":
			return DatatypeConverter.parseShort(value);

		case "unsignedShort":
			return Integer.parseInt(value); // no unsigned short in Java

		case "int":
		case "integer":
			return DatatypeConverter.parseInt(value);

		case "unsignedInt":
			return Long.parseLong(value); // no unsigned int in Java

		case "long":
			return DatatypeConverter.parseLong(value);

		case "unsignedLong":
		case "nonNegativeInteger":
		case "positiveInteger":
			return new BigInteger(value);

		case "negativeInteger":
		case "nonPositiveInteger":
			return new BigInteger(value);

		case "float":
			return DatatypeConverter.parseFloat(value);

		case "double":
		case "decimal":
			return DatatypeConverter.parseDouble(value);

		case "date":
			return DatatypeConverter.parseDate(value).getTime();

		case "dateTime":
			return DatatypeConverter.parseDateTime(value).getTime();

		case "time":
			return DatatypeConverter.parseTime(value).getTime();

		case "base64Binary":
			return DatatypeConverter.parseBase64Binary(value);

		case "hexBinary":
			return DatatypeConverter.parseHexBinary(value);

		default:
			return value;
		}
	}
}
