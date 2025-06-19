/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ContextFactory.Listener;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;

public class RhinoUtils {
	static public boolean debugMode = false;
	static final public Object[] EMPTY_ARGS = new Object[0];
	static final private Map<String, Script> compiledScript = new ConcurrentHashMap<String, Script>();
	static final private NativeJSON json;
	static {
		ScriptableObject scope = Context.enter().initStandardObjects();
		json = (NativeJSON) scope.get("JSON", scope);
		Context.exit();
	}

	static public Scriptable copyScope(Context context, Scriptable scope) {
		Scriptable scopeCopy = context.initStandardObjects();
		for (Object id : scope.getIds()) {
			scopeCopy.put(id.toString(), scopeCopy, scope.get(id.toString(), scope));
		}
		return scopeCopy;
	}
	static public Object evalCachedJavascript(Context cx, Scriptable scope, String source, String sourceName, int lineno, Object securityDomain) {
		return evalCachedJavascript(null, cx, scope, source, sourceName, lineno, securityDomain);
	}

	static public Object evalCachedJavascript(DatabaseObject dbo, Context cx, Scriptable scope, String source, String sourceName, int lineno, Object securityDomain) {
		source = CopilotHelper.addInstruction(dbo, source);
		if (debugMode) {
			if (dbo != null) {
				sourceName = dbo.getShortQName() + "-" + sourceName;
			}
			return evalInterpretedJavascript(cx, scope, source, sourceName, lineno, securityDomain);
		}
		Script script = compiledScript.get(source);
		if (script == null) {
			cx.setInterpretedMode(false);
			script = cx.compileString(source, sourceName, lineno, securityDomain);
			compiledScript.put(source, script);
		}
		Object result = script.exec(cx, scope);
		return result;
	}

	static public Object evalInterpretedJavascript(Context cx, Scriptable scope, String source, String sourceName, int lineno, Object securityDomain) {
		cx.setInterpretedMode(true);
		Object result = cx.evaluateString(scope, source, sourceName, lineno, securityDomain);
		return result;
	}

	static public Object jsonParse(String string) {
		return ScriptableObject.callMethod(json, "parse", new Object[]{string});
	}

	static public String jsonStringify(Object object) {
		return ScriptableObject.callMethod(json, "stringify", new Object[]{object}).toString();
	}

	static public void init() {
		ContextFactory.getGlobal().addListener(new Listener() {

			@Override
			public void contextReleased(Context cx) {
			}

			@Override
			public void contextCreated(Context cx) {
				cx.setLanguageVersion(Context.VERSION_ES6);
			}
		});
	}
}
