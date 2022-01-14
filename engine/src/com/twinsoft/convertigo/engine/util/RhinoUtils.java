/*
 * Copyright (c) 2001-2021 Convertigo SA.
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
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

public class RhinoUtils {
	static final private Map<String, Script> compiledScript = new ConcurrentHashMap<String, Script>();
	
	static public Scriptable copyScope(Context context, Scriptable scope) {
		Scriptable scopeCopy = context.initStandardObjects();
		for (Object id : scope.getIds()) {
			scopeCopy.put(id.toString(), scopeCopy, scope.get(id.toString(), scope));
		}
		return scopeCopy;
	}
	
	static public Object evalCachedJavascript(Context cx, Scriptable scope, String source, String sourceName, int lineno, Object securityDomain) {
		Script script = compiledScript.get(source);
		if (script == null) {
			cx.setOptimizationLevel(9);
			script = cx.compileString(source, sourceName, lineno, securityDomain);
			compiledScript.put(source, script);
		}
		Object result = script.exec(cx, scope);
		return result;
	}
	
	static public Object evalInterpretedJavascript(Context cx, Scriptable scope, String source, String sourceName, int lineno, Object securityDomain) {
		cx.setOptimizationLevel(-1);
		Object result = cx.evaluateString(scope, source, sourceName, lineno, securityDomain);
		return result;
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
