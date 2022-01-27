/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.editors.completion;

import java.util.ArrayList;
import java.util.List;

public class Entry {
	public static int  ICONID_CTF = 0x0001;
	public static int  ICONID_JQM = 0x0002;
	
	private String	keyword;
	private String	definition;
	private int		iconId;
	private List<String> parameters;

	public Entry() { 
		iconId = ICONID_CTF;
		keyword = "";
		definition = "";
		parameters = new ArrayList<String>();
	}

	public Entry(String key, String def, int icId, List<String>paramList) {
		iconId = icId;
		keyword = key;
		definition = def;
		parameters = paramList;
	}
	
	public void setKeyword(String str) {
		keyword = str;
	}

	public void setDefinition(String str) {
		definition = str;
	}

	public void setIconId(int id) {
		iconId = id;
	}
	
	public void setParameters(List<String> params) {
		parameters = params;
	}
	
	public String getKeyword() {
		return keyword;
	}

	public String getDefinition() {
		return definition;
	}

	public int getIconId() {
		return iconId;
	}

	public List<String> getParameters() {
		return parameters;
	}
	
	public String toString() {
		return keyword + ", " + definition + ", " + iconId + ", " + parameters.toString();
	}
}
