/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

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
