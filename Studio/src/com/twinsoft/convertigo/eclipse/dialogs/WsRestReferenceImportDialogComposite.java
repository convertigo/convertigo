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
 * $URL: svn://devus.twinsoft.fr/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/eclipse/dialogs/WsReferenceImportDialogComposite.java $
 * $Author: nathalieh $
 * $Revision: 41744 $
 * $Date: 2016-05-10 17:46:38 +0200 (mar., 10 mai 2016) $
 */

package com.twinsoft.convertigo.eclipse.dialogs;

import org.eclipse.swt.widgets.Composite;

public class WsRestReferenceImportDialogComposite extends WsReferenceImportDialogComposite {

	public WsRestReferenceImportDialogComposite(Composite parent, int style) {
		super(parent, style);
		this.filterExtension = new String[]{"*.yaml", "*.json"};
		this.filterNames = new String[]{"YAML files", "JSON files"};
	}
}
