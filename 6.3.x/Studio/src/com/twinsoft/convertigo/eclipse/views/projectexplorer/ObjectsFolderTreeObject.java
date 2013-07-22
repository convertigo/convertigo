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

package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IActionFilter;

public class ObjectsFolderTreeObject extends FolderTreeObject implements IActionFilter {

	public static final int FOLDER_TYPE_INVISIBLE_ROOT = -1; 

	public static final int FOLDER_TYPE_TRANSACTIONS = 0; 
	public static final int FOLDER_TYPE_SHEETS = 1; 
	public static final int FOLDER_TYPE_FUNCTIONS = 2; 
	public static final int FOLDER_TYPE_VARIABLES = 3; 
	public static final int FOLDER_TYPE_SCREEN_CLASSES = 4; 
	public static final int FOLDER_TYPE_INHERITED_SCREEN_CLASSES = 5; 
	public static final int FOLDER_TYPE_CRITERIAS = 6;
	public static final int FOLDER_TYPE_EXTRACTION_RULES = 7;
	public static final int FOLDER_TYPE_POOLS = 8;
	public static final int FOLDER_TYPE_CONNECTORS = 9;
	public static final int FOLDER_TYPE_TEMPLATES = 10;
	public static final int FOLDER_TYPE_SEQUENCES = 11;
	public static final int FOLDER_TYPE_STEPS = 12;
	public static final int FOLDER_TYPE_TESTCASES = 13;
	public static final int FOLDER_TYPE_MOBILEDEVICES = 14;
	
	public int folderType;
	
	public ObjectsFolderTreeObject(Viewer viewer, int folderType) {
		super(viewer, null);
		this.folderType = folderType;
		
		String folderName = "";

		switch(folderType) {
			case FOLDER_TYPE_CONNECTORS:
				folderName = "Connectors";
				break;
			case FOLDER_TYPE_SEQUENCES:
				folderName = "Sequences";
				break;
			case FOLDER_TYPE_MOBILEDEVICES:
				folderName = "Mobile devices";
				break;
			case FOLDER_TYPE_TRANSACTIONS:
				folderName = "Transactions";
				break;
			case FOLDER_TYPE_SHEETS:
				folderName = "Sheets";
				break;
			case FOLDER_TYPE_FUNCTIONS:
				folderName = "Functions";
				break;
			case FOLDER_TYPE_VARIABLES:
				folderName = "Variables";
				break;
			case FOLDER_TYPE_SCREEN_CLASSES:
				folderName = "Screen classes";
				break;
			case FOLDER_TYPE_INHERITED_SCREEN_CLASSES:
				folderName = "Inherited screen classes";
				break;
			case FOLDER_TYPE_CRITERIAS:
				folderName = "Criteria";
				break;
			case FOLDER_TYPE_EXTRACTION_RULES:
				folderName = "Extraction rules";
				break;
			case FOLDER_TYPE_POOLS:
				folderName = "Pools";
				break;
			case FOLDER_TYPE_TEMPLATES:
				folderName = "Templates";
				break;
			case FOLDER_TYPE_STEPS:
				folderName = "Steps";
				break;
			case FOLDER_TYPE_TESTCASES:
				folderName = "Test cases";
				break;
		}

        setObject(folderName);
	}

	public boolean testAttribute(Object target, String name, String value) {
		if (name.equals("folderType")) {
			int iTest = Integer.parseInt(value);
			return folderType == iTest;
		}
		return super.testAttribute(target, name, value);
	}
}
