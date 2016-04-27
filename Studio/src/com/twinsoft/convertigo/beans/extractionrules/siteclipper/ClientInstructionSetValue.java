/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.extractionrules.siteclipper;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaObject;

import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector.Shuttle;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.siteclipper.clientinstruction.SetValue;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class ClientInstructionSetValue extends AbstractClientInstructionWithPath {
	private static final long serialVersionUID = -2865404303732191409L;
	
	private String targetValue = "\"value\"";
	
	public ClientInstructionSetValue() {
		super();
	}

	@Override
	public boolean applyOnResponse(Shuttle shuttle) {
		String targetPath = getEvaluatedTargetPath(shuttle);
			
		String value = "";
		Object ob = shuttle.evalJavascript(targetValue);
		if (ob != null) {
			if (ob instanceof NativeJavaArray) {
				Object object = ((NativeJavaArray)ob).unwrap();
				List<Object> list = Arrays.asList((Object[])object);
				for (int j=0; j<list.size(); j++) {
					Object item = list.get(j);
					value += item.toString() + ",";
				}
			} else if (ob instanceof NativeJavaObject) {
				NativeJavaObject nativeJavaObject = (NativeJavaObject)ob;
				Object javaObject = nativeJavaObject.unwrap();
				if (javaObject instanceof List) {
					Vector<String> v = GenericUtils.cast(javaObject);
					for (int j=0; j<v.size(); j++) {
						value += v.get(j) + ",";
					}
				} else {
					value = (String)nativeJavaObject.getDefaultValue(String.class);
				}
				
			} else if (ob instanceof NativeArray) {
				NativeArray array = (NativeArray)ob;
				for (int j=0; j<array.getLength(); j++) {
					Object item = array.get(j,array);
					value += item.toString() + ",";
				}
			} else if (ob instanceof List) {
				Vector<String> v = GenericUtils.cast(ob);
				for (int j=0; j<v.size(); j++) {
					value += v.get(j) + ",";
				}
			} else {
				value = ob.toString();
			}
		}
		
		
		Engine.logSiteClipper.trace("(ClientInstructionSetValue) JQuery selector '" + targetPath + "' with value '" + value + "'");
		shuttle.addPostInstruction(new SetValue(targetPath, value));
		return true;
	}

	public String getTargetValue() {
		return targetValue;
	}

	public void setTargetValue(String targetValue) {
		this.targetValue = targetValue;
	}
}
