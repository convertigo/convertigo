/*
* Copyright (c) 2014 Convertigo. All Rights Reserved.
*
* The copyright to the computer  program(s) herein  is the property
* of Convertigo.
* The program(s) may  be used  and/or copied  only with the written
* permission  of  Convertigo  or in accordance  with  the terms and
* conditions  stipulated  in the agreement/contract under which the
* program(s) have been supplied.
*
* Convertigo makes  no  representations  or  warranties  about  the
* suitability of the software, either express or implied, including
* but  not  limited  to  the implied warranties of merchantability,
* fitness for a particular purpose, or non-infringement. Convertigo
* shall  not  be  liable for  any damage  suffered by licensee as a
* result of using,  modifying or  distributing this software or its
* derivatives.
*/

/*
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
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
