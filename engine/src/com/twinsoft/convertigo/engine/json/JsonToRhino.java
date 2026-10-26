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

package com.twinsoft.convertigo.engine.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * The JavaScript value of the JSON under a Jackson parser, built in one pass as JSON.parse of Rhino builds it: objects
 * and arrays of the given scope, an index key put as an index, a number as Integer when it is an int, else Double.
 */
public final class JsonToRhino {

	private JsonToRhino() {
	}

	/** the parser is on the first token of a value; it ends on the last token of this value */
	public static Object read(JsonParser parser, Scriptable scope) throws IOException {
		Context cx = Context.getCurrentContext();
		boolean entered = cx == null;
		if (entered) {
			cx = Context.enter();
		}
		try {
			return value(cx, scope, parser);
		} finally {
			if (entered) {
				Context.exit();
			}
		}
	}

	private static Object value(Context cx, Scriptable scope, JsonParser parser) throws IOException {
		JsonToken token = parser.currentToken();
		if (token == null) {
			throw new IOException("No JSON value");
		}
		switch (token) {
		case START_OBJECT: {
			Scriptable object = cx.newObject(scope);
			while (parser.nextToken() == JsonToken.FIELD_NAME) {
				String name = parser.currentName();
				parser.nextToken();
				Object child = value(cx, scope, parser);
				ScriptRuntime.StringIdOrIndex id = ScriptRuntime.toStringIdOrIndex(name);
				if (id.getStringId() == null) {
					object.put(id.getIndex(), object, child);
				} else {
					object.put(id.getStringId(), object, child);
				}
			}
			return object;
		}
		case START_ARRAY: {
			List<Object> items = new ArrayList<Object>();
			while (parser.nextToken() != JsonToken.END_ARRAY) {
				items.add(value(cx, scope, parser));
			}
			return items.isEmpty() ? cx.newArray(scope, 0) : cx.newArray(scope, items.toArray());
		}
		case VALUE_STRING:
			return parser.getText();
		case VALUE_NUMBER_INT:
			if (parser.getNumberType() == JsonParser.NumberType.INT) {
				// -0 included: Rhino gives the int 0 for it too
				return Integer.valueOf(parser.getIntValue());
			}
			// fall through: a long or a big integer, read as Rhino reads it
		case VALUE_NUMBER_FLOAT: {
			double d = Double.parseDouble(parser.getText());
			int i = (int) d;
			return i == d ? (Object) Integer.valueOf(i) : (Object) Double.valueOf(d);
		}
		case VALUE_TRUE:
			return Boolean.TRUE;
		case VALUE_FALSE:
			return Boolean.FALSE;
		case VALUE_NULL:
			return null;
		default:
			throw new IOException("Unexpected JSON token " + token);
		}
	}
}
